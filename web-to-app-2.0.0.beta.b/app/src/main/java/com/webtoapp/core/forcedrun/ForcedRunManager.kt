package com.webtoapp.core.forcedrun

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import com.webtoapp.core.logging.AppLogger
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

/**
 * 强制运行防护级别
 */
enum class ProtectionLevel {
    BASIC,      // 基础：仅拦截返回键（几乎无效）
    STANDARD,   // 标准：辅助功能服务（需要用户授权）
    MAXIMUM     // 最强：辅助功能 + 前台守护服务 + UsageStats
}

/**
 * 强制运行管理器 - 重写版
 * 
 * 三层防护体系：
 * 1. AccessibilityService - 监控窗口变化，毫秒级响应拉回
 * 2. ForegroundService + UsageStats - 轮询检测前台应用
 * 3. Lock Task Mode - 系统级锁定（需要设备管理员权限）
 * 
 * 功能：
 * - 时间检测和自动启动/退出
 * - 真正有效的系统UI屏蔽
 * - 按键拦截（返回键）
 * - 窗口切换拦截（通过辅助功能）
 * - 倒计时显示
 */
@SuppressLint("StaticFieldLeak")
class ForcedRunManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ForcedRunManager"
        const val ACTION_FORCED_RUN_START = "com.webtoapp.ACTION_FORCED_RUN_START"
        const val ACTION_FORCED_RUN_END = "com.webtoapp.ACTION_FORCED_RUN_END"
        const val ACTION_FORCED_RUN_CHECK = "com.webtoapp.ACTION_FORCED_RUN_CHECK"
        const val EXTRA_APP_ID = "app_id"
        const val PREFS_NAME = "forced_run_prefs"
        
        @Volatile
        private var instance: ForcedRunManager? = null
        
        fun getInstance(context: Context): ForcedRunManager {
            return instance ?: synchronized(this) {
                instance ?: ForcedRunManager(context.applicationContext).also { instance = it }
            }
        }
        
        /**
         * 检查当前防护级别的权限是否已授权
         */
        fun checkProtectionPermissions(context: Context, level: ProtectionLevel): PermissionStatus {
            return when (level) {
                ProtectionLevel.BASIC -> PermissionStatus(true, true, "基础防护无需额外权限")
                ProtectionLevel.STANDARD -> {
                    val hasAccessibility = ForcedRunAccessibilityService.isAccessibilityServiceEnabled(context)
                    PermissionStatus(
                        hasAccessibility,
                        hasAccessibility,
                        if (hasAccessibility) "辅助功能已启用" else "需要启用辅助功能服务"
                    )
                }
                ProtectionLevel.MAXIMUM -> {
                    val hasAccessibility = ForcedRunAccessibilityService.isAccessibilityServiceEnabled(context)
                    val hasUsageStats = ForcedRunGuardService.hasUsageStatsPermission(context)
                    val allGranted = hasAccessibility && hasUsageStats
                    val message = buildString {
                        if (!hasAccessibility) append("需要启用辅助功能服务\n")
                        if (!hasUsageStats) append("需要授权使用情况访问权限")
                        if (allGranted) append("所有权限已授权")
                    }.trim()
                    PermissionStatus(hasAccessibility, hasUsageStats, message)
                }
            }
        }
    }
    
    /**
     * 权限状态
     */
    data class PermissionStatus(
        val hasAccessibility: Boolean,
        val hasUsageStats: Boolean,
        val message: String
    ) {
        val isFullyGranted: Boolean get() = hasAccessibility && hasUsageStats
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val handler = Handler(Looper.getMainLooper())
    private var wakeLock: PowerManager.WakeLock? = null
    
    // 状态流
    private val _isInForcedRunMode = MutableStateFlow(false)
    val isInForcedRunMode: StateFlow<Boolean> = _isInForcedRunMode.asStateFlow()
    
    private val _remainingTimeMs = MutableStateFlow(0L)
    val remainingTimeMs: StateFlow<Long> = _remainingTimeMs.asStateFlow()
    
    private val _currentConfig = MutableStateFlow<ForcedRunConfig?>(null)
    val currentConfig: StateFlow<ForcedRunConfig?> = _currentConfig.asStateFlow()
    
    // 倒计时更新 Runnable
    private val countdownRunnable = object : Runnable {
        override fun run() {
            updateRemainingTime()
            if (_isInForcedRunMode.value) {
                handler.postDelayed(this, 1000) // 每秒更新
            }
        }
    }
    
    // Time检查 Runnable
    private val timeCheckRunnable = object : Runnable {
        override fun run() {
            checkTimeAndUpdateState()
            handler.postDelayed(this, 60000) // 每分钟检查
        }
    }
    
    /**
     * 检查当前时间是否在强制运行时间段内
     */
    fun isInForcedRunPeriod(config: ForcedRunConfig): Boolean {
        if (!config.enabled) return false
        
        return when (config.mode) {
            ForcedRunMode.FIXED_TIME -> isInFixedTimePeriod(config)
            ForcedRunMode.COUNTDOWN -> _isInForcedRunMode.value // 倒计时模式由手动启动控制
            ForcedRunMode.DURATION -> isInAccessPeriod(config)
        }
    }
    
    /**
     * 检查是否在固定时间段内
     */
    private fun isInFixedTimePeriod(config: ForcedRunConfig): Boolean {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        // Calendar.SUNDAY = 1, 转换为我们的格式（周一=1）
        val dayIndex = if (currentDayOfWeek == Calendar.SUNDAY) 7 else currentDayOfWeek - 1
        
        if (!config.activeDays.contains(dayIndex)) return false
        
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinutes = parseTimeToMinutes(config.startTime)
        val endMinutes = parseTimeToMinutes(config.endTime)
        
        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            // 跨午夜的情况
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }
    
    /**
     * 检查是否在可访问时间段内（限时模式）
     */
    private fun isInAccessPeriod(config: ForcedRunConfig): Boolean {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayIndex = if (currentDayOfWeek == Calendar.SUNDAY) 7 else currentDayOfWeek - 1
        
        if (!config.accessDays.contains(dayIndex)) return false
        
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinutes = parseTimeToMinutes(config.accessStartTime)
        val endMinutes = parseTimeToMinutes(config.accessEndTime)
        
        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }
    
    /**
     * 检查是否可以进入应用（限时模式）
     */
    fun canEnterApp(config: ForcedRunConfig): Boolean {
        if (!config.enabled) return true
        
        return when (config.mode) {
            ForcedRunMode.FIXED_TIME -> true // 固定时间模式总是可以进入
            ForcedRunMode.COUNTDOWN -> true // 倒计时模式总是可以进入
            ForcedRunMode.DURATION -> isInAccessPeriod(config) // 限时模式检查时间
        }
    }
    
    /**
     * 获取距离下次可进入的剩余时间（毫秒）
     */
    fun getTimeUntilNextAccess(config: ForcedRunConfig): Long {
        if (!config.enabled || config.mode != ForcedRunMode.DURATION) return 0
        
        val calendar = Calendar.getInstance()
        val startMinutes = parseTimeToMinutes(config.accessStartTime)
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        
        val minutesUntilStart = if (currentMinutes < startMinutes) {
            startMinutes - currentMinutes
        } else {
            // 明天的开始时间
            (24 * 60 - currentMinutes) + startMinutes
        }
        
        return minutesUntilStart * 60 * 1000L
    }
    
    // 当前目标 Activity 信息（用于辅助功能服务拉回）
    private var targetPackageName: String? = null
    private var targetActivityClass: String? = null
    
    // 状态变化回调
    private var onStateChangedCallback: ((Boolean, ForcedRunConfig?) -> Unit)? = null
    
    /**
     * 设置目标 Activity（用于辅助功能服务拉回）
     */
    fun setTargetActivity(packageName: String, activityClass: String) {
        targetPackageName = packageName
        targetActivityClass = activityClass
    }
    
    /**
     * 获取目标 Activity 信息
     */
    fun getTargetPackageName(): String? = targetPackageName
    fun getTargetActivityClass(): String? = targetActivityClass
    
    /**
     * 设置状态变化回调
     */
    fun setOnStateChangedCallback(callback: (Boolean, ForcedRunConfig?) -> Unit) {
        onStateChangedCallback = callback
    }
    
    /**
     * 处理按键事件
     * @return true 表示拦截该按键
     */
    fun handleKeyEvent(keyCode: Int): Boolean {
        val config = _currentConfig.value ?: return false
        if (!_isInForcedRunMode.value) return false
        
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> config.blockBackButton
            KeyEvent.KEYCODE_HOME -> config.blockHomeButton
            KeyEvent.KEYCODE_APP_SWITCH -> config.blockRecentApps
            KeyEvent.KEYCODE_POWER -> config.blockPowerButton
            else -> false
        }
    }
    
    /**
     * 验证紧急退出密码
     */
    fun verifyEmergencyPassword(password: String): Boolean {
        val config = _currentConfig.value ?: return false
        if (!config.allowEmergencyExit) return false
        
        return config.emergencyPassword == password
    }
    
    /**
     * 紧急退出
     */
    fun emergencyExit(password: String): Boolean {
        if (verifyEmergencyPassword(password)) {
            stopForcedRunMode()
            return true
        }
        return false
    }
    
    /**
     * 格式化剩余时间
     */
    fun formatRemainingTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        return if (hours > 0) {
            String.format(java.util.Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }
    
    /**
     * 启动强制运行模式
     */
    fun startForcedRunMode(config: ForcedRunConfig, remainingMs: Long = -1L) {
        _currentConfig.value = config
        _isInForcedRunMode.value = true
        
        // 根据模式设置剩余时间
        when (config.mode) {
            ForcedRunMode.COUNTDOWN -> {
                _remainingTimeMs.value = config.countdownMinutes * 60 * 1000L
            }
            ForcedRunMode.FIXED_TIME -> {
                updateRemainingTime()
            }
            ForcedRunMode.DURATION -> {
                updateRemainingTime()
            }
        }
        
        // 启动倒计时更新
        handler.removeCallbacks(countdownRunnable)
        handler.post(countdownRunnable)
        
        // 启动时间检查
        handler.removeCallbacks(timeCheckRunnable)
        handler.post(timeCheckRunnable)
        
        onStateChangedCallback?.invoke(true, config)
        AppLogger.i(TAG, "Forced run mode started: mode=${config.mode}")
    }
    
    /**
     * 停止强制运行模式
     */
    fun stopForcedRunMode() {
        _isInForcedRunMode.value = false
        _remainingTimeMs.value = 0L
        
        handler.removeCallbacks(countdownRunnable)
        handler.removeCallbacks(timeCheckRunnable)
        
        val config = _currentConfig.value
        _currentConfig.value = null
        
        onStateChangedCallback?.invoke(false, config)
        AppLogger.i(TAG, "Forced run mode stopped")
    }
    
    /**
     * 更新剩余时间
     */
    private fun updateRemainingTime() {
        val config = _currentConfig.value ?: return
        
        when (config.mode) {
            ForcedRunMode.COUNTDOWN -> {
                val remaining = _remainingTimeMs.value - 1000L
                if (remaining <= 0) {
                    _remainingTimeMs.value = 0
                    stopForcedRunMode()
                } else {
                    _remainingTimeMs.value = remaining
                }
            }
            ForcedRunMode.FIXED_TIME -> {
                val calendar = Calendar.getInstance()
                val endMinutes = parseTimeToMinutes(config.endTime)
                val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
                val currentSeconds = calendar.get(Calendar.SECOND)
                
                val remainingMinutes = if (endMinutes > currentMinutes) {
                    endMinutes - currentMinutes
                } else {
                    (24 * 60 - currentMinutes) + endMinutes
                }
                _remainingTimeMs.value = (remainingMinutes * 60L - currentSeconds) * 1000L
            }
            ForcedRunMode.DURATION -> {
                val calendar = Calendar.getInstance()
                val endMinutes = parseTimeToMinutes(config.accessEndTime)
                val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
                val currentSeconds = calendar.get(Calendar.SECOND)
                
                val remainingMinutes = if (endMinutes > currentMinutes) {
                    endMinutes - currentMinutes
                } else {
                    0
                }
                _remainingTimeMs.value = (remainingMinutes * 60L - currentSeconds) * 1000L
            }
        }
    }
    
    /**
     * 检查时间并更新状态（用于固定时间/限时模式的自动启停）
     */
    private fun checkTimeAndUpdateState() {
        val config = _currentConfig.value ?: return
        
        when (config.mode) {
            ForcedRunMode.FIXED_TIME -> {
                if (!isInFixedTimePeriod(config) && _isInForcedRunMode.value) {
                    stopForcedRunMode()
                }
            }
            ForcedRunMode.DURATION -> {
                if (!isInAccessPeriod(config) && _isInForcedRunMode.value) {
                    stopForcedRunMode()
                }
            }
            else -> { /* countdown handled by updateRemainingTime */ }
        }
    }
    
    /**
     * 解析时间字符串为分钟数
     */
    private fun parseTimeToMinutes(time: String): Int {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return hour * 60 + minute
    }
}
