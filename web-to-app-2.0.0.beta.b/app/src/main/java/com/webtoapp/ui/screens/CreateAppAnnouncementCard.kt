package com.webtoapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import com.webtoapp.ui.components.PremiumOutlinedButton
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.webtoapp.R
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.*
import com.webtoapp.ui.components.*
import com.webtoapp.ui.components.announcement.AnnouncementDialog
import com.webtoapp.ui.components.announcement.AnnouncementConfig
import com.webtoapp.ui.components.announcement.AnnouncementTemplate
import com.webtoapp.ui.components.announcement.AnnouncementTemplateSelector
import com.webtoapp.ui.viewmodel.EditState

/**
 * 公告设置卡片 - 支持多种精美模板
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementCard(
    editState: EditState,
    onEnabledChange: (Boolean) -> Unit,
    onAnnouncementChange: (Announcement) -> Unit
) {
    var showPreview by remember { mutableStateOf(false) }
    
    // 预览弹窗
    if (showPreview && (editState.announcement.title.isNotBlank() || editState.announcement.content.isNotBlank())) {
        com.webtoapp.ui.components.announcement.AnnouncementDialog(
            config = com.webtoapp.ui.components.announcement.AnnouncementConfig(
                announcement = editState.announcement,
                template = com.webtoapp.ui.components.announcement.AnnouncementTemplate.valueOf(
                    editState.announcement.template.name
                ),
                showEmoji = editState.announcement.showEmoji,
                animationEnabled = editState.announcement.animationEnabled
            ),
            onDismiss = { showPreview = false },
            onLinkClick = { /* 预览模式不处理链接 */ }
        )
    }
    
    EnhancedElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                                if (editState.announcementEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_feature_announcement),
                            null,
                            tint = if (editState.announcementEnabled) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = Strings.popupAnnouncement,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                PremiumSwitch(
                    checked = editState.announcementEnabled,
                    onCheckedChange = onEnabledChange
                )
            }

            AnimatedVisibility(
                visible = editState.announcementEnabled,
                enter = expandVertically(
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                    expandFrom = Alignment.Top
                ) + fadeIn(tween(350, easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f))),
                exit = shrinkVertically(
                    animationSpec = tween(
                        durationMillis = 280,
                        easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
                    ),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                )
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 模板选择器
                com.webtoapp.ui.components.announcement.AnnouncementTemplateSelector(
                    selectedTemplate = com.webtoapp.ui.components.announcement.AnnouncementTemplate.valueOf(
                        editState.announcement.template.name
                    ),
                    onTemplateSelected = { template ->
                        onAnnouncementChange(
                            editState.announcement.copy(
                                template = com.webtoapp.data.model.AnnouncementTemplateType.valueOf(template.name)
                            )
                        )
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                PremiumTextField(
                    value = editState.announcement.title,
                    onValueChange = {
                        onAnnouncementChange(editState.announcement.copy(title = it))
                    },
                    label = { Text(Strings.announcementTitle) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                PremiumTextField(
                    value = editState.announcement.content,
                    onValueChange = {
                        onAnnouncementChange(editState.announcement.copy(content = it))
                    },
                    label = { Text(Strings.announcementContent) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                PremiumTextField(
                    value = editState.announcement.linkUrl ?: "",
                    onValueChange = {
                        onAnnouncementChange(editState.announcement.copy(linkUrl = it.ifBlank { null }))
                    },
                    label = { Text(Strings.linkUrl) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (!editState.announcement.linkUrl.isNullOrBlank()) {
                    PremiumTextField(
                        value = editState.announcement.linkText ?: "",
                        onValueChange = {
                            onAnnouncementChange(editState.announcement.copy(linkText = it.ifBlank { null }))
                        },
                        label = { Text(Strings.linkButtonText) },
                        placeholder = { Text(Strings.viewDetails) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Show频率选择
                Text(
                    Strings.displayFrequency,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PremiumFilterChip(
                        selected = editState.announcement.showOnce,
                        onClick = { onAnnouncementChange(editState.announcement.copy(showOnce = true)) },
                        label = { Text(Strings.showOnce) },
                        leadingIcon = if (editState.announcement.showOnce) {
                            { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                        } else null
                    )
                    PremiumFilterChip(
                        selected = !editState.announcement.showOnce,
                        onClick = { onAnnouncementChange(editState.announcement.copy(showOnce = false)) },
                        label = { Text(Strings.everyLaunch) },
                        leadingIcon = if (!editState.announcement.showOnce) {
                            { Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }
                        } else null
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 触发机制设置
                Text(
                    Strings.announcementTriggerSettings,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Start时触发
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                        Text(Strings.announcementTriggerOnLaunch, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            Strings.announcementTriggerOnLaunchHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    PremiumSwitch(
                        checked = editState.announcement.triggerOnLaunch,
                        onCheckedChange = { onAnnouncementChange(editState.announcement.copy(triggerOnLaunch = it)) }
                    )
                }
                
                // 无网络时触发
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                        Text(Strings.announcementTriggerOnNoNetwork, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            Strings.announcementTriggerOnNoNetworkHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    PremiumSwitch(
                        checked = editState.announcement.triggerOnNoNetwork,
                        onCheckedChange = { onAnnouncementChange(editState.announcement.copy(triggerOnNoNetwork = it)) }
                    )
                }
                
                // 定时间隔触发
                var intervalExpanded by remember { mutableStateOf(false) }
                val intervalOptions = listOf(0, 1, 3, 5, 10, 15, 30, 60)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                        Text(Strings.announcementTriggerInterval, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            Strings.announcementTriggerIntervalHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    ExposedDropdownMenuBox(
                        expanded = intervalExpanded,
                        onExpandedChange = { intervalExpanded = it },
                        modifier = Modifier.width(120.dp)
                    ) {
                        PremiumTextField(
                            value = if (editState.announcement.triggerIntervalMinutes == 0) 
                                Strings.announcementIntervalDisabled 
                            else 
                                "${editState.announcement.triggerIntervalMinutes} ${Strings.minutesShort}",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
                            modifier = Modifier.menuAnchor(),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = intervalExpanded,
                            onDismissRequest = { intervalExpanded = false }
                        ) {
                            intervalOptions.forEach { interval ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            if (interval == 0) Strings.announcementIntervalDisabled 
                                            else "$interval ${Strings.minutesShort}"
                                        ) 
                                    },
                                    onClick = {
                                        onAnnouncementChange(editState.announcement.copy(triggerIntervalMinutes = interval))
                                        intervalExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Start时也立即触发一次（仅当定时间隔启用时显示）
                if (editState.announcement.triggerIntervalMinutes > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = editState.announcement.triggerIntervalIncludeLaunch,
                            onCheckedChange = { onAnnouncementChange(editState.announcement.copy(triggerIntervalIncludeLaunch = it)) }
                        )
                        Text(
                            Strings.announcementTriggerIntervalIncludeLaunch,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                // 高级选项
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(weight = 1f, fill = true)
                    ) {
                        Checkbox(
                            checked = editState.announcement.showEmoji,
                            onCheckedChange = {
                                onAnnouncementChange(editState.announcement.copy(showEmoji = it))
                            }
                        )
                        Text(Strings.showEmoji, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(weight = 1f, fill = true)
                    ) {
                        Checkbox(
                            checked = editState.announcement.animationEnabled,
                            onCheckedChange = {
                                onAnnouncementChange(editState.announcement.copy(animationEnabled = it))
                            }
                        )
                        Text(Strings.enableAnimation, style = MaterialTheme.typography.bodySmall)
                    }
                }
                
// 新增选项：勾选确认与不再显示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = editState.announcement.requireConfirmation,
                            onCheckedChange = { onAnnouncementChange(editState.announcement.copy(requireConfirmation = it)) }
                        )
                        Text(Strings.announcementAgreeAndContinue, style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = editState.announcement.allowNeverShow,
                            onCheckedChange = { onAnnouncementChange(editState.announcement.copy(allowNeverShow = it)) }
                        )
                        Text(Strings.announcementNeverShow, style = MaterialTheme.typography.bodySmall)
                    }
                }

                // 预览按钮
                PremiumOutlinedButton(
                onClick = { showPreview = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = editState.announcement.title.isNotBlank() || editState.announcement.content.isNotBlank()
            ) {
                    Icon(Icons.Outlined.Preview, null, Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.previewAnnouncementEffect)
                }
              }
            }
        }
    }
}
