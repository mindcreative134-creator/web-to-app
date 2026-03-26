package com.webtoapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.core.cloud.AnnouncementData
import com.webtoapp.core.i18n.Strings

/**
 * 全局公告弹窗
 *
 * 用于在 HomeScreen 启动时展示从服务器拉取的公告
 */
@Composable
fun AnnouncementDialog(
    announcement: AnnouncementData,
    onDismiss: () -> Unit,
    onAction: ((String) -> Unit)? = null
) {
    val typeIcon = when (announcement.type) {
        "warning" -> Icons.Filled.Warning
        "error" -> Icons.Filled.Error
        "success" -> Icons.Filled.CheckCircle
        else -> Icons.Filled.Info
    }
    val typeColor = when (announcement.type) {
        "warning" -> Color(0xFFFFA000)
        "error" -> MaterialTheme.colorScheme.error
        "success" -> Color(0xFF4CAF50)
        else -> MaterialTheme.colorScheme.primary
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                typeIcon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = typeColor
            )
        },
        title = {
            Text(
                text = announcement.title,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = announcement.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            if (announcement.actionUrl != null && announcement.actionText != null) {
                PremiumButton(
                    onClick = {
                        onAction?.invoke(announcement.actionUrl)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(announcement.actionText)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(Strings.cloudDismiss)
                }
            }
        },
        dismissButton = {
            if (announcement.actionUrl != null) {
                TextButton(onClick = onDismiss) {
                    Text(Strings.cloudDismiss)
                }
            }
        }
    )
}
