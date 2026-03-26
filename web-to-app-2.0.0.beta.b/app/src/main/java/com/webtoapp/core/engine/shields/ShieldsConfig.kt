package com.webtoapp.core.engine.shields

import com.webtoapp.core.i18n.Strings

/**
 * 第三方 Cookie 策略
 *
 * ★ displayName 改为使用 i18n 系统的动态 getter，而非硬编码中文
 */
enum class ThirdPartyCookiePolicy {
    ALLOW_ALL,
    BLOCK_CROSS_SITE,
    BLOCK_ALL_THIRD_PARTY;

    val displayName: String get() = when (this) {
        ALLOW_ALL -> Strings.shieldsCookieAllowAll
        BLOCK_CROSS_SITE -> Strings.shieldsCookieBlockCrossSite
        BLOCK_ALL_THIRD_PARTY -> Strings.shieldsCookieBlockAllThirdParty
    }
}

/**
 * Shields Referrer 策略
 *
 * ★ displayName 改为使用 i18n 系统的动态 getter
 */
enum class ShieldsReferrerPolicy(val value: String) {
    NO_REFERRER("no-referrer"),
    ORIGIN("origin"),
    STRICT_ORIGIN_CROSS("strict-origin-when-cross-origin"),
    SAME_ORIGIN("same-origin"),
    UNSAFE_URL("unsafe-url");

    val displayName: String get() = when (this) {
        NO_REFERRER -> Strings.shieldsRefNoReferrer
        ORIGIN -> Strings.shieldsRefOrigin
        STRICT_ORIGIN_CROSS -> Strings.shieldsRefStrictOriginCross
        SAME_ORIGIN -> Strings.shieldsRefSameOrigin
        UNSAFE_URL -> Strings.shieldsRefUnsafeUrl
    }
}

/**
 * 跟踪器分类
 *
 * ★ displayName 改为使用 i18n 系统的动态 getter
 */
enum class TrackerCategory {
    ANALYTICS,
    SOCIAL,
    FINGERPRINTING,
    CRYPTOMINING,
    AD_NETWORK;

    val displayName: String get() = when (this) {
        ANALYTICS -> Strings.shieldsTrackerAnalytics
        SOCIAL -> Strings.shieldsTrackerSocial
        FINGERPRINTING -> Strings.shieldsTrackerFingerprinting
        CRYPTOMINING -> Strings.shieldsTrackerCryptomining
        AD_NETWORK -> Strings.shieldsTrackerAdNetwork
    }
}

/**
 * Shields 统一配置
 * 管理所有浏览器隐私保护功能的开关
 */
data class ShieldsConfig(
    /** 总开关 */
    val enabled: Boolean = true,
    
    /** HTTPS 自动升级 */
    val httpsUpgrade: Boolean = true,
    
    /** 跟踪器拦截 */
    val trackerBlocking: Boolean = true,
    
    /** Cookie 弹窗自动关闭 */
    val cookieConsentBlock: Boolean = true,
    
    /** Global Privacy Control 信号 */
    val gpcEnabled: Boolean = true,
    
    /** 第三方 Cookie 策略 */
    val thirdPartyCookiePolicy: ThirdPartyCookiePolicy = ThirdPartyCookiePolicy.BLOCK_CROSS_SITE,
    
    /** Referrer 策略 */
    val referrerPolicy: ShieldsReferrerPolicy = ShieldsReferrerPolicy.STRICT_ORIGIN_CROSS,
    
    /** 阅读模式 */
    val readerModeEnabled: Boolean = true
) {
    companion object {
        /** 默认配置（推荐） */
        val DEFAULT = ShieldsConfig()
        
        /** 关闭所有防护 */
        val DISABLED = ShieldsConfig(enabled = false)
        
        /** 最大防护 */
        val MAXIMUM = ShieldsConfig(
            enabled = true,
            httpsUpgrade = true,
            trackerBlocking = true,
            cookieConsentBlock = true,
            gpcEnabled = true,
            thirdPartyCookiePolicy = ThirdPartyCookiePolicy.BLOCK_ALL_THIRD_PARTY,
            referrerPolicy = ShieldsReferrerPolicy.NO_REFERRER,
            readerModeEnabled = true
        )
    }
}
