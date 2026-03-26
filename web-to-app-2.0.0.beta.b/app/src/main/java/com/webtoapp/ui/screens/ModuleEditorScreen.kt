package com.webtoapp.ui.screens

import android.widget.Toast
import com.webtoapp.ui.components.PremiumButton
import com.webtoapp.ui.components.PremiumFilterChip
import com.webtoapp.core.logging.AppLogger
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalTextStyle
import com.webtoapp.core.extension.*
import com.webtoapp.core.i18n.Strings
import kotlinx.coroutines.launch
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.EnhancedElevatedCard
import com.webtoapp.ui.components.PremiumTextField

import com.webtoapp.ui.components.PremiumSwitch
/**
 * 模块编辑器页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleEditorScreen(
    moduleId: String?,  // null 表示新建
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val extensionManager = remember { ExtensionManager.getInstance(context) }
    
    // Decode moduleId in case it was URL-encoded during navigation
    val decodedModuleId = remember(moduleId) {
        moduleId?.let { 
            try {
                java.net.URLDecoder.decode(it, "UTF-8")
            } catch (e: Exception) {
                it // Use original if decoding fails
            }
        }
    }
    
    // Load现有模块或创建新模块
    val existingModule = remember(decodedModuleId) {
        decodedModuleId?.let { id ->
            try {
                extensionManager.getAllModules().find { it.id == id }
            } catch (e: Exception) {
                AppLogger.e("ModuleEditorScreen", "Failed to load module: $id", e)
                null
            }
        }
    }
    
    // Edit state
    var name by remember { mutableStateOf(existingModule?.name ?: "") }
    var description by remember { mutableStateOf(existingModule?.description ?: "") }
    var icon by remember { mutableStateOf(existingModule?.icon ?: "extension") }
    var category by remember { mutableStateOf(existingModule?.category ?: ModuleCategory.OTHER) }
    var tags by remember { mutableStateOf(existingModule?.tags?.joinToString(", ") ?: "") }
    var code by remember { mutableStateOf(existingModule?.code ?: "") }
    var cssCode by remember { mutableStateOf(existingModule?.cssCode ?: "") }
    var runAt by remember { mutableStateOf(existingModule?.runAt ?: ModuleRunTime.DOCUMENT_END) }
    var urlMatches by remember { mutableStateOf(existingModule?.urlMatches ?: emptyList()) }
    var permissions by remember { mutableStateOf(existingModule?.permissions?.toSet() ?: emptySet()) }
    var configItems by remember { mutableStateOf(existingModule?.configItems ?: emptyList()) }
    var versionName by remember { mutableStateOf(existingModule?.version?.name ?: "1.0.0") }
    var authorName by remember { mutableStateOf(existingModule?.author?.name ?: "") }
    var uiConfig by remember { mutableStateOf(existingModule?.uiConfig ?: ModuleUiConfig()) }
    var runMode by remember { mutableStateOf(existingModule?.runMode ?: ModuleRunMode.INTERACTIVE) }
    
    var currentTab by remember { mutableIntStateOf(0) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showRunAtDialog by remember { mutableStateOf(false) }
    var showRunModeDialog by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }
    var showUrlMatchDialog by remember { mutableStateOf(false) }
    var showConfigItemDialog by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showUiTypeDialog by remember { mutableStateOf(false) }
    
    val tabs = listOf(Strings.basicInfo, Strings.code, Strings.advancedSettings)
    
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (moduleId == null) Strings.createModule else Strings.editModule) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = Strings.close)
                    }
                },
                actions = {
                    // 模板按钮（仅新建时显示）
                    if (moduleId == null) {
                        IconButton(onClick = { showTemplateDialog = true }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = Strings.useTemplate)
                        }
                    }
                    TextButton(
                        onClick = {
                            // Verify并保存
                            if (name.isBlank()) {
                                Toast.makeText(context, Strings.pleaseEnterModuleName, Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            if (code.isBlank() && cssCode.isBlank()) {
                                Toast.makeText(context, Strings.pleaseEnterCodeContent, Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            
                            val module = ExtensionModule(
                                id = existingModule?.id ?: java.util.UUID.randomUUID().toString(),
                                name = name,
                                description = description,
                                icon = icon,
                                category = category,
                                tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
                                version = ModuleVersion(
                                    code = (existingModule?.version?.code ?: 0) + 1,
                                    name = versionName
                                ),
                                author = if (authorName.isNotBlank()) ModuleAuthor(authorName) else null,
                                code = code,
                                cssCode = cssCode,
                                runAt = runAt,
                                urlMatches = urlMatches,
                                permissions = permissions.toList(),
                                configItems = configItems,
                                enabled = existingModule?.enabled ?: true,
                                builtIn = false,
                                createdAt = existingModule?.createdAt ?: System.currentTimeMillis(),
                                uiConfig = uiConfig,
                                runMode = runMode
                            )
                            
                            scope.launch {
                                extensionManager.addModule(module).onSuccess {
                                    Toast.makeText(context, Strings.saveSuccess, Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                }.onFailure { e ->
                                    Toast.makeText(context, "${Strings.saveFailed}: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text(Strings.save)
                    }
                }
            )
        }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 美化的 Tab 栏
            val tabIcons = listOf(
                Icons.Outlined.Info to Icons.Filled.Info,
                Icons.Outlined.Code to Icons.Filled.Code,
                Icons.Outlined.Settings to Icons.Filled.Settings
            )
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = currentTab == index
                        Surface(
                            onClick = { currentTab = index },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                            modifier = Modifier.weight(weight = 1f, fill = true)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    if (isSelected) tabIcons[index].second else tabIcons[index].first,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Text(
                                    title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            when (currentTab) {
                0 -> BasicInfoTab(
                    name = name,
                    onNameChange = { name = it },
                    description = description,
                    onDescriptionChange = { description = it },
                    icon = icon,
                    onIconClick = { showIconPicker = true },
                    category = category,
                    onCategoryClick = { showCategoryDialog = true },
                    tags = tags,
                    onTagsChange = { tags = it },
                    versionName = versionName,
                    onVersionNameChange = { versionName = it },
                    authorName = authorName,
                    onAuthorNameChange = { authorName = it }
                )
                1 -> CodeTab(
                    code = code,
                    onCodeChange = { code = it },
                    cssCode = cssCode,
                    onCssCodeChange = { cssCode = it }
                )
                2 -> AdvancedTab(
                    runAt = runAt,
                    onRunAtClick = { showRunAtDialog = true },
                    runMode = runMode,
                    onRunModeClick = { showRunModeDialog = true },
                    permissions = permissions,
                    onPermissionsClick = { showPermissionsDialog = true },
                    urlMatches = urlMatches,
                    onUrlMatchesClick = { showUrlMatchDialog = true },
                    configItems = configItems,
                    onConfigItemsClick = { showConfigItemDialog = true },
                    uiConfig = uiConfig,
                    onUiTypeClick = { showUiTypeDialog = true }
                )
            }
        }
    }
    
    // 分类选择对话框
    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text(Strings.selectCategory) },
            text = {
                LazyColumn {
                    items(ModuleCategory.values().toList()) { cat ->
                        ListItem(
                            headlineContent = { Text(cat.getDisplayName()) },
                            leadingContent = { Icon(com.webtoapp.util.SvgIconMapper.getIcon(cat.icon), contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary) },
                            supportingContent = { Text(cat.getDescription()) },
                            modifier = Modifier.clickable {
                                category = cat
                                showCategoryDialog = false
                            },
                            trailingContent = {
                                if (category == cat) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
    
    // Execute时机选择对话框
    if (showRunAtDialog) {
        AlertDialog(
            onDismissRequest = { showRunAtDialog = false },
            title = { Text(Strings.runTime) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModuleRunTime.values().forEach { time ->
                        val isSelected = runAt == time
                        val timeIcon = when (time) {
                            ModuleRunTime.DOCUMENT_START -> "play_arrow"
                            ModuleRunTime.DOCUMENT_END -> "description"
                            ModuleRunTime.DOCUMENT_IDLE -> "hourglass_top"
                            ModuleRunTime.CONTEXT_MENU -> "menu"
                            ModuleRunTime.BEFORE_UNLOAD -> "exit_to_app"
                        }
                        Surface(
                            onClick = {
                                runAt = time
                                showRunAtDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    com.webtoapp.util.SvgIconMapper.getIcon(timeIcon),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                    Text(
                                        time.getDisplayName(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        time.getDescription(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
    
    // 运行模式选择对话框
    if (showRunModeDialog) {
        AlertDialog(
            onDismissRequest = { showRunModeDialog = false },
            title = { Text(Strings.runModeLabel) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModuleRunMode.values().forEach { mode ->
                        val isSelected = runMode == mode
                        Surface(
                            onClick = {
                                runMode = mode
                                showRunModeDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    com.webtoapp.util.SvgIconMapper.getIcon(mode.getIcon()),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = if (isSelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                    Text(
                                        mode.getDisplayName(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        mode.getDescription(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
    
    // Permission选择对话框
    if (showPermissionsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionsDialog = false },
            title = { Text(Strings.requiredPermissions) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(ModulePermission.values().toList()) { perm ->
                        val isChecked = perm in permissions
                        val permIcon = when (perm) {
                            ModulePermission.DOM_ACCESS, ModulePermission.DOM_OBSERVE -> "code"
                            ModulePermission.CSS_INJECT -> "palette"
                            ModulePermission.STORAGE, ModulePermission.CACHE -> "save"
                            ModulePermission.COOKIE, ModulePermission.INDEXED_DB -> "cookie"
                            ModulePermission.NETWORK, ModulePermission.WEBSOCKET, ModulePermission.FETCH_INTERCEPT -> "cloud"
                            ModulePermission.CLIPBOARD -> "content_paste"
                            ModulePermission.NOTIFICATION, ModulePermission.ALERT -> "notifications"
                            ModulePermission.KEYBOARD, ModulePermission.MOUSE, ModulePermission.TOUCH -> "mouse"
                            ModulePermission.LOCATION -> "location_on"
                            ModulePermission.CAMERA -> "camera"
                            ModulePermission.MICROPHONE -> "mic"
                            ModulePermission.DEVICE_INFO -> "phone_android"
                            ModulePermission.MEDIA, ModulePermission.FULLSCREEN, ModulePermission.PICTURE_IN_PICTURE -> "movie"
                            ModulePermission.SCREEN_CAPTURE -> "screenshot"
                            ModulePermission.DOWNLOAD, ModulePermission.FILE_ACCESS -> "folder"
                            ModulePermission.EVAL -> "terminal"
                            ModulePermission.IFRAME, ModulePermission.WINDOW_OPEN -> "open_in_new"
                            ModulePermission.HISTORY, ModulePermission.NAVIGATION -> "explore"
                        }
                        Surface(
                            onClick = {
                                permissions = if (isChecked) permissions - perm else permissions + perm
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isChecked)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    com.webtoapp.util.SvgIconMapper.getIcon(permIcon),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (perm.dangerous)
                                        MaterialTheme.colorScheme.error
                                    else if (isChecked)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            perm.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (perm.dangerous) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.errorContainer
                                            ) {
                                                Text(
                                                    Strings.sensitive,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        perm.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        permissions = if (it) permissions + perm else permissions - perm
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPermissionsDialog = false }) {
                    Text(Strings.confirm)
                }
            }
        )
    }
    
    // Icon选择对话框
    if (showIconPicker) {
        IconPickerDialog(
            currentIcon = icon,
            onIconSelected = {
                icon = it
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false }
        )
    }
    
    // URL 匹配规则对话框
    if (showUrlMatchDialog) {
        UrlMatchDialog(
            urlMatches = urlMatches,
            onUrlMatchesChange = { urlMatches = it },
            onDismiss = { showUrlMatchDialog = false }
        )
    }
    
    // Configure项对话框
    if (showConfigItemDialog) {
        ConfigItemsDialog(
            configItems = configItems,
            onConfigItemsChange = { configItems = it },
            onDismiss = { showConfigItemDialog = false }
        )
    }
    
    // 模板选择对话框
    if (showTemplateDialog) {
         TemplateSelectionDialog(
            onTemplateSelected = { template ->
                // App模板
                name = template.name
                description = template.description
                icon = template.icon
                category = template.category
                code = template.code
                cssCode = template.cssCode
                configItems = template.configItems
                showTemplateDialog = false
            },
            onDismiss = { showTemplateDialog = false }
        )
    }
    
    // UI 类型选择对话框
    if (showUiTypeDialog) {
        UiTypeSelectionDialog(
            currentUiConfig = uiConfig,
            onUiConfigChange = { uiConfig = it },
            onDismiss = { showUiTypeDialog = false }
        )
    }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BasicInfoTab(
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    icon: String,
    onIconClick: () -> Unit,
    category: ModuleCategory,
    onCategoryClick: () -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit,
    versionName: String,
    onVersionNameChange: (String) -> Unit,
    authorName: String,
    onAuthorNameChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Module标识卡片
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon选择器
                Surface(
                    onClick = onIconClick,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        2.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        com.webtoapp.ui.components.ModuleIcon(
                            iconId = icon,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        // 编辑指示器
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 4.dp, y = 4.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                
                // Name输入
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Text(
                        Strings.moduleNameRequired,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PremiumTextField(
                        value = name,
                        onValueChange = onNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(
                                Strings.inputModuleName,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            ) 
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
        
        // Description输入
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        Strings.description,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                PremiumTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(Strings.briefModuleDescription) },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
        
        // 分类选择
        Surface(
            onClick = onCategoryClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(com.webtoapp.util.SvgIconMapper.getIcon(category.icon), contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Text(
                        Strings.category,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        category.getDisplayName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 标签输入
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Label,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        Strings.tags,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                PremiumTextField(
                    value = tags,
                    onValueChange = onTagsChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(Strings.tagsHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 标签预览
                if (tags.isNotBlank()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tagList = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        items(tagList) { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    "#$tag",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Version和作者信息
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        Strings.moduleInfo,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PremiumTextField(
                        value = versionName,
                        onValueChange = onVersionNameChange,
                        modifier = Modifier.weight(weight = 1f, fill = true),
                        label = { Text(Strings.versionLabel) },
                        placeholder = { Text("1.0.0") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Numbers,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                    
                    PremiumTextField(
                        value = authorName,
                        onValueChange = onAuthorNameChange,
                        modifier = Modifier.weight(weight = 1f, fill = true),
                        label = { Text(Strings.author) },
                        placeholder = { Text(Strings.yourName) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
        
        // 底部间距
        Spacer(modifier = Modifier.height(16.dp))
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodeTab(
    code: String,
    onCodeChange: (String) -> Unit,
    cssCode: String,
    onCssCodeChange: (String) -> Unit
) {
    var showJsTab by remember { mutableStateOf(true) }
    var showCodeSnippetSelector by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // JS/CSS 切换 + 代码块按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PremiumFilterChip(
                    selected = showJsTab,
                    onClick = { showJsTab = true },
                    label = { Text("JavaScript") },
                    leadingIcon = if (showJsTab) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
                PremiumFilterChip(
                    selected = !showJsTab,
                    onClick = { showJsTab = false },
                    label = { Text("CSS") },
                    leadingIcon = if (!showJsTab) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }
            
            // 代码块库按钮
            if (showJsTab) {
                FilledTonalButton(
                    onClick = { showCodeSnippetSelector = true },
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.codeSnippets)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 代码提示
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    if (showJsTab) Strings.availableFunctions else Strings.cssTips,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (showJsTab) Strings.jsFunctionsHint else Strings.cssHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 代码编辑器
        PremiumTextField(
            value = if (showJsTab) code else cssCode,
            onValueChange = { if (showJsTab) onCodeChange(it) else onCssCodeChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 1f, fill = true),
            label = { Text(if (showJsTab) Strings.javascriptCode else Strings.cssCode) },
            placeholder = {
                Text(
                    if (showJsTab) Strings.jsCodePlaceholder else Strings.cssCodePlaceholder
                )
            },
            textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        )
    }
    
    // 代码块选择器对话框
    if (showCodeSnippetSelector) {
        com.webtoapp.ui.components.CodeSnippetSelectorDialog(
            onDismiss = { showCodeSnippetSelector = false },
            onSelect = { snippet ->
                // 插入代码到当前位置
                val newCode = if (code.isBlank()) {
                    snippet.code
                } else {
                    code + "\n\n" + snippet.code
                }
                onCodeChange(newCode)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedTab(
    runAt: ModuleRunTime,
    onRunAtClick: () -> Unit,
    runMode: ModuleRunMode,
    onRunModeClick: () -> Unit,
    permissions: Set<ModulePermission>,
    onPermissionsClick: () -> Unit,
    urlMatches: List<UrlMatchRule>,
    onUrlMatchesClick: () -> Unit,
    configItems: List<ModuleConfigItem>,
    onConfigItemsClick: () -> Unit,
    uiConfig: ModuleUiConfig,
    onUiTypeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // UI 类型配置
        EnhancedElevatedCard(
            onClick = onUiTypeClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItem(
                headlineContent = { Text(Strings.uiTypeConfig) },
                supportingContent = { Text(uiConfig.type.getDisplayName()) },
                leadingContent = { Icon(Icons.Default.Widgets, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
            )
        }
        
        // 运行模式
        EnhancedElevatedCard(
            onClick = onRunModeClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItem(
                headlineContent = { Text(Strings.runModeLabel) },
                supportingContent = { Text("${runMode.getDisplayName()} - ${runMode.getDescription()}") },
                leadingContent = { Icon(Icons.Default.PlayCircle, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
            )
        }
        
        // Execute时机
        EnhancedElevatedCard(
            onClick = onRunAtClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItem(
                headlineContent = { Text(Strings.runTime) },
                supportingContent = { Text(runAt.getDisplayName()) },
                leadingContent = { Icon(Icons.Default.Schedule, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
            )
        }
        
        // Permission
        EnhancedElevatedCard(
            onClick = onPermissionsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItem(
                headlineContent = { Text(Strings.requiredPermissions) },
                supportingContent = {
                    Text(
                        if (permissions.isEmpty()) Strings.noSpecialPermissions
                        else permissions.joinToString { it.displayName }
                    )
                },
                leadingContent = { Icon(Icons.Default.Security, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
            )
        }
        
        // URL 匹配
        EnhancedElevatedCard(
            onClick = onUrlMatchesClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItem(
                headlineContent = { Text(Strings.urlMatchRules) },
                supportingContent = {
                    Text(
                        if (urlMatches.isEmpty()) Strings.matchAllWebsites
                        else Strings.rulesCount.replace("%d", urlMatches.size.toString())
                    )
                },
                leadingContent = { Icon(Icons.Default.Link, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
            )
        }
        
        // Configure项
        EnhancedElevatedCard(
            onClick = onConfigItemsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItem(
                headlineContent = { Text(Strings.userConfigItems) },
                supportingContent = {
                    Text(
                        if (configItems.isEmpty()) Strings.noConfigItems
                        else Strings.configItemsCount.replace("%d", configItems.size.toString())
                    )
                },
                leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) }
            )
        }
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        // 帮助信息
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    Strings.developerGuide,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    Strings.developerGuideContent,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 图标选择对话框
 */
@Composable
fun IconPickerDialog(
    currentIcon: String,
    onIconSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val icons = listOf(
        "package", "block", "palette", "bolt", "analytics", "mouse", "movie", "lock", "wrench",
        "dark_mode", "list", "clipboard", "image", "fast_forward", "shield", "book", "font_download", "globe",
        "target", "lightbulb", "wrench", "settings", "gaming", "music_note", "phone_android", "computer", "star",
        "fire", "diamond", "gift", "trophy", "festival", "theater_comedy", "palette", "movie", "camera"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.selectIcon) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                icons.chunked(6).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { iconId ->
                            Surface(
                                onClick = { onIconSelected(iconId) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (iconId == currentIcon)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surface
                            ) {
                                Icon(
                                    com.webtoapp.util.SvgIconMapper.getIcon(iconId),
                                    contentDescription = iconId,
                                    modifier = Modifier.padding(12.dp).size(24.dp),
                                    tint = if (iconId == currentIcon)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.cancel)
            }
        }
    )
}

/**
 * URL 匹配规则对话框
 */
@Composable
fun UrlMatchDialog(
    urlMatches: List<UrlMatchRule>,
    onUrlMatchesChange: (List<UrlMatchRule>) -> Unit,
    onDismiss: () -> Unit
) {
    var newPattern by remember { mutableStateOf("") }
    var isRegex by remember { mutableStateOf(false) }
    var isExclude by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.urlMatchRules) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // 现有规则
                if (urlMatches.isNotEmpty()) {
                    urlMatches.forEachIndexed { index, rule ->
                        ListItem(
                            headlineContent = { Text(rule.pattern) },
                            supportingContent = {
                                Text(
                                    buildString {
                                        if (rule.isRegex) append("${Strings.regex} ")
                                        if (rule.exclude) append(Strings.exclude)
                                        else append(Strings.include)
                                    }
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    onUrlMatchesChange(urlMatches.filterIndexed { i, _ -> i != index })
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = Strings.delete)
                                }
                            }
                        )
                    }
                    HorizontalDivider()
                }
                
                // 添加新规则
                PremiumTextField(
                    value = newPattern,
                    onValueChange = { newPattern = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(Strings.urlPattern) },
                    placeholder = { Text("*.example.com/*") },
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isRegex, onCheckedChange = { isRegex = it })
                        Text(Strings.regexExpression)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isExclude, onCheckedChange = { isExclude = it })
                        Text(Strings.excludeRule)
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = {
                        if (newPattern.isNotBlank()) {
                            onUrlMatchesChange(urlMatches + UrlMatchRule(newPattern, isRegex, isExclude))
                            newPattern = ""
                            isRegex = false
                            isExclude = false
                        }
                    },
                    enabled = newPattern.isNotBlank()
                ) {
                    Text(Strings.add)
                }
                TextButton(onClick = onDismiss) {
                    Text(Strings.done)
                }
            }
        }
    )
}

/**
 * 配置项管理对话框
 */
@Composable
fun ConfigItemsDialog(
    configItems: List<ModuleConfigItem>,
    onConfigItemsChange: (List<ModuleConfigItem>) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.userConfigItems) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (configItems.isEmpty()) {
                    Text(
                        Strings.noConfigItemsHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    configItems.forEachIndexed { index, item ->
                        ListItem(
                            headlineContent = { Text(item.name) },
                            supportingContent = { Text("${item.type.name} · ${item.key}") },
                            trailingContent = {
                                IconButton(onClick = {
                                    onConfigItemsChange(configItems.filterIndexed { i, _ -> i != index })
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = Strings.delete)
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(Strings.add)
                }
                TextButton(onClick = onDismiss) {
                    Text(Strings.done)
                }
            }
        }
    )
    
    if (showAddDialog) {
        AddConfigItemDialog(
            onAdd = { item ->
                onConfigItemsChange(configItems + item)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConfigItemDialog(
    onAdd: (ModuleConfigItem) -> Unit,
    onDismiss: () -> Unit
) {
    var key by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ConfigItemType.TEXT) }
    var defaultValue by remember { mutableStateOf("") }
    var required by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.addConfigItem) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PremiumTextField(
                    value = key,
                    onValueChange = { key = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(Strings.keyNameRequired) },
                    placeholder = { Text(Strings.keyNamePlaceholder) },
                    singleLine = true
                )
                
                PremiumTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(Strings.displayNameRequired) },
                    placeholder = { Text(Strings.displayNamePlaceholder) },
                    singleLine = true
                )
                
                PremiumTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(Strings.explanationLabel) },
                    placeholder = { Text(Strings.configExplanationPlaceholder) },
                    singleLine = true
                )
                
                // Class型选择
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    PremiumTextField(
                        value = type.name,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text(Strings.typeLabel) },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ConfigItemType.values().forEach { itemType ->
                            DropdownMenuItem(
                                text = { Text(itemType.name) },
                                onClick = {
                                    type = itemType
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                PremiumTextField(
                    value = defaultValue,
                    onValueChange = { defaultValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(Strings.defaultValueLabel) },
                    singleLine = true
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = required, onCheckedChange = { required = it })
                    Text(Strings.requiredField)
                }
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = {
                    onAdd(ModuleConfigItem(
                        key = key,
                        name = name,
                        description = description,
                        type = type,
                        defaultValue = defaultValue,
                        required = required
                    ))
                },
                enabled = key.isNotBlank() && name.isNotBlank()
            ) {
                Text(Strings.add)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.cancel)
            }
        }
    )
}


/**
 * 模板选择对话框
 */
@Composable
fun TemplateSelectionDialog(
    onTemplateSelected: (ModuleTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    val templates = remember { ModuleTemplates.getAll() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.selectTemplate) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates) { template ->
                    Surface(
                        onClick = { onTemplateSelected(template) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(com.webtoapp.util.SvgIconMapper.getIcon(template.icon), contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                Text(
                                    template.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    template.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    template.category.getDisplayName(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.cancel)
            }
        }
    )
}

/**
 * UI 类型选择对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UiTypeSelectionDialog(
    currentUiConfig: ModuleUiConfig,
    onUiConfigChange: (ModuleUiConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(currentUiConfig.type) }
    var position by remember { mutableStateOf(currentUiConfig.position) }
    var draggable by remember { mutableStateOf(currentUiConfig.draggable) }
    var toolbarOrientation by remember { mutableStateOf(currentUiConfig.toolbarOrientation) }
    var sidebarPosition by remember { mutableStateOf(currentUiConfig.sidebarPosition) }
    var showPositionSelector by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.uiTypeConfig) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // UI 类型选择
                Text(
                    Strings.selectUiType,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                ModuleUiType.values().forEach { uiType ->
                    Surface(
                        onClick = { selectedType = uiType },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedType == uiType) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                com.webtoapp.util.SvgIconMapper.getIcon(uiType.getIcon()),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = if (selectedType == uiType)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                                Text(
                                    uiType.getDisplayName(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    uiType.getDescription(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selectedType == uiType) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // 通用配置
                Text(
                    Strings.commonConfig,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // 位置选择
                if (selectedType != ModuleUiType.SIDEBAR && selectedType != ModuleUiType.BOTTOM_BAR) {
                    EnhancedElevatedCard(
                        onClick = { showPositionSelector = !showPositionSelector },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ListItem(
                            headlineContent = { Text(Strings.uiPosition) },
                            supportingContent = { Text(position.getDisplayName()) },
                            trailingContent = { 
                                Icon(
                                    if (showPositionSelector) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                ) 
                            }
                        )
                    }
                    
                    if (showPositionSelector) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                UiPosition.values().forEach { pos ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                position = pos
                                                showPositionSelector = false
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = position == pos,
                                            onClick = { 
                                                position = pos
                                                showPositionSelector = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(pos.getDisplayName())
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 可拖动开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Strings.draggableSwitch)
                    PremiumSwitch(checked = draggable, onCheckedChange = { draggable = it })
                }
                
                // Toolbar特定配置
                if (selectedType == ModuleUiType.FLOATING_TOOLBAR) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        Strings.toolbarConfig,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PremiumFilterChip(
                            selected = toolbarOrientation == ToolbarOrientation.HORIZONTAL,
                            onClick = { toolbarOrientation = ToolbarOrientation.HORIZONTAL },
                            label = { Text(ToolbarOrientation.HORIZONTAL.getDisplayName()) }
                        )
                        PremiumFilterChip(
                            selected = toolbarOrientation == ToolbarOrientation.VERTICAL,
                            onClick = { toolbarOrientation = ToolbarOrientation.VERTICAL },
                            label = { Text(ToolbarOrientation.VERTICAL.getDisplayName()) }
                        )
                    }
                }
                
                // 侧边栏特定配置
                if (selectedType == ModuleUiType.SIDEBAR) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        Strings.sidebarConfig,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PremiumFilterChip(
                            selected = sidebarPosition == SidebarPosition.LEFT,
                            onClick = { sidebarPosition = SidebarPosition.LEFT },
                            label = { Text(SidebarPosition.LEFT.getDisplayName()) }
                        )
                        PremiumFilterChip(
                            selected = sidebarPosition == SidebarPosition.RIGHT,
                            onClick = { sidebarPosition = SidebarPosition.RIGHT },
                            label = { Text(SidebarPosition.RIGHT.getDisplayName()) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            PremiumButton(
                onClick = {
                    onUiConfigChange(
                        currentUiConfig.copy(
                            type = selectedType,
                            position = position,
                            draggable = draggable,
                            toolbarOrientation = toolbarOrientation,
                            sidebarPosition = sidebarPosition
                        )
                    )
                    onDismiss()
                }
            ) {
                Text(Strings.confirm)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.cancel)
            }
        }
    )
}
