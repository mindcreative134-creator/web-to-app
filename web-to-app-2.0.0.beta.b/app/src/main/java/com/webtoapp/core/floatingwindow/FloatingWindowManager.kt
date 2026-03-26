package com.webtoapp.core.floatingwindow

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.FloatingWindowConfig

/**
 * 悬浮小窗管理器
 * 管理悬浮窗的创建、拖拽、大小调整和透明度控制
 *
 * 功能：
 * - 以窗口覆盖层方式显示 WebView
 * - 标题栏可拖拽移动窗口位置
 * - 支持窗口大小 50%~100% 调整
 * - 支持透明度 30%~100% 调整
 * - 可记住上次窗口位置
 * - 最小化为悬浮按钮
 */
class FloatingWindowManager(private val context: Context) {

    companion object {
        private const val TAG = "FloatingWindowManager"
        private const val PREFS_NAME = "floating_window_prefs"
        private const val KEY_POS_X = "position_x"
        private const val KEY_POS_Y = "position_y"
        
        // 标题栏高度 dp
        private const val TITLE_BAR_HEIGHT_DP = 40
        // 最小化按钮大小 dp
        private const val MINI_BUTTON_SIZE_DP = 56
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // 悬浮窗视图
    private var floatingView: View? = null
    // 最小化按钮
    private var miniButton: View? = null
    // 悬浮窗中的 WebView
    private var webView: WebView? = null
    // 当前配置
    private var config: FloatingWindowConfig = FloatingWindowConfig()
    // 窗口参数
    private var windowParams: WindowManager.LayoutParams? = null
    // 最小化按钮参数
    private var miniParams: WindowManager.LayoutParams? = null
    // 状态
    private var isMinimized: Boolean = false
    private var isShowing: Boolean = false
    
    // 回调
    var onWebViewCreated: ((WebView) -> Unit)? = null
    var onDismiss: (() -> Unit)? = null

    /**
     * 创建并显示悬浮窗
     */
    @SuppressLint("ClickableViewAccessibility")
    fun show(config: FloatingWindowConfig, appName: String = "", url: String = "") {
        if (isShowing) return
        
        this.config = config
        
        // 计算窗口尺寸
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val width = (screenWidth * config.windowSizePercent / 100)
        val height = (screenHeight * config.windowSizePercent / 100)
        
        // 创建窗口参数
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        windowParams = WindowManager.LayoutParams(
            width,
            height,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // 恢复上次位置或居中
            if (config.rememberPosition) {
                x = prefs.getInt(KEY_POS_X, (screenWidth - width) / 2)
                y = prefs.getInt(KEY_POS_Y, (screenHeight - height) / 2)
            } else {
                x = (screenWidth - width) / 2
                y = (screenHeight - height) / 2
            }
            alpha = config.opacity / 100f
        }
        
        // 创建悬浮窗布局
        floatingView = createFloatingLayout(appName)
        
        // 添加到 WindowManager
        try {
            windowManager.addView(floatingView, windowParams)
            isShowing = true
            isMinimized = false
            
            // 加载 URL
            if (url.isNotBlank()) {
                webView?.loadUrl(url)
            }
            
            // 如果配置了启动时最小化
            if (config.startMinimized) {
                minimize()
            }
            
            AppLogger.i(TAG, "悬浮窗已显示: size=${config.windowSizePercent}%, opacity=${config.opacity}%")
        } catch (e: Exception) {
            AppLogger.e(TAG, "显示悬浮窗失败", e)
        }
    }
    
    /**
     * 获取内部 WebView 实例
     */
    fun getWebView(): WebView? = webView

    /**
     * 创建悬浮窗布局
     */
    @SuppressLint("ClickableViewAccessibility", "SetJavaScriptEnabled")
    private fun createFloatingLayout(appName: String): View {
        val density = context.resources.displayMetrics.density
        val titleBarHeight = (TITLE_BAR_HEIGHT_DP * density).toInt()
        val cornerRadius = (16 * density)
        
        // 根容器
        val rootLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            // 圆角背景
            background = createRoundedBackground(cornerRadius)
            clipToOutline = true
            elevation = 8 * density
        }
        
        // 标题栏（用于拖拽）
        if (config.showTitleBar) {
            val titleBar = createTitleBar(appName, titleBarHeight, density)
            rootLayout.addView(titleBar, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                titleBarHeight
            ))
        }
        
        // WebView 容器
        val webViewContainer = FrameLayout(context)
        webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
        }
        webViewContainer.addView(webView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        rootLayout.addView(webViewContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))
        
        // 通知回调
        webView?.let { onWebViewCreated?.invoke(it) }
        
        return rootLayout
    }
    
    /**
     * 创建标题栏
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun createTitleBar(appName: String, height: Int, density: Float): View {
        val titleBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF1E1E2E.toInt()) // 深色标题栏
            gravity = Gravity.CENTER_VERTICAL
            setPadding((12 * density).toInt(), 0, (8 * density).toInt(), 0)
        }
        
        // 拖拽指示器
        val dragIndicator = TextView(context).apply {
            text = "⠿"
            setTextColor(0xFF888888.toInt())
            textSize = 16f
            setPadding(0, 0, (8 * density).toInt(), 0)
        }
        titleBar.addView(dragIndicator)
        
        // 标题文本
        val titleText = TextView(context).apply {
            text = appName.ifBlank { "WebToApp" }
            setTextColor(0xFFE0E0E0.toInt())
            textSize = 14f
            setSingleLine(true)
        }
        titleBar.addView(titleText, LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        ))
        
        // 最小化按钮
        val minimizeBtn = TextView(context).apply {
            text = "─"
            setTextColor(0xFFE0E0E0.toInt())
            textSize = 16f
            gravity = Gravity.CENTER
            val size = (32 * density).toInt()
            minimumWidth = size
            minimumHeight = size
            setOnClickListener { minimize() }
        }
        titleBar.addView(minimizeBtn)
        
        // 关闭按钮
        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(0xFFFF6B6B.toInt())
            textSize = 14f
            gravity = Gravity.CENTER
            val size = (32 * density).toInt()
            minimumWidth = size
            minimumHeight = size
            setOnClickListener { dismiss() }
        }
        titleBar.addView(closeBtn)
        
        // 拖拽逻辑
        setupDragHandler(titleBar)
        
        return titleBar
    }
    
    /**
     * 设置拖拽处理
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragHandler(dragView: View) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        
        dragView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = windowParams?.x ?: 0
                    initialY = windowParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    
                    // 超过阈值才开始拖拽（避免误触）
                    if (!isDragging && (dx * dx + dy * dy > 100)) {
                        isDragging = true
                    }
                    
                    if (isDragging) {
                        windowParams?.x = initialX + dx.toInt()
                        windowParams?.y = initialY + dy.toInt()
                        try {
                            windowManager.updateViewLayout(floatingView, windowParams)
                        } catch (e: Exception) {
                            AppLogger.w(TAG, "更新窗口位置失败", e)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // 保存位置
                    if (config.rememberPosition && isDragging) {
                        prefs.edit()
                            .putInt(KEY_POS_X, windowParams?.x ?: 0)
                            .putInt(KEY_POS_Y, windowParams?.y ?: 0)
                            .apply()
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    /**
     * 最小化为悬浮按钮
     */
    @SuppressLint("ClickableViewAccessibility")
    fun minimize() {
        if (isMinimized || !isShowing) return
        
        // 隐藏悬浮窗
        floatingView?.visibility = View.GONE
        
        val density = context.resources.displayMetrics.density
        val buttonSize = (MINI_BUTTON_SIZE_DP * density).toInt()
        
        // 创建最小化按钮
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        miniParams = WindowManager.LayoutParams(
            buttonSize,
            buttonSize,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = windowParams?.x ?: 0
            y = windowParams?.y ?: 0
            alpha = 0.85f
        }
        
        miniButton = createMiniButton(buttonSize, density)
        
        try {
            windowManager.addView(miniButton, miniParams)
            isMinimized = true
            AppLogger.d(TAG, "悬浮窗已最小化")
        } catch (e: Exception) {
            AppLogger.e(TAG, "最小化失败", e)
        }
    }
    
    /**
     * 创建最小化按钮
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun createMiniButton(size: Int, density: Float): View {
        val button = FrameLayout(context).apply {
            background = createCircleBackground()
            elevation = 8 * density
        }
        
        val icon = TextView(context).apply {
            text = "🌐"
            textSize = 24f
            gravity = Gravity.CENTER
        }
        button.addView(icon, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))
        
        // 点击恢复
        button.setOnClickListener { restore() }
        
        // 拖拽最小化按钮
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = miniParams?.x ?: 0
                    initialY = miniParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    false // 不消费，让 onClick 有机会触发
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (!isDragging && (dx * dx + dy * dy > 100)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        miniParams?.x = initialX + dx.toInt()
                        miniParams?.y = initialY + dy.toInt()
                        try {
                            windowManager.updateViewLayout(miniButton, miniParams)
                        } catch (e: Exception) {
                            // ignore
                        }
                        true
                    } else {
                        false
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        // 保存最小化按钮位置
                        windowParams?.x = miniParams?.x ?: 0
                        windowParams?.y = miniParams?.y ?: 0
                        if (config.rememberPosition) {
                            prefs.edit()
                                .putInt(KEY_POS_X, windowParams?.x ?: 0)
                                .putInt(KEY_POS_Y, windowParams?.y ?: 0)
                                .apply()
                        }
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
        
        return button
    }
    
    /**
     * 从最小化恢复
     */
    fun restore() {
        if (!isMinimized || !isShowing) return
        
        // 移除最小化按钮
        try {
            miniButton?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            // ignore
        }
        miniButton = null
        
        // 恢复悬浮窗位置到最小化按钮位置
        windowParams?.x = miniParams?.x ?: windowParams?.x ?: 0
        windowParams?.y = miniParams?.y ?: windowParams?.y ?: 0
        
        // 显示悬浮窗
        floatingView?.visibility = View.VISIBLE
        try {
            windowManager.updateViewLayout(floatingView, windowParams)
        } catch (e: Exception) {
            AppLogger.w(TAG, "恢复窗口位置失败", e)
        }
        
        isMinimized = false
        AppLogger.d(TAG, "悬浮窗已恢复")
    }
    
    /**
     * 更新窗口大小
     */
    fun updateSize(percent: Int) {
        val clamped = percent.coerceIn(50, 100)
        config = config.copy(windowSizePercent = clamped)
        
        val displayMetrics = context.resources.displayMetrics
        windowParams?.width = (displayMetrics.widthPixels * clamped / 100)
        windowParams?.height = (displayMetrics.heightPixels * clamped / 100)
        
        try {
            if (!isMinimized) {
                windowManager.updateViewLayout(floatingView, windowParams)
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "更新窗口大小失败", e)
        }
    }
    
    /**
     * 更新透明度
     */
    fun updateOpacity(opacity: Int) {
        val clamped = opacity.coerceIn(30, 100)
        config = config.copy(opacity = clamped)
        
        windowParams?.alpha = clamped / 100f
        
        try {
            if (!isMinimized) {
                windowManager.updateViewLayout(floatingView, windowParams)
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "更新透明度失败", e)
        }
    }
    
    /**
     * 关闭悬浮窗
     */
    fun dismiss() {
        if (!isShowing) return
        
        try {
            // 保存位置
            if (config.rememberPosition) {
                prefs.edit()
                    .putInt(KEY_POS_X, windowParams?.x ?: 0)
                    .putInt(KEY_POS_Y, windowParams?.y ?: 0)
                    .apply()
            }
            
            // 清理 WebView
            webView?.let { wv ->
                wv.stopLoading()
                wv.onPause()
                wv.pauseTimers()
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.removeAllViews()
                wv.destroy()
            }
            webView = null
            
            // 移除最小化按钮
            miniButton?.let { windowManager.removeView(it) }
            miniButton = null
            
            // 移除悬浮窗
            floatingView?.let { windowManager.removeView(it) }
            floatingView = null
            
            isShowing = false
            isMinimized = false
            
            onDismiss?.invoke()
            AppLogger.i(TAG, "悬浮窗已关闭")
        } catch (e: Exception) {
            AppLogger.e(TAG, "关闭悬浮窗失败", e)
        }
    }
    
    /**
     * 是否正在显示
     */
    fun isShowing(): Boolean = isShowing
    
    /**
     * 是否已最小化
     */
    fun isMinimized(): Boolean = isMinimized
    
    // ==================== 辅助方法 ====================
    
    /**
     * 创建圆角矩形背景
     */
    private fun createRoundedBackground(cornerRadius: Float): android.graphics.drawable.Drawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            setColor(0xFF1A1A2E.toInt()) // 深色背景
            this.cornerRadius = cornerRadius
            setStroke(2, 0xFF333355.toInt()) // 边框
        }
    }
    
    /**
     * 创建圆形背景
     */
    private fun createCircleBackground(): android.graphics.drawable.Drawable {
        return android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(0xFF2A2A4E.toInt())
            setStroke(2, 0xFF4444AA.toInt())
        }
    }
}
