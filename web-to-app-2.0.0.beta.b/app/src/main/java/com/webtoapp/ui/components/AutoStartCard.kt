package com.webtoapp.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.webtoapp.R
import com.webtoapp.core.autostart.AutoStartManager
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.AutoStartConfig

/**
 * 自启动配置卡片
 *
 * 优化点：
 * 1. 显示"下次启动时间"让用户确认自启动是否生效
 * 2. 检查并引导精确闹钟权限（Android 12+）
 * 3. 检查并引导电池优化白名单设置（国产 ROM 核心问题）
 * 4. 提示不同品牌 ROM 的特殊自启动设置路径
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoStartCard(
    config: AutoStartConfig?,
    onConfigChange: (AutoStartConfig?) -> Unit
) {
    val context = LocalContext.current

    var expanded by remember { mutableStateOf(config != null && (config.bootStartEnabled || config.scheduledStartEnabled)) }
    var bootStartEnabled by remember(config) { mutableStateOf(config?.bootStartEnabled ?: false) }
    var scheduledStartEnabled by remember(config) { mutableStateOf(config?.scheduledStartEnabled ?: false) }
    var scheduledTime by remember(config) { mutableStateOf(config?.scheduledTime ?: "08:00") }
    var scheduledDays by remember(config) { mutableStateOf(config?.scheduledDays ?: listOf(1,2,3,4,5,6,7)) }

    // Time选择对话框
    var showTimePicker by remember { mutableStateOf(false) }

    // 下次启动时间预览
    val nextTriggerDisplay by remember(scheduledStartEnabled, scheduledTime, scheduledDays) {
        mutableStateOf(
            if (scheduledStartEnabled && scheduledDays.isNotEmpty()) {
                try {
                    val manager = AutoStartManager(context)
                    val nextTrigger = manager.calculateNextTriggerTime(scheduledTime, scheduledDays)
                    nextTrigger?.let {
                        val now = java.util.Calendar.getInstance()
                        val daysDiff = ((it.timeInMillis - now.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
                        val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(it.time)
                        when (daysDiff) {
                            0 -> "${Strings.today} $timeStr"
                            1 -> "${Strings.tomorrow} $timeStr"
                            else -> {
                                val dayNames = listOf(
                                    Strings.dayMon, Strings.dayTue, Strings.dayWed,
                                    Strings.dayThu, Strings.dayFri, Strings.daySat, Strings.daySun
                                )
                                val calendarDow = it.get(java.util.Calendar.DAY_OF_WEEK)
                                val ourDow = if (calendarDow == java.util.Calendar.SUNDAY) 7 else calendarDow - 1
                                "${dayNames[ourDow - 1]} $timeStr"
                            }
                        }
                    }
                } catch (e: Exception) { null }
            } else null
        )
    }

    // 权限状态
    val canScheduleExact by remember {
        mutableStateOf(
            try {
                AutoStartManager(context).canScheduleExactAlarms()
            } catch (e: Exception) { true }
        )
    }
    val ignoringBatteryOpt by remember {
        mutableStateOf(
            try {
                AutoStartManager(context).isIgnoringBatteryOptimizations()
            } catch (e: Exception) { true }
        )
    }

    fun updateConfig() {
        if (!bootStartEnabled && !scheduledStartEnabled) {
            onConfigChange(null)
        } else {
            onConfigChange(AutoStartConfig(
                bootStartEnabled = bootStartEnabled,
                scheduledStartEnabled = scheduledStartEnabled,
                scheduledTime = scheduledTime,
                scheduledDays = scheduledDays
            ))
        }
    }

    EnhancedElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (bootStartEnabled || scheduledStartEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_feature_auto_start),
                            contentDescription = null,
                            tint = if (bootStartEnabled || scheduledStartEnabled) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            Strings.autoStartSettings,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (bootStartEnabled || scheduledStartEnabled) Strings.configured else Strings.notEnabled,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // 开机自启动
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(
                                Strings.bootAutoStart,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                Strings.bootAutoStartHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PremiumSwitch(
                            checked = bootStartEnabled,
                            onCheckedChange = {
                                bootStartEnabled = it
                                updateConfig()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // 定时自启动
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                            Text(
                                Strings.scheduledAutoStart,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                Strings.scheduledAutoStartHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PremiumSwitch(
                            checked = scheduledStartEnabled,
                            onCheckedChange = {
                                scheduledStartEnabled = it
                                updateConfig()
                            }
                        )
                    }

                    // 定时启动详细配置
                    AnimatedVisibility(visible = scheduledStartEnabled) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            // Time选择
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTimePicker = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(Strings.launchTime)
                                    }
                                    Text(
                                        scheduledTime,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 星期选择
                            Text(
                                Strings.launchDate,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                val dayNames = listOf(
                                    Strings.dayMon, Strings.dayTue, Strings.dayWed,
                                    Strings.dayThu, Strings.dayFri, Strings.daySat, Strings.daySun
                                )
                                dayNames.forEachIndexed { index, name ->
                                    val day = index + 1
                                    val isSelected = scheduledDays.contains(day)
                                    PremiumFilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            scheduledDays = if (isSelected) {
                                                scheduledDays - day
                                            } else {
                                                scheduledDays + day
                                            }
                                            updateConfig()
                                        },
                                        label = { Text(name) },
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                }
                            }

                            // ★ 下次启动时间预览
                            nextTriggerDisplay?.let { display ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.Alarm,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "${Strings.nextLaunchTime}: $display",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ★ 权限警告区域
                    if (bootStartEnabled || scheduledStartEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Android 12+ 精确闹钟权限提示
                        if (!canScheduleExact && scheduledStartEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) { /* 某些系统可能不支持 */ }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.Warning,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        Strings.exactAlarmPermissionHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // 电池优化提示
                        if (!ignoringBatteryOpt) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // 降级到通用电池设置
                                            try {
                                                context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
                                            } catch (_: Exception) { }
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.BatteryAlert,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        Strings.batteryOptimizationHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // 提示信息
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                Strings.autoStartNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }

    // Time选择对话框
    if (showTimePicker) {
        TimePickerDialog(
            initialTime = scheduledTime,
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                scheduledTime = time
                updateConfig()
                showTimePicker = false
            }
        )
    }
}

/**
 * 时间选择对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.selectLaunchTime) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val hour = timePickerState.hour.toString().padStart(2, '0')
                    val minute = timePickerState.minute.toString().padStart(2, '0')
                    onConfirm("$hour:$minute")
                }
            ) {
                Text(Strings.btnOk)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.btnCancel)
            }
        }
    )
}
