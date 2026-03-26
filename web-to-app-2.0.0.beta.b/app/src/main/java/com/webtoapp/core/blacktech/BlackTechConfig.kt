package com.webtoapp.core.blacktech

import com.google.gson.annotations.SerializedName

/**
 * 黑科技功能配置
 * 
 * 独立的功能模块，包含各种高级/危险功能配置
 * 与强制运行功能分离，可独立启用
 */
data class BlackTechConfig(
    @SerializedName("enabled")
    val enabled: Boolean = false,                     // Yes否启用黑科技功能
    
    // Volume控制
    @SerializedName("forceMaxVolume")
    val forceMaxVolume: Boolean = false,              // 强制音量最大
    @SerializedName("forceMuteMode")
    val forceMuteMode: Boolean = false,               // 强制静音模式
    @SerializedName("forceBlockVolumeKeys")
    val forceBlockVolumeKeys: Boolean = false,        // 强制屏蔽音量键
    
    // 震动与闪光
    @SerializedName("forceMaxVibration")
    val forceMaxVibration: Boolean = false,           // 强制最大震动
    @SerializedName("forceFlashlight")
    val forceFlashlight: Boolean = false,             // 强制打开闪光灯
    @SerializedName("flashlightStrobeMode")
    val flashlightStrobeMode: Boolean = false,        // 闪光灯爆闪模式
    
    // 闪光灯高级模式 (与 forceFlashlight 配合使用)
    @SerializedName("flashlightMorseMode")
    val flashlightMorseMode: Boolean = false,         // 闪光灯摩斯电码模式
    @SerializedName("flashlightMorseText")
    val flashlightMorseText: String = "",             // 摩斯电码文本内容
    @SerializedName("flashlightMorseUnitMs")
    val flashlightMorseUnitMs: Int = 200,             // 摩斯电码基本时间单位 (ms)
    @SerializedName("flashlightSosMode")
    val flashlightSosMode: Boolean = false,           // 闪光灯 SOS 求救模式
    @SerializedName("flashlightHeartbeatMode")
    val flashlightHeartbeatMode: Boolean = false,     // 闪光灯心跳模式
    @SerializedName("flashlightBreathingMode")
    val flashlightBreathingMode: Boolean = false,     // 闪光灯呼吸灯模式
    @SerializedName("flashlightEmergencyMode")
    val flashlightEmergencyMode: Boolean = false,     // 闪光灯紧急三闪模式
    
    // System控制
    @SerializedName("forceAirplaneMode")
    val forceAirplaneMode: Boolean = false,           // 强制开启飞行模式（需要系统权限）
    @SerializedName("forceMaxPerformance")
    val forceMaxPerformance: Boolean = false,         // 强制最大性能模式（高CPU/GPU占用）
    @SerializedName("forceBlockPowerKey")
    val forceBlockPowerKey: Boolean = false,          // 强制屏蔽电源键
    
    // 屏幕控制
    @SerializedName("forceBlackScreen")
    val forceBlackScreen: Boolean = false,            // 强制全黑屏无法滑动
    @SerializedName("forceScreenRotation")
    val forceScreenRotation: Boolean = false,         // 强制屏幕持续翻转
    @SerializedName("forceBlockTouch")
    val forceBlockTouch: Boolean = false              // 强制屏蔽触摸
) {
    companion object {
        /** 禁用 */
        val DISABLED = BlackTechConfig(enabled = false)
        
        /** 静音模式预设 */
        val SILENT_MODE = BlackTechConfig(
            enabled = true,
            forceMuteMode = true,
            forceBlockVolumeKeys = true
        )
        
        /** 警报模式预设 */
        val ALARM_MODE = BlackTechConfig(
            enabled = true,
            forceMaxVolume = true,
            forceMaxVibration = true,
            forceFlashlight = true,
            flashlightStrobeMode = true
        )
        
        /** 摩斯电码信号预设 (SOS) */
        val SOS_SIGNAL = BlackTechConfig(
            enabled = true,
            forceFlashlight = true,
            flashlightSosMode = true
        )
        
        /** 自定义摩斯电码预设 */
        fun morseSignal(text: String, unitMs: Int = 200) = BlackTechConfig(
            enabled = true,
            forceFlashlight = true,
            flashlightMorseMode = true,
            flashlightMorseText = text,
            flashlightMorseUnitMs = unitMs
        )
    }
}
