package com.webtoapp.ui.shell

import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.shell.ShellConfig
import com.webtoapp.data.model.ScriptRunTime
import com.webtoapp.data.model.WebViewConfig

/**
 * 从 ShellConfig 构建 WebViewConfig
 * 将 Shell 配置中的 WebView 相关设置转换为 WebViewConfig 数据类
 */
fun buildWebViewConfig(config: ShellConfig): WebViewConfig {
    val webViewConfig = WebViewConfig(
        javaScriptEnabled = config.webViewConfig.javaScriptEnabled,
        domStorageEnabled = config.webViewConfig.domStorageEnabled,
        zoomEnabled = config.webViewConfig.zoomEnabled,
        desktopMode = config.webViewConfig.desktopMode,
        userAgent = config.webViewConfig.userAgent,
        userAgentMode = run {
            val rawMode = config.webViewConfig.userAgentMode
            try {
                val mode = com.webtoapp.data.model.UserAgentMode.valueOf(rawMode)
                AppLogger.d("ShellActivity", "UserAgentMode parsed: '$rawMode' -> ${mode.name}")
                mode
            } catch (e: Exception) {
                AppLogger.e("ShellActivity", "UserAgentMode parse failed: '$rawMode', falling back to DEFAULT", e)
                com.webtoapp.data.model.UserAgentMode.DEFAULT
            }
        },
        customUserAgent = config.webViewConfig.customUserAgent,
        openExternalLinks = config.webViewConfig.openExternalLinks,
        downloadEnabled = true, // 确保下载功能始终启用
        popupBlockerEnabled = config.webViewConfig.popupBlockerEnabled,
        // 浏览器兼容性增强配置
        initialScale = config.webViewConfig.initialScale,
        newWindowBehavior = try { com.webtoapp.data.model.NewWindowBehavior.valueOf(config.webViewConfig.newWindowBehavior) } catch (e: Exception) { com.webtoapp.data.model.NewWindowBehavior.SAME_WINDOW },
        enablePaymentSchemes = config.webViewConfig.enablePaymentSchemes,
        enableShareBridge = config.webViewConfig.enableShareBridge,
        enableZoomPolyfill = config.webViewConfig.enableZoomPolyfill,
        enableCrossOriginIsolation = config.webViewConfig.enableCrossOriginIsolation,
        disableShields = config.webViewConfig.disableShields,
        injectScripts = config.webViewConfig.injectScripts.map { shellScript ->
            com.webtoapp.data.model.UserScript(
                name = shellScript.name,
                code = shellScript.code,
                enabled = shellScript.enabled,
                runAt = try { ScriptRunTime.valueOf(shellScript.runAt) } catch (e: Exception) { ScriptRunTime.DOCUMENT_END }
            )
        }
    )

    AppLogger.d("ShellActivity", "Constructed WebViewConfig: userAgentMode=${webViewConfig.userAgentMode.name}, customUserAgent=${webViewConfig.customUserAgent}, userAgent=${webViewConfig.userAgent}")
    AppLogger.d("ShellActivity", "InjectScripts: shellScripts=${config.webViewConfig.injectScripts.size}, mapped=${webViewConfig.injectScripts.size}")
    webViewConfig.injectScripts.forEachIndexed { i, script ->
        AppLogger.d("ShellActivity", "  Script[$i]: name='${script.name}', enabled=${script.enabled}, runAt=${script.runAt.name}, codeLength=${script.code.length}")
    }

    return webViewConfig
}
