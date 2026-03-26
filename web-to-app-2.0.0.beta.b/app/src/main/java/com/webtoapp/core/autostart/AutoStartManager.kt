package com.webtoapp.core.autostart

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.webtoapp.core.logging.AppLogger
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 自启动管理器
 * 管理开机自启动和定时自启动功能
 *
 * 优化点：
 * 1. 使用精确闹钟（setExactAndAllowWhileIdle）替换 setRepeating，保证 Doze 模式精准触发
 * 2. 精确计算下一次启动时间（考虑 days 过滤器 + 跨周）
 * 3. Android 12+ SCHEDULE_EXACT_ALARM 权限检查与优雅降级
 * 4. 支持 TIME_SET / TIMEZONE_CHANGED 后自动重新调度
 * 5. 提供"下次启动时间"查询接口，供 UI 层显示
 */
class AutoStartManager(private val context: Context) {

    companion object {
        private const val TAG = "AutoStartManager"
        const val ACTION_SCHEDULED_START = "com.webtoapp.ACTION_SCHEDULED_START"
        const val EXTRA_APP_ID = "app_id"
        const val PREFS_NAME = "auto_start_prefs"
        const val KEY_BOOT_START_APP_ID = "boot_start_app_id"
        const val KEY_SCHEDULED_START_APP_ID = "scheduled_start_app_id"
        const val KEY_SCHEDULED_TIME = "scheduled_time"
        const val KEY_SCHEDULED_DAYS = "scheduled_days"

        /** 开机延迟启动毫秒数 — 给系统服务初始化预留时间 */
        const val BOOT_LAUNCH_DELAY_MS = 5000L
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // ═══════════════════════════════════════
    // 开机自启动
    // ═══════════════════════════════════════

    /**
     * 设置开机自启动
     */
    fun setBootStart(appId: Long, enabled: Boolean) {
        prefs.edit().apply {
            if (enabled) {
                putLong(KEY_BOOT_START_APP_ID, appId)
            } else {
                remove(KEY_BOOT_START_APP_ID)
            }
            apply()
        }
        AppLogger.d(TAG, "开机自启动 ${if (enabled) "已启用" else "已禁用"}, appId=$appId")
    }

    /**
     * 获取开机自启动的应用ID
     */
    fun getBootStartAppId(): Long {
        return prefs.getLong(KEY_BOOT_START_APP_ID, -1L)
    }

    // ═══════════════════════════════════════
    // 定时自启动
    // ═══════════════════════════════════════

    /**
     * 设置定时自启动
     * @param appId 应用ID
     * @param enabled 是否启用
     * @param time 启动时间（HH:mm 格式）
     * @param days 启动日期列表（1-7 代表周一到周日）
     */
    fun setScheduledStart(
        appId: Long,
        enabled: Boolean,
        time: String = "08:00",
        days: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7)
    ) {
        if (enabled) {
            prefs.edit().apply {
                putLong(KEY_SCHEDULED_START_APP_ID, appId)
                putString(KEY_SCHEDULED_TIME, time)
                putString(KEY_SCHEDULED_DAYS, days.joinToString(","))
                apply()
            }
            scheduleNextAlarm(appId, time, days)
        } else {
            prefs.edit().apply {
                remove(KEY_SCHEDULED_START_APP_ID)
                remove(KEY_SCHEDULED_TIME)
                remove(KEY_SCHEDULED_DAYS)
                apply()
            }
            cancelAlarm()
        }
        AppLogger.d(TAG, "定时自启动 ${if (enabled) "已启用" else "已禁用"}, appId=$appId, time=$time, days=$days")
    }

    /**
     * 获取定时自启动配置
     */
    fun getScheduledStartConfig(): ScheduledStartConfig? {
        val appId = prefs.getLong(KEY_SCHEDULED_START_APP_ID, -1L)
        if (appId == -1L) return null

        val time = prefs.getString(KEY_SCHEDULED_TIME, "08:00") ?: "08:00"
        val daysStr = prefs.getString(KEY_SCHEDULED_DAYS, "1,2,3,4,5,6,7") ?: "1,2,3,4,5,6,7"
        val days = daysStr.split(",").mapNotNull { it.trim().toIntOrNull() }

        return ScheduledStartConfig(appId, time, days)
    }

    // ═══════════════════════════════════════
    // 精确闹钟调度
    // ═══════════════════════════════════════

    /**
     * 计算下一次启动的精确时间（考虑 days 过滤器 + 跨周）
     *
     * @return Calendar 对象表示下一次启动时间，如果 days 为空则返回 null
     */
    fun calculateNextTriggerTime(time: String, days: List<Int>): Calendar? {
        if (days.isEmpty()) return null

        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val now = Calendar.getInstance()

        // 搜索未来 8 天（当天 + 一整周），找到最近的匹配日
        for (dayOffset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // 如果是今天且已过时间，跳过
            if (dayOffset == 0 && candidate.timeInMillis <= now.timeInMillis) {
                continue
            }

            // 转换 Calendar.DAY_OF_WEEK (1=周日..7=周六) → 我们的格式 (1=周一..7=周日)
            val calendarDow = candidate.get(Calendar.DAY_OF_WEEK)
            val ourDow = if (calendarDow == Calendar.SUNDAY) 7 else calendarDow - 1

            if (days.contains(ourDow)) {
                return candidate
            }
        }

        return null
    }

    /**
     * 格式化下次启动时间（供 UI 展示）
     * @return 如 "明天 08:00" / "周三 08:00" / null（未启用）
     */
    fun getNextTriggerTimeDisplay(): String? {
        val config = getScheduledStartConfig() ?: return null
        val nextTrigger = calculateNextTriggerTime(config.time, config.days) ?: return null

        val now = Calendar.getInstance()
        val daysDiff = ((nextTrigger.timeInMillis - now.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(nextTrigger.time)

        return when (daysDiff) {
            0 -> "今天 $timeStr"
            1 -> "明天 $timeStr"
            else -> {
                val dayNames = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
                val calendarDow = nextTrigger.get(Calendar.DAY_OF_WEEK)
                val ourDow = if (calendarDow == Calendar.SUNDAY) 7 else calendarDow - 1
                "${dayNames[ourDow - 1]} $timeStr"
            }
        }
    }

    /**
     * 设置下一次精确闹钟（替换 setRepeating，每次只调度一次，触发后再 reschedule）
     */
    private fun scheduleNextAlarm(appId: Long, time: String, days: List<Int>) {
        val nextTrigger = calculateNextTriggerTime(time, days)
        if (nextTrigger == null) {
            AppLogger.w(TAG, "无法计算下一次启动时间 (time=$time, days=$days)")
            return
        }

        val intent = Intent(context, ScheduledStartReceiver::class.java).apply {
            action = ACTION_SCHEDULED_START
            putExtra(EXTRA_APP_ID, appId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 根据系统版本选择最合适的调度方式
        try {
            if (canScheduleExactAlarms()) {
                // 精确闹钟 — Doze 模式下依然触发
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTrigger.timeInMillis,
                    pendingIntent
                )
                AppLogger.d(TAG, "精确闹钟已设置: ${formatDate(nextTrigger.time)}")
            } else {
                // 没有精确闹钟权限，使用允许的近似方式
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTrigger.timeInMillis,
                    pendingIntent
                )
                AppLogger.w(TAG, "精确闹钟权限不可用，使用近似闹钟: ${formatDate(nextTrigger.time)}")
            }
        } catch (e: SecurityException) {
            // Android 12+ 可能抛出 SecurityException
            AppLogger.w(TAG, "设置精确闹钟被拒绝，降级为近似闹钟", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTrigger.timeInMillis,
                    pendingIntent
                )
            } catch (ex: Exception) {
                AppLogger.e(TAG, "设置闹钟完全失败", ex)
            }
        }
    }

    /**
     * 取消定时闹钟
     */
    private fun cancelAlarm() {
        val intent = Intent(context, ScheduledStartReceiver::class.java).apply {
            action = ACTION_SCHEDULED_START
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        AppLogger.d(TAG, "定时闹钟已取消")
    }

    /**
     * 当闹钟触发后，重新调度下一次（由 ScheduledStartReceiver 调用）
     */
    fun rescheduleAfterTrigger() {
        val config = getScheduledStartConfig() ?: return
        scheduleNextAlarm(config.appId, config.time, config.days)
    }

    /**
     * 重新设置闹钟（用于开机、时区变化、时间修改后恢复）
     */
    fun rescheduleAlarmIfNeeded() {
        val config = getScheduledStartConfig() ?: return
        scheduleNextAlarm(config.appId, config.time, config.days)
    }

    // ═══════════════════════════════════════
    // 权限检查
    // ═══════════════════════════════════════

    /**
     * 检查是否可以设置精确闹钟
     * Android 12 (API 31) 起需要 SCHEDULE_EXACT_ALARM 权限
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Android 11 及以下不需要特殊权限
        }
    }

    /**
     * 检查电池优化是否被忽略（即是否在白名单中）
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    // ═══════════════════════════════════════
    // 工具方法
    // ═══════════════════════════════════════

    private fun formatDate(date: Date): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)
    }
}

/**
 * 定时启动配置
 */
data class ScheduledStartConfig(
    val appId: Long,
    val time: String,
    val days: List<Int>
)
