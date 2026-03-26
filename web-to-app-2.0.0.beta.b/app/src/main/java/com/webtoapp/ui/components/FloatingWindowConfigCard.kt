package com.webtoapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.webtoapp.R
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.FloatingWindowConfig
import kotlin.math.roundToInt

/**
 * 悬浮小窗配置卡片
 * 用于配置应用的悬浮窗模式：窗口大小、透明度、标题栏等
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingWindowConfigCard(
    config: FloatingWindowConfig,
    onConfigChange: (FloatingWindowConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    EnhancedElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (config.enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_feature_floating_window),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (config.enabled) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            Strings.floatingWindowTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                PremiumSwitch(
                    checked = config.enabled,
                    onCheckedChange = { onConfigChange(config.copy(enabled = it)) }
                )
            }
            
            // 展开的详细配置
            AnimatedVisibility(visible = config.enabled) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ========== 窗口大小滑块 ==========
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                Strings.floatingWindowSize,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${config.windowSizePercent}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            Strings.floatingWindowSizeDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = config.windowSizePercent.toFloat(),
                            onValueChange = { 
                                onConfigChange(config.copy(windowSizePercent = it.roundToInt())) 
                            },
                            valueRange = 50f..100f,
                            steps = 9, // 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "50%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "100%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    // ========== 透明度滑块 ==========
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                Strings.floatingWindowOpacity,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${config.opacity}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            Strings.floatingWindowOpacityDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = config.opacity.toFloat(),
                            onValueChange = { 
                                onConfigChange(config.copy(opacity = it.roundToInt())) 
                            },
                            valueRange = 30f..100f,
                            steps = 6, // 30, 40, 50, 60, 70, 80, 90, 100
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "30%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "100%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    // ========== 高级选项 ==========
                    
                    // 显示标题栏开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(
                                Strings.floatingWindowShowTitleBar,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                Strings.floatingWindowShowTitleBarDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PremiumSwitch(
                            checked = config.showTitleBar,
                            onCheckedChange = { onConfigChange(config.copy(showTitleBar = it)) }
                        )
                    }
                    
                    // 展开更多设置
                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (expanded) Strings.hideAdvanced else Strings.showAdvanced)
                    }
                    
                    // 高级设置
                    AnimatedVisibility(visible = expanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // 启动时最小化
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                    Text(
                                        Strings.floatingWindowStartMinimized,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        Strings.floatingWindowStartMinimizedDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                PremiumSwitch(
                                    checked = config.startMinimized,
                                    onCheckedChange = { onConfigChange(config.copy(startMinimized = it)) }
                                )
                            }
                            
                            // 记住位置
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                    Text(
                                        Strings.floatingWindowRememberPosition,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        Strings.floatingWindowRememberPositionDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                PremiumSwitch(
                                    checked = config.rememberPosition,
                                    onCheckedChange = { onConfigChange(config.copy(rememberPosition = it)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
