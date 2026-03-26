package com.webtoapp.ui.components.announcement

import androidx.compose.animation.*
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumOutlinedButton
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.Announcement
import androidx.compose.ui.graphics.Color

/**
 * 公告模板类型
 */
enum class AnnouncementTemplate(
    val displayName: String,
    val description: String,
    val icon: ImageVector
) {
    MINIMAL("极简", "简约清爽的设计风格", Icons.Outlined.Minimize),
    XIAOHONGSHU("小红书", "精美卡片风格", Icons.Outlined.AutoAwesome),
    GRADIENT("渐变", "炫彩渐变背景", Icons.Outlined.Gradient),
    GLASSMORPHISM("毛玻璃", "现代毛玻璃效果", Icons.Outlined.BlurOn),
    NEON("霓虹", "赛博朋克风格", Icons.Outlined.Lightbulb),
    CUTE("可爱", "萌系卡通风格", Icons.Outlined.Favorite),
    ELEGANT("优雅", "高端商务风格", Icons.Outlined.Diamond),
    FESTIVE("节日", "喜庆节日风格", Icons.Outlined.Celebration),
    DARK("暗黑", "深色主题风格", Icons.Outlined.DarkMode),
    NATURE("自然", "清新自然风格", Icons.Outlined.Park)
}

/**
 * 公告配置扩展 - 包含模板信息
 */
data class AnnouncementConfig(
    val announcement: Announcement,
    val template: AnnouncementTemplate = AnnouncementTemplate.XIAOHONGSHU,
    val primaryColor: Color = Color(0xFFFF2442),
    val showEmoji: Boolean = true,
    val animationEnabled: Boolean = true
)

/**
 * 公告弹窗 - 根据模板显示不同样式
 */
@Composable
fun AnnouncementDialog(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)? = null,
    onNeverShowChecked: ((Boolean) -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
when (config.template) {
            AnnouncementTemplate.MINIMAL -> MinimalTemplate(config, onDismiss, onLinkClick, onNeverShowChecked)
            AnnouncementTemplate.XIAOHONGSHU -> XiaohongshuTemplate(config, onDismiss, onLinkClick, onNeverShowChecked)
            AnnouncementTemplate.GRADIENT -> GradientTemplate(config, onDismiss, onLinkClick, onNeverShowChecked)
            AnnouncementTemplate.GLASSMORPHISM -> GlassmorphismTemplate(config, onDismiss, onLinkClick, onNeverShowChecked)
            AnnouncementTemplate.NEON -> NeonTemplate(config, onDismiss, onLinkClick, onNeverShowChecked)
            AnnouncementTemplate.CUTE -> CuteTemplate(config, onDismiss, onLinkClick, onNeverShowChecked)
            AnnouncementTemplate.ELEGANT -> ElegantTemplate(config, onDismiss, onLinkClick, onNeverShowChecked)
            AnnouncementTemplate.FESTIVE -> FestiveTemplate(config, onDismiss, onLinkClick, onNeverShowChecked)
            AnnouncementTemplate.DARK -> DarkTemplate(config, onDismiss, onLinkClick, onNeverShowChecked)
            AnnouncementTemplate.NATURE -> NatureTemplate(config, onDismiss, onLinkClick, onNeverShowChecked)
        }
    }
}

/**
 * 模板1: 极简风格
 */
@Composable
private fun MinimalTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?,
    onNeverShowChecked: ((Boolean) -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .animateEnterExit(config.animationEnabled),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            if (config.announcement.title.isNotBlank()) {
                Text(
                    text = config.announcement.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 内容
            Text(
                text = config.announcement.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Options区：确认与不再显示
            var agreed by remember { mutableStateOf(!config.announcement.requireConfirmation) }
            var neverShow by remember { mutableStateOf(false) }

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (config.announcement.requireConfirmation) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = agreed, onCheckedChange = { agreed = it })
                        Text(Strings.announcementAgreeAndContinue, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (config.announcement.allowNeverShow) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = neverShow, onCheckedChange = { neverShow = it; onNeverShowChecked?.invoke(it) })
                        Text(Strings.announcementNeverShow, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                config.announcement.linkUrl?.let { url ->
                    PremiumOutlinedButton(
                        onClick = { onLinkClick?.invoke(url) },
                        modifier = Modifier.weight(weight = 1f, fill = true),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(config.announcement.linkText ?: Strings.viewDetails)
                    }
                }
PremiumButton(
                    onClick = { if (agreed) onDismiss() },
                    enabled = agreed,
                    modifier = Modifier.weight(weight = 1f, fill = true),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A))
                ) {
                    Text(Strings.gotIt)
                }
            }
        }
    }
}

/**
 * 模板2: 小红书风格 - 精美卡片
 */
@Composable
private fun XiaohongshuTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?,
    onNeverShowChecked: ((Boolean) -> Unit)?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .animateEnterExit(config.animationEnabled),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Box {
            Column(modifier = Modifier.padding(0.dp)) {
                // 顶部装饰条
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFF2442),
                                    Color(0xFFFF6B6B),
                                    Color(0xFFFFE66D)
                                )
                            )
                        )
                )
                
                Column(modifier = Modifier.padding(24.dp)) {
                    // Icon和标题行
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 装饰图标
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFFFF2442), Color(0xFFFF6B6B))
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Campaign,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            if (config.announcement.title.isNotBlank()) {
                                Text(
                                    text = config.announcement.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1A1A)
                                )
                            }
                            if (config.showEmoji) {
                                Text(
                                    text = "New",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFF2442)
                                )
                            }
                        }
                        
                        // 关闭按钮
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFF5F5F5), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = Strings.close,
                                tint = Color(0xFF999999),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // 内容卡片
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFAFAFA)
                    ) {
                        Text(
                            text = config.announcement.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF333333),
                            lineHeight = 26.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 底部按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        config.announcement.linkUrl?.let { url ->
                            PremiumOutlinedButton(
                                onClick = { onLinkClick?.invoke(url) },
                                modifier = Modifier
                                    .weight(weight = 1f, fill = true)
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.5.dp, Color(0xFFFF2442))
                            ) {
                                Text(
                                    config.announcement.linkText ?: Strings.learnMore,
                                    color = Color(0xFFFF2442),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        PremiumButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(weight = 1f, fill = true)
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF2442)
                            )
                        ) {
                            Text(
                                Strings.okGood,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 模板3: 渐变风格
 */
@Composable
private fun GradientTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?,
    onNeverShowChecked: ((Boolean) -> Unit)?
) {
    var agreed by remember { mutableStateOf(!config.announcement.requireConfirmation) }
    var neverShow by remember { mutableStateOf(false) }
    
    val gradientColors = listOf(
        Color(0xFF667EEA),
        Color(0xFF764BA2),
        Color(0xFFF093FB)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .animateEnterExit(config.animationEnabled),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            // 装饰圆形
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset(x = (-30).dp, y = (-30).dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = 40.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            )
            
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 标题
                if (config.announcement.title.isNotBlank()) {
                    Text(
                        text = config.announcement.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // 内容
                Text(
                    text = config.announcement.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Options区
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (config.announcement.requireConfirmation) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = agreed,
                                onCheckedChange = { agreed = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color.White.copy(alpha = 0.7f), checkmarkColor = Color(0xFF667EEA))
                            )
                            Text(Strings.announcementAgreeAndContinue, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                    if (config.announcement.allowNeverShow) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = neverShow,
                                onCheckedChange = { neverShow = it; onNeverShowChecked?.invoke(it) },
                                colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color.White.copy(alpha = 0.7f), checkmarkColor = Color(0xFF667EEA))
                            )
                            Text(Strings.announcementNeverShow, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (config.announcement.linkUrl != null) {
                        PremiumOutlinedButton(
                            onClick = { onLinkClick?.invoke(config.announcement.linkUrl!!) },
                            modifier = Modifier
                                .weight(weight = 1f, fill = true)
                                .height(50.dp),
                            shape = RoundedCornerShape(25.dp),
                            border = BorderStroke(2.dp, Color.White),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                config.announcement.linkText ?: "查看",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    PremiumButton(
                        onClick = { if (agreed) onDismiss() },
                        enabled = agreed,
                        modifier = Modifier
                            .weight(weight = 1f, fill = true)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF667EEA)
                        )
                    ) {
                        Text(Strings.btnConfirm, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}


/**
 * 模板4: 毛玻璃风格 - 真正的 Glassmorphism 效果
 */
@Composable
private fun GlassmorphismTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?,
    onNeverShowChecked: ((Boolean) -> Unit)?
) {
    var agreed by remember { mutableStateOf(!config.announcement.requireConfirmation) }
    var neverShow by remember { mutableStateOf(false) }
    
    // 动态光晕动画
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowOffset"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .animateEnterExit(config.animationEnabled)
    ) {
        // 背景渐变光晕层 - 创造毛玻璃背后的彩色效果
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 8.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF6366F1).copy(alpha = 0.6f),
                            Color(0xFFA855F7).copy(alpha = 0.6f),
                            Color(0xFFEC4899).copy(alpha = 0.6f),
                            Color(0xFF3B82F6).copy(alpha = 0.6f),
                            Color(0xFF6366F1).copy(alpha = 0.6f)
                        )
                    )
                )
                .blur(30.dp)
        )
        
        // 主毛玻璃卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.75f)
            ),
            border = BorderStroke(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f),
                        Color.White.copy(alpha = 0.2f),
                        Color.White.copy(alpha = 0.5f)
                    )
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            // 内部光泽效果
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        // 顶部高光
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height * 0.3f
                            )
                        )
                    }
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 顶部装饰 - 三个彩色圆点
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .shadow(4.dp, CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                                        ),
                                        shape = CircleShape
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .shadow(4.dp, CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color(0xFFA855F7), Color(0xFF9333EA))
                                        ),
                                        shape = CircleShape
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .shadow(4.dp, CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(Color(0xFFEC4899), Color(0xFFDB2777))
                                        ),
                                        shape = CircleShape
                                    )
                            )
                        }
                        
                        // 关闭按钮 - 毛玻璃风格
                        Surface(
                            onClick = onDismiss,
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = Strings.close,
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    // Icon - 渐变背景
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(12.dp, RoundedCornerShape(20.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF6366F1),
                                        Color(0xFFA855F7),
                                        Color(0xFFEC4899)
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 标题
                    if (config.announcement.title.isNotBlank()) {
                        Text(
                            text = config.announcement.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F2937),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // 内容 - 半透明背景
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = config.announcement.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4B5563),
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Options区
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (config.announcement.requireConfirmation) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = agreed, onCheckedChange = { agreed = it })
                                Text(Strings.announcementAgreeAndContinue, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4B5563))
                            }
                        }
                        if (config.announcement.allowNeverShow) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = neverShow, onCheckedChange = { neverShow = it; onNeverShowChecked?.invoke(it) })
                                Text(Strings.announcementNeverShow, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4B5563))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 主按钮 - 渐变
                    PremiumButton(
                        onClick = { if (agreed) onDismiss() },
                        enabled = agreed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF6366F1),
                                            Color(0xFFA855F7)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                Strings.understood,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                    
                    // 链接按钮
                    if (config.announcement.linkUrl != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { onLinkClick?.invoke(config.announcement.linkUrl!!) }
                        ) {
                            Text(
                            config.announcement.linkText ?: Strings.viewDetails,
                                color = Color(0xFF6366F1),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF6366F1)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 模板5: 霓虹风格
 */
@Composable
private fun NeonTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?,
    onNeverShowChecked: ((Boolean) -> Unit)?
) {
    var agreed by remember { mutableStateOf(!config.announcement.requireConfirmation) }
    var neverShow by remember { mutableStateOf(false) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "neon")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .animateEnterExit(config.animationEnabled)
    ) {
        // 霓虹光晕
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .blur(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF00F5FF).copy(alpha = glowAlpha * 0.4f))
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D)),
            border = BorderStroke(2.dp, Color(0xFF00F5FF).copy(alpha = glowAlpha))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部霓虹线
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFF00FF),
                                    Color(0xFF00F5FF),
                                    Color(0xFFFF00FF)
                                )
                            )
                        )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .border(2.dp, Color(0xFF00F5FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.ElectricBolt,
                        contentDescription = null,
                        tint = Color(0xFF00F5FF),
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 标题
                if (config.announcement.title.isNotBlank()) {
                    Text(
                        text = config.announcement.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00F5FF),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // 内容
                Text(
                    text = config.announcement.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE0E0E0),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Options区
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (config.announcement.requireConfirmation) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = agreed,
                                onCheckedChange = { agreed = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00F5FF), uncheckedColor = Color(0xFF00F5FF).copy(alpha = 0.7f))
                            )
                            Text(Strings.announcementAgreeAndContinue, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE0E0E0))
                        }
                    }
                    if (config.announcement.allowNeverShow) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = neverShow,
                                onCheckedChange = { neverShow = it; onNeverShowChecked?.invoke(it) },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00F5FF), uncheckedColor = Color(0xFF00F5FF).copy(alpha = 0.7f))
                            )
                            Text(Strings.announcementNeverShow, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE0E0E0))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (config.announcement.linkUrl != null) {
                        PremiumOutlinedButton(
                            onClick = { onLinkClick?.invoke(config.announcement.linkUrl!!) },
                            modifier = Modifier
                                .weight(weight = 1f, fill = true)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFFF00FF))
                        ) {
                            Text(
                                config.announcement.linkText ?: "LINK",
                                color = Color(0xFFFF00FF),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    PremiumButton(
                        onClick = { if (agreed) onDismiss() },
                        enabled = agreed,
                        modifier = Modifier
                            .weight(weight = 1f, fill = true)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00F5FF),
                            contentColor = Color(0xFF0D0D0D)
                        )
                    ) {
                        Text("ENTER", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 模板6: 可爱风格
 */
@Composable
private fun CuteTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?,
    onNeverShowChecked: ((Boolean) -> Unit)?
) {
    var agreed by remember { mutableStateOf(!config.announcement.requireConfirmation) }
    var neverShow by remember { mutableStateOf(false) }
    val bounceAnim by rememberInfiniteTransition(label = "bounce").animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .animateEnterExit(config.animationEnabled),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 可爱表情
            Text(
                text = "♡",
                fontSize = 48.sp,
                modifier = Modifier.offset(y = (-bounceAnim).dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 标题
            if (config.announcement.title.isNotBlank()) {
                Text(
                    text = "~ ${config.announcement.title} ~",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF69B4),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // 装饰分隔线
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("*", color = Color(0xFFFFB6C1))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(2.dp)
                        .background(Color(0xFFFFB6C1), RoundedCornerShape(1.dp))
                )
                Text("*", color = Color(0xFFFFB6C1))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(2.dp)
                        .background(Color(0xFFFFB6C1), RoundedCornerShape(1.dp))
                )
                Text("*", color = Color(0xFFFFB6C1))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 内容
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(2.dp, Color(0xFFFFB6C1))
            ) {
                Text(
                    text = config.announcement.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Options区
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (config.announcement.requireConfirmation) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = agreed,
                            onCheckedChange = { agreed = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF69B4))
                        )
                        Text(Strings.announcementAgreeAndContinue, style = MaterialTheme.typography.bodySmall, color = Color(0xFF666666))
                    }
                }
                if (config.announcement.allowNeverShow) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = neverShow,
                            onCheckedChange = { neverShow = it; onNeverShowChecked?.invoke(it) },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF69B4))
                        )
                        Text(Strings.announcementNeverShow, style = MaterialTheme.typography.bodySmall, color = Color(0xFF666666))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 按钮
            PremiumButton(
                onClick = { if (agreed) onDismiss() },
                enabled = agreed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF69B4)
                )
            ) {
                Text(Strings.gotItCute, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            if (config.announcement.linkUrl != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { onLinkClick?.invoke(config.announcement.linkUrl!!) }) {
                    Text(
                        "${config.announcement.linkText ?: "去看看"}",
                        color = Color(0xFFFF69B4)
                    )
                }
            }
        }
    }
}


/**
 * 模板7: 优雅风格
 */
@Composable
private fun ElegantTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?,
    onNeverShowChecked: ((Boolean) -> Unit)?
) {
    var agreed by remember { mutableStateOf(!config.announcement.requireConfirmation) }
    var neverShow by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .animateEnterExit(config.animationEnabled),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF9F6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column {
            // 顶部金色装饰
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFD4AF37),
                                Color(0xFFF4E4BA),
                                Color(0xFFD4AF37)
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 装饰图案
                Text(
                    text = "◆",
                    fontSize = 24.sp,
                    color = Color(0xFFD4AF37)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 标题
                if (config.announcement.title.isNotBlank()) {
                    Text(
                        text = config.announcement.title.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFF2C2C2C),
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 装饰线
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(1.dp)
                                .background(Color(0xFFD4AF37))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFFD4AF37), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(1.dp)
                                .background(Color(0xFFD4AF37))
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                }
                
                // 内容
                Text(
                    text = config.announcement.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4A4A4A),
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Options区
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (config.announcement.requireConfirmation) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = agreed,
                                onCheckedChange = { agreed = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD4AF37))
                            )
                            Text(Strings.announcementAgreeAndContinue, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4A4A4A))
                        }
                    }
                    if (config.announcement.allowNeverShow) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = neverShow,
                                onCheckedChange = { neverShow = it; onNeverShowChecked?.invoke(it) },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFD4AF37))
                            )
                            Text(Strings.announcementNeverShow, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4A4A4A))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 按钮
                PremiumOutlinedButton(
                    onClick = { if (agreed) onDismiss() },
                    enabled = agreed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, Color(0xFFD4AF37))
                ) {
                    Text(
                        "确认",
                        color = Color(0xFFD4AF37),
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp
                    )
                }
                
                if (config.announcement.linkUrl != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { onLinkClick?.invoke(config.announcement.linkUrl!!) }) {
                        Text(
                            config.announcement.linkText ?: Strings.learnMore,
                            color = Color(0xFF8B7355),
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        }
    }
}

/**
 * 模板8: 节日风格
 */
@Composable
private fun FestiveTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?,
    onNeverShowChecked: ((Boolean) -> Unit)?
) {
    var agreed by remember { mutableStateOf(!config.announcement.requireConfirmation) }
    var neverShow by remember { mutableStateOf(false) }
    val sparkleAnim by rememberInfiniteTransition(label = "sparkle").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .animateEnterExit(config.animationEnabled),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFB22222)),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Box {
            // 装饰元素
            Text(
                text = "✦",
                fontSize = 20.sp,
                color = Color(0xFFFFD700),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 20.dp, y = 20.dp)
                    .rotate(sparkleAnim)
            )
            Text(
                text = "✦",
                fontSize = 16.sp,
                color = Color(0xFFFFD700),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-30).dp, y = 40.dp)
                    .rotate(-sparkleAnim)
            )
            Text(
                text = "✦",
                fontSize = 14.sp,
                color = Color(0xFFFFD700),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 40.dp, y = (-30).dp)
                    .rotate(sparkleAnim * 0.5f)
            )
            
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 节日图标
                Text(
                    text = "★",
                    fontSize = 56.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 标题
                if (config.announcement.title.isNotBlank()) {
                    Text(
                        text = config.announcement.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // 装饰
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("!", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(2.dp)
                            .background(Color(0xFFFFD700), RoundedCornerShape(1.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("!", fontSize = 20.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 内容
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = config.announcement.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Options区
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (config.announcement.requireConfirmation) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = agreed,
                                onCheckedChange = { agreed = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFFD700), uncheckedColor = Color(0xFFFFD700).copy(alpha = 0.7f))
                            )
                            Text(Strings.announcementAgreeAndContinue, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                    if (config.announcement.allowNeverShow) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = neverShow,
                                onCheckedChange = { neverShow = it; onNeverShowChecked?.invoke(it) },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFFD700), uncheckedColor = Color(0xFFFFD700).copy(alpha = 0.7f))
                            )
                            Text(Strings.announcementNeverShow, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 按钮
                PremiumButton(
                    onClick = { if (agreed) onDismiss() },
                    enabled = agreed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD700),
                        contentColor = Color(0xFFB22222)
                    )
                ) {
                    Text("Received!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                
                if (config.announcement.linkUrl != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { onLinkClick?.invoke(config.announcement.linkUrl!!) }) {
                        Text(
                            "${config.announcement.linkText ?: Strings.viewDetails}",
                            color = Color(0xFFFFD700)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 模板9: 暗黑风格
 */
@Composable
private fun DarkTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?,
    onNeverShowChecked: ((Boolean) -> Unit)?
) {
    var agreed by remember { mutableStateOf(!config.announcement.requireConfirmation) }
    var neverShow by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .animateEnterExit(config.animationEnabled),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
        border = BorderStroke(1.dp, Color(0xFF2D2D44))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF2D2D44), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = Strings.systemNotification,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF6B7280)
                        )
                        Text(
                            text = Strings.justNow,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF4B5563)
                        )
                    }
                }
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = Strings.close,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 标题
            if (config.announcement.title.isNotBlank()) {
                Text(
                    text = config.announcement.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // 内容
            Text(
                text = config.announcement.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD1D5DB),
                lineHeight = 24.sp,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Options区
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (config.announcement.requireConfirmation) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = agreed,
                            onCheckedChange = { agreed = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF8B5CF6))
                        )
                        Text(Strings.announcementAgreeAndContinue, style = MaterialTheme.typography.bodySmall, color = Color(0xFFD1D5DB))
                    }
                }
                if (config.announcement.allowNeverShow) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = neverShow,
                            onCheckedChange = { neverShow = it; onNeverShowChecked?.invoke(it) },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF8B5CF6))
                        )
                        Text(Strings.announcementNeverShow, style = MaterialTheme.typography.bodySmall, color = Color(0xFFD1D5DB))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (config.announcement.linkUrl != null) {
                    PremiumOutlinedButton(
                        onClick = { onLinkClick?.invoke(config.announcement.linkUrl!!) },
                        modifier = Modifier
                            .weight(weight = 1f, fill = true)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF8B5CF6))
                    ) {
                        Text(
                            config.announcement.linkText ?: Strings.details,
                            color = Color(0xFF8B5CF6)
                        )
                    }
                }
                
                PremiumButton(
                    onClick = { if (agreed) onDismiss() },
                    enabled = agreed,
                    modifier = Modifier
                        .weight(weight = 1f, fill = true)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B5CF6)
                    )
                ) {
                    Text(Strings.btnOk)
                }
            }
        }
    }
}

/**
 * 模板10: 自然风格
 */
@Composable
private fun NatureTemplate(
    config: AnnouncementConfig,
    onDismiss: () -> Unit,
    onLinkClick: ((String) -> Unit)?,
    onNeverShowChecked: ((Boolean) -> Unit)?
) {
    var agreed by remember { mutableStateOf(!config.announcement.requireConfirmation) }
    var neverShow by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .animateEnterExit(config.animationEnabled),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column {
            // 顶部装饰
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF86EFAC),
                                Color(0xFFF0FDF4)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Spa, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Outlined.LocalFlorist, contentDescription = null, tint = Color(0xFFE91E63), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Outlined.Spa, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
                }
            }
            
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                if (config.announcement.title.isNotBlank()) {
                    Text(
                        text = config.announcement.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF166534),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // 内容卡片
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Text(
                        text = config.announcement.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF374151),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Options区
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (config.announcement.requireConfirmation) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = agreed,
                                onCheckedChange = { agreed = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF22C55E))
                            )
                            Text(Strings.announcementAgreeAndContinue, style = MaterialTheme.typography.bodySmall, color = Color(0xFF374151))
                        }
                    }
                    if (config.announcement.allowNeverShow) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = neverShow,
                                onCheckedChange = { neverShow = it; onNeverShowChecked?.invoke(it) },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF22C55E))
                            )
                            Text(Strings.announcementNeverShow, style = MaterialTheme.typography.bodySmall, color = Color(0xFF374151))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 按钮
                PremiumButton(
                    onClick = { if (agreed) onDismiss() },
                    enabled = agreed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF22C55E)
                    )
                ) {
                    Text(Strings.btnOk, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                }
                
                if (config.announcement.linkUrl != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { onLinkClick?.invoke(config.announcement.linkUrl!!) }) {
                        Text(
                            "${config.announcement.linkText ?: Strings.learnMore}",
                            color = Color(0xFF16A34A)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 入场动画扩展
 */
private fun Modifier.animateEnterExit(enabled: Boolean): Modifier {
    if (!enabled) return this

    return composed {
        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        val scale by animateFloatAsState(
            targetValue = if (visible) 1f else 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "scale"
        )

        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = tween(300),
            label = "alpha"
        )

        this
            .scale(scale)
            .graphicsLayer { this.alpha = alpha }
    }
}

/**
 * AnnouncementTemplate 本地化扩展函数
 */
fun AnnouncementTemplate.getLocalizedDisplayName(): String = when (this) {
    AnnouncementTemplate.MINIMAL -> Strings.templateMinimal
    AnnouncementTemplate.XIAOHONGSHU -> Strings.templateXiaohongshu
    AnnouncementTemplate.GRADIENT -> Strings.templateGradient
    AnnouncementTemplate.GLASSMORPHISM -> Strings.templateGlassmorphism
    AnnouncementTemplate.NEON -> Strings.templateNeon
    AnnouncementTemplate.CUTE -> Strings.templateCute
    AnnouncementTemplate.ELEGANT -> Strings.templateElegant
    AnnouncementTemplate.FESTIVE -> Strings.templateFestive
    AnnouncementTemplate.DARK -> Strings.templateDark
    AnnouncementTemplate.NATURE -> Strings.templateNature
}

fun AnnouncementTemplate.getLocalizedDescription(): String = when (this) {
    AnnouncementTemplate.MINIMAL -> Strings.templateMinimalDesc
    AnnouncementTemplate.XIAOHONGSHU -> Strings.templateXiaohongshuDesc
    AnnouncementTemplate.GRADIENT -> Strings.templateGradientDesc
    AnnouncementTemplate.GLASSMORPHISM -> Strings.templateGlassmorphismDesc
    AnnouncementTemplate.NEON -> Strings.templateNeonDesc
    AnnouncementTemplate.CUTE -> Strings.templateCuteDesc
    AnnouncementTemplate.ELEGANT -> Strings.templateElegantDesc
    AnnouncementTemplate.FESTIVE -> Strings.templateFestiveDesc
    AnnouncementTemplate.DARK -> Strings.templateDarkDesc
    AnnouncementTemplate.NATURE -> Strings.templateNatureDesc
}
