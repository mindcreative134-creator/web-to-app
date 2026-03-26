package com.webtoapp.ui.screens.community

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.cloud.*
import com.webtoapp.core.auth.TokenManager
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.ThemedBackgroundBox
import com.webtoapp.ui.components.UserTitleBadges
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import com.webtoapp.ui.viewmodel.CommunityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    onNavigateToUser: (Int) -> Unit,
    onNavigateToModule: (Int) -> Unit,
    onNavigateToPost: (Int) -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    isTabVisible: Boolean = true
) {
    val apiClient: CloudApiClient = koinInject()
    val tokenManager: TokenManager = koinInject()
    val communityViewModel: CommunityViewModel = koinViewModel()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Feed state — from ViewModel (survives config changes)
    val posts by communityViewModel.posts.collectAsState()
    val isLoading by communityViewModel.feedLoading.collectAsState()
    val isLoadingMore by communityViewModel.feedLoadingMore.collectAsState()
    val isRefreshing by communityViewModel.feedRefreshing.collectAsState()
    val selectedTag by communityViewModel.selectedTag.collectAsState()
    val unreadCount by communityViewModel.unreadCount.collectAsState()
    var showCreatePost by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val availableTags = remember { listOf(
        "html", "css", "javascript", "typescript", "vue", "react", "angular",
        "svelte", "nextjs", "nuxtjs", "nodejs", "python", "php", "go",
        "webtoapp", "pwa", "responsive", "animation", "game", "tool"
    ) }

    // ViewModel message → snackbar
    val vmMessage by communityViewModel.message.collectAsState()
    LaunchedEffect(vmMessage) {
        vmMessage?.let {
            snackbarHostState.showSnackbar(it)
            communityViewModel.clearMessage()
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        if (posts.isEmpty()) communityViewModel.loadPosts()
        communityViewModel.loadUnreadCount()
    }

    // P3 #16: Detect scroll to bottom → load more
    val lastVisibleIndex by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
    }
    LaunchedEffect(lastVisibleIndex) {
        if (lastVisibleIndex >= posts.size - 3 && !isLoadingMore && posts.isNotEmpty()) {
            communityViewModel.loadMorePosts()
        }
    }

    // P3 #17: Periodic heartbeat (every 2 minutes) — only when tab is visible
    LaunchedEffect(isTabVisible) {
        if (!isTabVisible) return@LaunchedEffect
        while (true) {
            try { apiClient.sendHeartbeat() } catch (_: Exception) {}
            kotlinx.coroutines.delay(120_000L)
        }
    }

    // P3 #15: Pull-to-refresh (manual via refresh button; compatible with compose-bom 2024.02)

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        Strings.tabCommunity,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Search
                    IconButton(onClick = { showSearchSheet = true }) {
                        Icon(Icons.Outlined.Search, Strings.communitySearch, Modifier.size(21.dp))
                    }
                    // Notifications with badge
                    IconButton(onClick = { onNavigateToNotifications() }) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ) {
                                        Text(
                                            if (unreadCount > 99) "99+" else "$unreadCount",
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Notifications, Strings.communityNotifications, Modifier.size(21.dp))
                        }
                    }
                    // Favorites
                    IconButton(onClick = { onNavigateToFavorites() }) {
                        Icon(Icons.Outlined.BookmarkBorder, Strings.communityBookmarks, Modifier.size(21.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (tokenManager.isLoggedIn()) {
                        showCreatePost = true
                    } else {
                        scope.launch { snackbarHostState.showSnackbar(Strings.communityLoginToPost) }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(Icons.Outlined.Edit, Strings.communityCreatePost)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
                // ─── Tag Filter Bar ───
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedTag == null,
                            onClick = { communityViewModel.setSelectedTag(null) },
                            label = { Text(Strings.communityAllTags, fontSize = 12.sp) },
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    items(availableTags) { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { communityViewModel.setSelectedTag(if (selectedTag == tag) null else tag) },
                            label = { Text(tag, fontSize = 12.sp) },
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(8.dp),
                            leadingIcon = if (selectedTag == tag) {
                                { Icon(Icons.Filled.Check, null, Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }

                // ─── Feed ───
                if (isLoading && posts.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.5.dp)
                    }
                } else if (posts.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Forum, null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(Strings.communityNoPosts, fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    // P3 #15: Feed container
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState
                        ) {
                            itemsIndexed(posts, key = { _, p -> p.id }, contentType = { _, _ -> "post" }) { index, post ->
                                PostCard(
                                    post = post,
                                    onPostClick = { onNavigateToPost(post.id) },
                                    onLike = { communityViewModel.togglePostLike(post.id) },
                                    onShare = { communityViewModel.sharePost(post.id) },
                                    onComment = { onNavigateToPost(post.id) },
                                    onReport = { communityViewModel.reportPost(post.id) },
                                    onAuthorClick = { onNavigateToUser(post.authorId) },
                                    onAppLinkClick = { moduleId -> onNavigateToModule(moduleId) },
                                    modifier = Modifier.animateItem()
                                )
                                if (index < posts.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                }
                            }
                            // P3 #16: Loading more indicator
                            if (isLoadingMore) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(80.dp)) }
                        }

                        // P3 #15: Refresh indicator
                        if (isRefreshing) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                            )
                        }
                    }
                }
        }
    }

    // ─── Create Post Sheet ───
    if (showCreatePost) {
        CreatePostSheet(
            apiClient = apiClient,
            availableTags = availableTags,
            onDismiss = { showCreatePost = false },
            onPosted = {
                showCreatePost = false
                communityViewModel.refreshPosts()
                scope.launch { snackbarHostState.showSnackbar(Strings.communityPostSuccess) }
            }
        )
    }

    // ─── Search Users Sheet ───
    if (showSearchSheet) {
        SearchUsersSheet(
            communityViewModel = communityViewModel,
            onDismiss = { showSearchSheet = false },
            onUserClick = { userId ->
                showSearchSheet = false
                onNavigateToUser(userId)
            }
        )
    }
}


// ═══════════════════════════════════════════
// Post Card
// ═══════════════════════════════════════════

@Composable
private fun PostCard(
    post: CommunityPostItem,
    onPostClick: () -> Unit,
    onLike: () -> Unit,
    onShare: () -> Unit,
    onComment: () -> Unit,
    onReport: () -> Unit,
    onAuthorClick: () -> Unit,
    onAppLinkClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().clickable { onPostClick() }.padding(horizontal = 16.dp, vertical = 12.dp)) {
        // ── Author Header ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp).clickable { onAuthorClick() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (post.authorAvatarUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(post.authorAvatarUrl).crossfade(true).build(),
                        contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            (post.authorDisplayName ?: post.authorUsername).take(1).uppercase(),
                            fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f).clickable { onAuthorClick() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        post.authorDisplayName ?: post.authorUsername,
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    UserTitleBadges(
                        isDeveloper = post.authorIsDeveloper,
                        teamBadges = post.authorTeamBadges
                    )
                }
                Text(
                    "@${post.authorUsername} · ${formatTimeAgo(post.createdAt)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            // More menu
            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.MoreHoriz, null, Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(Strings.communityReport) },
                        onClick = { showMenu = false; onReport() },
                        leadingIcon = { Icon(Icons.Outlined.Flag, null, Modifier.size(18.dp)) }
                    )
                }
            }
        }

        // ── Content ──
        Spacer(Modifier.height(8.dp))
        Text(
            post.content, fontSize = 14.5.sp, lineHeight = 21.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        )

        // ── Tags ──
        if (post.tags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(post.tags) { tag ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = tagColor(tag).copy(alpha = 0.12f)
                    ) {
                        Text(
                            "#$tag", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = tagColor(tag)
                        )
                    }
                }
            }
        }

        // ── Media (images/videos) ──
        if (post.media.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            if (post.media.size == 1) {
                val m = post.media[0]
                val url = m.urlGitee ?: m.urlGithub
                if (url != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(post.media) { m ->
                        val url = m.urlGitee ?: m.urlGithub
                        if (url != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
                                contentDescription = null,
                                modifier = Modifier.size(160.dp).clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        // ── App Links ──
        if (post.appLinks.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            post.appLinks.forEach { appLink ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable {
                        appLink.storeModuleId?.let { onAppLinkClick(it) }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // App icon
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            if (appLink.appIcon != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(appLink.appIcon).crossfade(true).build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        if (appLink.storeModuleType == "app") Icons.Outlined.Apps else Icons.Outlined.Extension,
                                        null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                appLink.appName ?: Strings.communityApplication,
                                fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            appLink.appDescription?.let {
                                Text(
                                    it, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                appLink.storeModuleDownloads?.let {
                                    Text("${it} ↓", fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                }
                                appLink.storeModuleRating?.let {
                                    Text("★ ${String.format("%.1f", it)}", fontSize = 10.sp,
                                        color = Color(0xFFFFB300).copy(alpha = 0.8f))
                                }
                            }
                        }
                        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        // ── Action Bar ──
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Like
            TextButton(onClick = onLike) {
                Icon(
                    if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    null, Modifier.size(18.dp),
                    tint = if (post.isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${post.likeCount}", fontSize = 12.sp,
                    color = if (post.isLiked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            // Comment
            TextButton(onClick = onComment) {
                Icon(Icons.Outlined.ChatBubbleOutline, null, Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Spacer(Modifier.width(4.dp))
                Text("${post.commentCount}", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
            // Share
            TextButton(onClick = onShare) {
                Icon(Icons.Outlined.Share, null, Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Spacer(Modifier.width(4.dp))
                Text("${post.shareCount}", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
            // View count
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.RemoveRedEye, null, Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                Spacer(Modifier.width(3.dp))
                Text("${post.viewCount}", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
            }
        }
    }
}


// ═══════════════════════════════════════════
// Create Post Sheet (P3 #18: Media upload)
// ═══════════════════════════════════════════

data class PendingMedia(
    val uri: android.net.Uri,
    val uploading: Boolean = false,
    val progress: Float = 0f,
    val uploadedUrl: String? = null,
    val error: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePostSheet(
    apiClient: CloudApiClient,
    availableTags: List<String>,
    onDismiss: () -> Unit,
    onPosted: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var content by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isPublishing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Store apps for linking
    var storeApps by remember { mutableStateOf<List<StoreModuleInfo>>(emptyList()) }
    var selectedAppLinks by remember { mutableStateOf<List<PostAppLinkInput>>(emptyList()) }
    var showAppPicker by remember { mutableStateOf(false) }

    // P3 #18: Media state
    var pendingMedia by remember { mutableStateOf<List<PendingMedia>>(emptyList()) }

    // Image picker launcher
    val mediaPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)
    ) { uris ->
        val newMedia = uris.map { PendingMedia(uri = it) }
        pendingMedia = (pendingMedia + newMedia).take(9)
        // Auto-upload each
        newMedia.forEach { media ->
            scope.launch {
                pendingMedia = pendingMedia.map { if (it.uri == media.uri) it.copy(uploading = true) else it }
                try {
                    val tempFile = java.io.File.createTempFile("post_media_", ".jpg", context.cacheDir)
                    context.contentResolver.openInputStream(media.uri)?.use { input ->
                        tempFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    val result = apiClient.uploadAsset(
                        file = tempFile, contentType = "image/jpeg",
                        onProgress = { p ->
                            pendingMedia = pendingMedia.map {
                                if (it.uri == media.uri) it.copy(progress = p) else it
                            }
                        }
                    )
                    when (result) {
                        is AuthResult.Success -> pendingMedia = pendingMedia.map {
                            if (it.uri == media.uri) it.copy(uploading = false, uploadedUrl = result.data, progress = 1f) else it
                        }
                        is AuthResult.Error -> pendingMedia = pendingMedia.map {
                            if (it.uri == media.uri) it.copy(uploading = false, error = true) else it
                        }
                    }
                    tempFile.delete()
                } catch (_: Exception) {
                    pendingMedia = pendingMedia.map {
                        if (it.uri == media.uri) it.copy(uploading = false, error = true) else it
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        when (val result = apiClient.listStoreModules(page = 1, size = 100)) {
            is AuthResult.Success -> storeApps = result.data.first
            is AuthResult.Error -> { /* silent */ }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetMaxWidth = 640.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // Title + Publish
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(Strings.communityCreatePost, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        if (content.isBlank()) return@Button
                        if (selectedTags.isEmpty()) {
                            scope.launch { snackbarHostState.showSnackbar(Strings.communityTagRequired) }
                            return@Button
                        }
                        val mediaInputs = pendingMedia
                            .filter { it.uploadedUrl != null }
                            .map { PostMediaInput(mediaType = "image", urlGithub = it.uploadedUrl) }
                        scope.launch {
                            isPublishing = true
                            when (apiClient.createCommunityPost(
                                content = content, tags = selectedTags.toList(),
                                appLinks = selectedAppLinks, media = mediaInputs
                            )) {
                                is AuthResult.Success -> onPosted()
                                is AuthResult.Error -> snackbarHostState.showSnackbar(Strings.communityPublishFailed)
                            }
                            isPublishing = false
                        }
                    },
                    enabled = content.isNotBlank() && selectedTags.isNotEmpty() && !isPublishing && pendingMedia.none { it.uploading },
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.height(36.dp)
                ) {
                    if (isPublishing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text(Strings.communityPublish, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Content input
            OutlinedTextField(
                value = content, onValueChange = { content = it },
                placeholder = { Text(Strings.communityWhatsNew, fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                shape = RoundedCornerShape(12.dp), maxLines = 8
            )

            // P3 #18: Media preview strip
            if (pendingMedia.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(pendingMedia.size) { index ->
                        val media = pendingMedia[index]
                        Box(modifier = Modifier.size(80.dp)) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(media.uri).crossfade(true).build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            if (media.uploading) {
                                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(progress = { media.progress }, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp, color = Color.White)
                                }
                            }
                            if (media.error) {
                                Box(Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.3f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.ErrorOutline, null, Modifier.size(24.dp), tint = Color.White)
                                }
                            }
                            if (media.uploadedUrl != null && !media.uploading) {
                                Surface(shape = CircleShape, color = Color(0xFF4CAF50), modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp).size(16.dp)) {
                                    Icon(Icons.Filled.Check, null, Modifier.padding(2.dp), tint = Color.White)
                                }
                            }
                            IconButton(onClick = { pendingMedia = pendingMedia.filterIndexed { i, _ -> i != index } },
                                modifier = Modifier.align(Alignment.TopEnd).size(20.dp)) {
                                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.6f), modifier = Modifier.size(16.dp)) {
                                    Icon(Icons.Filled.Close, null, Modifier.padding(2.dp), tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Tags
            val maxTags = 3
            Text(
                "${Strings.communitySelectTags} (${selectedTags.size}/$maxTags)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selectedTags.size >= maxTags)
                    MaterialTheme.colorScheme.tertiary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Column {
                availableTags.chunked(5).forEach { row ->
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(row) { tag ->
                            val isSelected = tag in selectedTags
                            val isDisabled = !isSelected && selectedTags.size >= maxTags
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        selectedTags = selectedTags - tag
                                    } else if (selectedTags.size < maxTags) {
                                        selectedTags = selectedTags + tag
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar(Strings.communityTagMaxLimit) }
                                    }
                                },
                                label = {
                                    Text(tag, fontSize = 11.sp,
                                        color = if (isDisabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        else Color.Unspecified)
                                },
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !isDisabled,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = tagColor(tag).copy(alpha = 0.15f),
                                    selectedLabelColor = tagColor(tag),
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                ),
                                leadingIcon = if (isSelected) { { Icon(Icons.Filled.Check, null, Modifier.size(12.dp)) } } else null
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action buttons: Add media + Link app
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { mediaPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(
                        mediaType = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                    shape = RoundedCornerShape(10.dp), modifier = Modifier.height(36.dp), enabled = pendingMedia.size < 9
                ) {
                    Icon(Icons.Outlined.Image, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(Strings.communityAddMedia, fontSize = 12.sp)
                    if (pendingMedia.isNotEmpty()) {
                        Spacer(Modifier.width(4.dp))
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("${pendingMedia.size}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { showAppPicker = true }, shape = RoundedCornerShape(10.dp), modifier = Modifier.height(36.dp)) {
                    Icon(Icons.Outlined.Link, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(Strings.communityLinkApp, fontSize = 12.sp)
                    if (selectedAppLinks.isNotEmpty()) {
                        Spacer(Modifier.width(4.dp))
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("${selectedAppLinks.size}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Linked apps preview
            if (selectedAppLinks.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                selectedAppLinks.forEach { link ->
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Apps, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(link.appName ?: "App", fontSize = 12.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { selectedAppLinks = selectedAppLinks.filter { it != link } }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Outlined.Close, null, Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            SnackbarHost(snackbarHostState)
        }
    }

    // App picker dialog
    if (showAppPicker) {
        AlertDialog(
            onDismissRequest = { showAppPicker = false },
            title = { Text(Strings.communityLinkApp, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(storeApps) { app ->
                        val isSelected = selectedAppLinks.any { it.storeModuleId == app.id }
                        Row(modifier = Modifier.fillMaxWidth().clickable {
                            selectedAppLinks = if (isSelected) selectedAppLinks.filter { it.storeModuleId != app.id }
                            else selectedAppLinks + PostAppLinkInput(linkType = "store", storeModuleId = app.id, appName = app.name, appIcon = app.icon, appDescription = app.description)
                        }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isSelected, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(app.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(app.description ?: "", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                    if (storeApps.isEmpty()) {
                        item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(Strings.communityNoAppsToLink, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        } }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAppPicker = false }) { Text(Strings.communityConfirm) } }
        )
    }
}

// ═══════════════════════════════════════════
// Helpers
// ═══════════════════════════════════════════

fun tagColor(tag: String): Color = when (tag) {
    "html" -> Color(0xFFE44D26)
    "css" -> Color(0xFF264DE4)
    "javascript", "typescript" -> Color(0xFFF7DF1E)
    "vue" -> Color(0xFF42B883)
    "react" -> Color(0xFF61DAFB)
    "angular" -> Color(0xFFDD0031)
    "svelte" -> Color(0xFFFF3E00)
    "nextjs" -> Color(0xFF888888)
    "nuxtjs" -> Color(0xFF00DC82)
    "nodejs" -> Color(0xFF339933)
    "python" -> Color(0xFF3776AB)
    "php" -> Color(0xFF777BB4)
    "go" -> Color(0xFF00ADD8)
    "webtoapp" -> Color(0xFF6C5CE7)
    "pwa" -> Color(0xFF5A0FC8)
    "responsive" -> Color(0xFF00BCD4)
    "animation" -> Color(0xFFFF5722)
    "game" -> Color(0xFFE91E63)
    "tool" -> Color(0xFF607D8B)
    "education" -> Color(0xFF4CAF50)
    else -> Color(0xFF9E9E9E)
}

fun formatTimeAgo(isoString: String?): String {
    if (isoString == null) return ""
    return try {
        // ── 1. Extract timezone offset from ISO string ──
        // Server may return: "2026-03-24T22:30:15+00:00", "...+08:00", "...Z", or bare
        val tzRegex = Regex("([+-])(\\d{2}):(\\d{2})$")
        val tzMatch = tzRegex.find(isoString)
        val endsWithZ = isoString.endsWith("Z")

        // Calculate the offset in milliseconds
        val offsetMs = when {
            tzMatch != null -> {
                val sign = if (tzMatch.groupValues[1] == "+") 1 else -1
                val hh = tzMatch.groupValues[2].toInt()
                val mm = tzMatch.groupValues[3].toInt()
                sign * (hh * 3600_000L + mm * 60_000L)
            }
            endsWithZ -> 0L
            else -> 0L   // no timezone info → assume UTC (server should always send +00:00)
        }

        // ── 2. Strip fractional seconds AND timezone suffix ──
        val cleaned = isoString
            .replace(Regex("\\.[0-9]+"), "")          // remove .123456
            .replace(Regex("[+-]\\d{2}:\\d{2}$"), "")  // remove +08:00
            .replace(Regex("Z$"), "")                  // remove trailing Z

        // ── 3. Parse as local-of-offset, then convert to epoch millis ──
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
            isLenient = false
        }
        val parsed = format.parse(cleaned) ?: return isoString
        // parsed.time = epoch millis assuming the string is UTC.
        // Subtract the offset to get the actual UTC epoch millis.
        val epochMs = parsed.time - offsetMs

        // ── 4. Relative time ──
        val now = System.currentTimeMillis()
        val diff = now - epochMs
        if (diff < 0) return Strings.timeJustNow // future time safety
        val minutes = diff / 60000
        val hours = minutes / 60
        val days = hours / 24
        when {
            minutes < 1 -> Strings.timeJustNow
            minutes < 60 -> String.format(Strings.timeMinutesAgo, minutes)
            hours < 24 -> String.format(Strings.timeHoursAgo, hours)
            days < 7 -> String.format(Strings.timeDaysAgo, days)
            days < 30 -> String.format(Strings.timeWeeksAgo, days / 7)
            else -> String.format(Strings.timeMonthsAgo, days / 30)
        }
    } catch (_: Exception) { isoString }
}

fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> String.format(Strings.durationHourMinute, hours, minutes)
        minutes > 0 -> String.format(Strings.durationMinute, minutes)
        else -> Strings.durationLessThanMinute
    }
}


// ═══════════════════════════════════════════
// User Search Sheet
// ═══════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUsersSheet(
    communityViewModel: CommunityViewModel,
    onDismiss: () -> Unit,
    onUserClick: (Int) -> Unit
) {
    val searchResults by communityViewModel.searchResults.collectAsState()
    val searchLoading by communityViewModel.searchLoading.collectAsState()
    var query by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)
        ) {
            Text(Strings.communitySearchUsers, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    communityViewModel.searchUsers(it)
                },
                placeholder = { Text(Strings.communitySearchHint, fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(20.dp)) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = ""; communityViewModel.searchUsers("") }) {
                            Icon(Icons.Filled.Close, null, Modifier.size(18.dp))
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            )
            Spacer(Modifier.height(12.dp))

            if (searchLoading) {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            } else if (query.isNotBlank() && searchResults.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.PersonOff, null, Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Spacer(Modifier.height(8.dp))
                        Text(Strings.communityNoUsersFound, fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp)
                ) {
                    items(searchResults, key = { it.id }) { user ->
                        UserListRow(
                            displayName = user.displayName ?: user.username,
                            username = user.username,
                            avatarUrl = user.avatarUrl,
                            isDeveloper = user.isDeveloper,
                            onClick = { onUserClick(user.id) }
                        )
                        if (user != searchResults.last()) {
                            HorizontalDivider(
                                Modifier.padding(start = 56.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════
// Follower / Following List Sheet
// ═══════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowerListSheet(
    title: String,
    users: List<com.webtoapp.core.cloud.CommunityUserProfile>,
    emptyText: String,
    onDismiss: () -> Unit,
    onUserClick: (Int) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))

            if (users.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.People, null, Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Spacer(Modifier.height(8.dp))
                        Text(emptyText, fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(users, key = { it.id }) { user ->
                        UserListRow(
                            displayName = user.displayName ?: user.username,
                            username = user.username,
                            avatarUrl = user.avatarUrl,
                            isDeveloper = user.isDeveloper,
                            subtitle = if (user.followerCount > 0) "${user.followerCount} ${Strings.communityFollowersList}" else null,
                            onClick = { onUserClick(user.id) }
                        )
                        if (user != users.last()) {
                            HorizontalDivider(
                                Modifier.padding(start = 56.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }
        }
    }
}


// ═══════════════════════════════════════════
// Reusable User Row (for search / follower list)
// ═══════════════════════════════════════════

@Composable
private fun UserListRow(
    displayName: String,
    username: String,
    avatarUrl: String?,
    isDeveloper: Boolean,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        displayName.take(1).uppercase(),
                        fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(displayName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (isDeveloper) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.Verified, null, Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text("@$username", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            subtitle?.let {
                Text(it, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            }
        }
        Icon(Icons.Outlined.ChevronRight, null, Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
    }
}
