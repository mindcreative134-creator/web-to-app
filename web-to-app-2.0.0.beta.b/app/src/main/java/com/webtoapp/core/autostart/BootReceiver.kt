package com.webtoapp.core.autostart

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.WebToAppApplication

/**
 * 开机 / 时间变更 广播接收器
 *
 * 监听以下系统事件：
 * - BOOT_COMPLETED  — 设备开机完成后自动启动配置的应用
 * - TIME_SET         — 用户手动修改系统时间后重新调度闹钟
 * - TIMEZONE_CHANGED — 用户切换时区后重新调度闹钟
 *
 * 优化点：
 * 1. 开机启动增加 5 秒延迟，避免系统服务尚未就绪导致崩溃（国产 ROM 常见）
 * 2. 使用 AutoStartLauncher 集中处理启动逻辑，消除重复代码
 * 3. 时间/时区变更后自动重新调度精确闹钟
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> handleBootCompleted(context)
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> handleTimeChanged(context, intent.action ?: "")
        }
    }

    /**
     * 处理开机完成事件
     */
    private fun handleBootCompleted(context: Context) {
        AppLogger.d(TAG, "收到开机完成广播")

        val autoStartManager = AutoStartManager(context)

        // ① 开机自启动
        val isShellMode = try {
            WebToAppApplication.shellMode.isShellMode()
        } catch (e: Exception) {
            false
        }

        if (isShellMode) {
            val config = try {
                WebToAppApplication.shellMode.getConfig()
            } catch (e: Exception) {
                null
            }
            if (config?.autoStartConfig?.bootStartEnabled == true) {
                AppLogger.d(TAG, "Shell 模式：延迟启动开机自启动应用")
                AutoStartLauncher.launch(
                    context = context,
                    source = "BootReceiver/Shell",
                    delayMs = AutoStartManager.BOOT_LAUNCH_DELAY_MS
                )
            }
        } else {
            val bootStartAppId = autoStartManager.getBootStartAppId()
            if (bootStartAppId > 0) {
                AppLogger.d(TAG, "主应用模式：延迟启动应用 $bootStartAppId")
                AutoStartLauncher.launch(
                    context = context,
                    source = "BootReceiver/Main",
                    appId = bootStartAppId,
                    delayMs = AutoStartManager.BOOT_LAUNCH_DELAY_MS
                )
            }
        }

        // ② 重新设置定时闹钟（开机后系统闹钟会丢失）
        autoStartManager.rescheduleAlarmIfNeeded()
    }

    /**
     * 处理时间/时区变更事件
     * 系统时间改变后，之前调度的精确闹钟可能不再准确，需要重新调度
     */
    private fun handleTimeChanged(context: Context, action: String) {
        AppLogger.d(TAG, "收到时间变更广播: $action")

        val autoStartManager = AutoStartManager(context)
        autoStartManager.rescheduleAlarmIfNeeded()
    }
}
