package com.webtoapp.core.errorpage

import com.webtoapp.util.upgradeRemoteHttpToHttps

/**
 * 网络错误页管理器
 * 根据配置组装完整的错误页 HTML（视觉风格 + 小游戏 + 重试逻辑）
 */
class ErrorPageManager(private val config: ErrorPageConfig) {

    /**
     * 生成完整的错误页 HTML
     * @param errorCode WebView 错误码
     * @param description 错误描述
     * @param failedUrl 失败的原始 URL
     * @return 完整的 HTML 字符串，或 null（DEFAULT 模式不拦截）
     */
    @Suppress("UNUSED_PARAMETER")
    fun generateErrorPage(errorCode: Int, description: String, failedUrl: String?): String? {
        return when (config.mode) {
            ErrorPageMode.DEFAULT -> null
            ErrorPageMode.BUILTIN_STYLE -> generateBuiltInPage(failedUrl)
            ErrorPageMode.CUSTOM_HTML -> config.customHtml
            ErrorPageMode.CUSTOM_MEDIA -> generateMediaPage(failedUrl)
        }
    }

    /**
     * 生成内置风格错误页
     */
    private fun generateBuiltInPage(failedUrl: String?): String {
        val style = config.builtInStyle
        val title = "网络连接失败"
        val subtitle = "请检查网络设置后重试"
        val retryBtnText = config.retryButtonText
        val showGame = config.showMiniGame
        val gameType = config.miniGameType
        val autoRetry = config.autoRetrySeconds

        val styleCss = ErrorPageStyles.getStyleCss(style)
        val styleBody = ErrorPageStyles.getStyleBody(style, title, subtitle)
        val gameJs = if (showGame) ErrorPageGames.getGameJs(gameType) else ""

        val safeUrl = failedUrl
            ?.let { upgradeRemoteHttpToHttps(it) }
            ?.replace("'", "\\'")
            ?.replace("\"", "&quot;")
            ?: ""

        return """
<!DOCTYPE html>
<html lang="zh">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<style>
*{margin:0;padding:0;box-sizing:border-box;}
html,body{width:100%;height:100%;overflow-x:hidden;-webkit-tap-highlight-color:transparent;}
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:100vh;padding:24px;text-align:center;}
.illustration{margin-bottom:24px;opacity:0;animation:fadeUp 0.6s ease 0.2s forwards;}
.error-title{font-size:20px;font-weight:600;margin-bottom:8px;opacity:0;animation:fadeUp 0.6s ease 0.4s forwards;}
.error-subtitle{font-size:14px;margin-bottom:28px;opacity:0;animation:fadeUp 0.6s ease 0.6s forwards;}
.retry-btn{
    display:inline-block;padding:12px 36px;border-radius:24px;border:none;
    color:#fff;font-size:15px;font-weight:500;cursor:pointer;
    transition:all 0.2s ease;text-decoration:none;
    opacity:0;animation:fadeUp 0.6s ease 0.8s forwards;
}
.retry-section{margin-bottom:16px;}
.auto-retry{font-size:12px;opacity:0.5;margin-top:10px;opacity:0;animation:fadeUp 0.6s ease 1s forwards;}
.game-link{
    display:inline-block;margin-top:20px;font-size:12px;opacity:0.5;
    cursor:pointer;text-decoration:none;color:inherit;
    opacity:0;animation:fadeUp 0.6s ease 1.1s forwards;
    transition:opacity 0.2s;
}
.game-link:hover,.game-link:active{opacity:0.8;}
@keyframes fadeUp{from{opacity:0;transform:translateY(12px);}to{opacity:1;transform:translateY(0);}}

/* 游戏容器 */
.game-overlay{
    position:fixed;top:0;left:0;width:100%;height:100%;z-index:100;
    display:none;flex-direction:column;align-items:center;justify-content:center;
    background:rgba(0,0,0,0.95);
}
.game-overlay.active{display:flex;}
.game-header{
    display:flex;justify-content:space-between;align-items:center;
    width:100%;max-width:320px;padding:8px 4px;
}
.game-header span{color:rgba(255,255,255,0.6);font-size:13px;}
.game-close{
    color:rgba(255,255,255,0.5);font-size:13px;cursor:pointer;
    padding:4px 12px;border:1px solid rgba(255,255,255,0.2);border-radius:12px;
}
.game-close:active{background:rgba(255,255,255,0.1);}
canvas{border-radius:8px;margin-top:4px;touch-action:none;}

$styleCss
</style>
</head>
<body>
$styleBody

<div class="retry-section">
    <button class="retry-btn" onclick="retryLoad()">$retryBtnText</button>
    ${if (autoRetry > 0) """<div class="auto-retry" id="autoRetry">自动重试中 <span id="countdown">$autoRetry</span>s</div>""" else ""}
</div>

${if (showGame) """<a class="game-link" onclick="showGame()">无聊？试试小游戏 →</a>""" else ""}

${if (showGame) """
<div class="game-overlay" id="gameOverlay">
    <div class="game-header">
        <span>小游戏</span>
        <div class="game-close" onclick="hideGame()">返回</div>
    </div>
    <canvas id="gameCanvas" width="300" height="380"></canvas>
</div>
""" else ""}

<script>
var __retryUrl='$safeUrl';
function retryLoad(){
    if(__retryUrl)location.href=__retryUrl;
    else location.reload();
}
${if (autoRetry > 0) """
(function(){
    var sec=$autoRetry,el=document.getElementById('countdown');
    var t=setInterval(function(){
        sec--;if(el)el.textContent=sec;
        if(sec<=0){clearInterval(t);retryLoad();}
    },1000);
})();
""" else ""}

${if (showGame) """
var __gameStarted=false;
function showGame(){
    document.getElementById('gameOverlay').classList.add('active');
    if(!__gameStarted){__gameStarted=true;startGame();}
}
function hideGame(){document.getElementById('gameOverlay').classList.remove('active');}
function startGame(){
    $gameJs
}
""" else ""}
</script>
</body>
</html>
        """.trimIndent()
    }

    /**
     * 生成自定义媒体错误页
     */
    private fun generateMediaPage(failedUrl: String?): String {
        val mediaPath = config.customMediaPath ?: ""
        val retryBtnText = config.retryButtonText
        val isVideo = mediaPath.endsWith(".mp4") || mediaPath.endsWith(".webm")
        val safeUrl = failedUrl
            ?.let { upgradeRemoteHttpToHttps(it) }
            ?.replace("'", "\\'")
            ?.replace("\"", "&quot;")
            ?: ""

        val mediaHtml = if (isVideo) {
            """<video src="$mediaPath" autoplay loop muted playsinline style="max-width:80%;max-height:50vh;border-radius:12px;"></video>"""
        } else {
            """<img src="$mediaPath" style="max-width:80%;max-height:50vh;border-radius:12px;object-fit:contain;" alt=""/>"""
        }

        return """
<!DOCTYPE html>
<html lang="zh">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<style>
*{margin:0;padding:0;box-sizing:border-box;}
body{
    font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;
    display:flex;flex-direction:column;align-items:center;justify-content:center;
    min-height:100vh;padding:24px;text-align:center;
    background:#1a1a2e;color:#e0e0e0;
}
.media-container{margin-bottom:24px;}
.retry-btn{
    display:inline-block;padding:12px 36px;border-radius:24px;border:none;
    background:linear-gradient(135deg,#667eea,#764ba2);
    color:#fff;font-size:15px;font-weight:500;cursor:pointer;
}
</style>
</head>
<body>
<div class="media-container">$mediaHtml</div>
<button class="retry-btn" onclick="var u='$safeUrl';if(u)location.href=u;else location.reload();">$retryBtnText</button>
</body>
</html>
        """.trimIndent()
    }
}
