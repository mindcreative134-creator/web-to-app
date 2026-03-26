package com.webtoapp.ui.screens

import androidx.compose.animation.*
import com.webtoapp.ui.components.PremiumButton
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.webtoapp.core.cloud.ActivationRecord
import com.webtoapp.core.cloud.RedeemPreview
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.viewmodel.CloudViewModel
import com.webtoapp.ui.viewmodel.FormState
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumOutlinedButton
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivationCodeScreen(
    cloudViewModel: CloudViewModel,
    onBack: () -> Unit
) {
    val redeemState by cloudViewModel.redeemState.collectAsStateWithLifecycle()
    val previewState by cloudViewModel.previewState.collectAsStateWithLifecycle()
    val redeemPreview by cloudViewModel.redeemPreview.collectAsStateWithLifecycle()
    val history by cloudViewModel.activationHistory.collectAsStateWithLifecycle()
    val message by cloudViewModel.message.collectAsStateWithLifecycle()

    var code by remember { mutableStateOf("") }
    var showPreviewDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { cloudViewModel.loadActivationHistory() }

    LaunchedEffect(redeemState) {
        when (redeemState) {
            is FormState.Success -> {
                snackbarHostState.showSnackbar((redeemState as FormState.Success).message)
                cloudViewModel.loadActivationHistory()
                cloudViewModel.resetRedeemState()
                code = ""
            }
            is FormState.Error -> {
                snackbarHostState.showSnackbar((redeemState as FormState.Error).message)
                cloudViewModel.resetRedeemState()
            }
            else -> {}
        }
    }

    // 预览结果处理
    LaunchedEffect(previewState) {
        when (previewState) {
            is FormState.Success -> showPreviewDialog = true
            is FormState.Error -> {
                snackbarHostState.showSnackbar((previewState as FormState.Error).message)
                cloudViewModel.resetPreviewState()
            }
            else -> {}
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            cloudViewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(Strings.cloudActivationCode) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, Strings.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        LazyColumn(
            modifier = Modifier,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 输入区
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = Strings.cloudRedeemTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = Strings.cloudRedeemDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' } },
                        placeholder = {
                            Text(
                                "XXXX-XXXX-XXXX-XXXX",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    letterSpacing = 2.sp,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center
                        )
                    )

                    // 预览 + 兑换按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PremiumOutlinedButton(
                            onClick = { cloudViewModel.previewRedeem(code) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = code.length >= 4 && previewState !is FormState.Loading && redeemState !is FormState.Loading
                        ) {
                            if (previewState is FormState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Icon(Icons.Outlined.Preview, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("预览", fontWeight = FontWeight.Medium)
                        }

                        PremiumButton(
                            onClick = { cloudViewModel.redeemCode(code) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = code.length >= 4 && redeemState !is FormState.Loading
                        ) {
                            if (redeemState is FormState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = if (redeemState is FormState.Loading)
                                    Strings.cloudRedeeming else Strings.cloudRedeemBtn,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Pro 特权
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Pro ${Strings.cloudProBenefits}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val benefits = listOf(
                        Strings.cloudBenefitCloud,
                        Strings.cloudBenefitPriority,
                        Strings.cloudBenefitDevices,
                        Strings.cloudBenefitAnalytics
                    )
                    benefits.forEach { benefit ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Check,
                                null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = benefit,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 兑换历史
            if (history.isNotEmpty()) {
                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
                item {
                    Text(
                        text = Strings.cloudRedeemHistory,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                history.forEach { record ->
                    item {
                        HistoryRow(record)
                    }
                }
            }
        }
        }

        // 预览对话框
        if (showPreviewDialog && redeemPreview != null) {
            RedeemPreviewDialog(
                preview = redeemPreview!!,
                onConfirm = {
                    showPreviewDialog = false
                    cloudViewModel.resetPreviewState()
                    cloudViewModel.redeemCode(code)
                },
                onDismiss = {
                    showPreviewDialog = false
                    cloudViewModel.resetPreviewState()
                }
            )
        }
    }
}

@Composable
private fun HistoryRow(record: ActivationRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
            Text(
                text = record.planType.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                record.createdAt?.let {
                    Text(
                        it.take(10),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                record.note?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        record.proEnd?.let {
            Text(
                text = "${Strings.cloudValidUntil} ${it.take(10)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun RedeemPreviewDialog(
    preview: RedeemPreview,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (preview.isUpgrade) Icons.Outlined.TrendingUp else Icons.Outlined.SwapHoriz,
                null,
                tint = if (preview.isUpgrade) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                if (preview.isUpgrade) "🚀 等级升级" else "兑换预览",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 激活码信息
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("激活码", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${preview.codeTier.uppercase()} · ${preview.codePlanType}" +
                                if (preview.durationDays > 0) " · ${preview.durationDays}天" else " · 终身",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 当前 → 新状态
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 当前
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("当前", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            preview.currentTier.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (preview.currentIsLifetime) {
                            Text("终身", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFD700))
                        } else {
                            preview.currentExpiresAt?.let {
                                Text(it.take(10), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Icon(Icons.Outlined.ArrowForward, null, modifier = Modifier.size(24.dp),
                        tint = if (preview.isUpgrade) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary)

                    // 新状态
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("兑换后", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                        Text(
                            preview.newTier.uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (preview.newIsLifetime) {
                            Text("终身", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFD700))
                        } else {
                            preview.newExpiresAt?.let {
                                Text(it.take(10), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                if (preview.isUpgrade) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "✨ 此操作将升级你的会员等级",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4CAF50),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("确认兑换", fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
