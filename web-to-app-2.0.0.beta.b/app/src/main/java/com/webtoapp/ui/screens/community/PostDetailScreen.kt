package com.webtoapp.ui.screens.community

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.CommunityPostItem
import com.webtoapp.core.cloud.PostCommentItem
import com.webtoapp.core.i18n.Strings
import com.webtoapp.ui.components.ThemedBackgroundBox
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * 帖子详情页 — Premium Twitter/X Style
 * 完整功能：帖子内容、媒体展示、应用链接、交互栏（物理弹簧）、评论列表、回复评论
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PostDetailScreen(
    postId: Int,
    communityViewModel: com.webtoapp.ui.viewmodel.CommunityViewModel,
    onBack: () -> Unit,
    onNavigateToUser: (Int) -> Unit
) {
    val apiClient: CloudApiClient = koinInject()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var post by remember { mutableStateOf<CommunityPostItem?>(null) }
    var comments by remember { mutableStateOf<List<PostCommentItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var commentText by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var replyingTo by remember { mutableStateOf<PostCommentItem?>(null) }
    var showMoreSheet by remember { mutableStateOf(false) }

    // Load post detail + comments
    LaunchedEffect(postId) {
        loading = true
        val postResult = apiClient.getCommunityPost(postId)
        if (postResult is AuthResult.Success) {
            post = postResult.data
        }
        val commentsResult = apiClient.listPostComments(postId)
        if (commentsResult is AuthResult.Success) {
            comments = commentsResult.data
        }
        loading = false
    }

    // Refresh helper
    fun refreshComments() {
        scope.launch {
            val refreshed = apiClient.listPostComments(postId)
            if (refreshed is AuthResult.Success) comments = refreshed.data
            // Also refresh the post to get latest counts
            val refreshedPost = apiClient.getCommunityPost(postId)
            if (refreshedPost is AuthResult.Success) post = refreshedPost.data
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(Strings.communityPost, fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(22.dp))
                    }
                },
                actions = {
                    IconButton(onClick = { showMoreSheet = true }) {
                        Icon(Icons.Outlined.MoreHoriz, null, Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            )
        },
        bottomBar = {
            FrostedBottomBar {
                Column {
                    // Reply indicator
                    AnimatedVisibility(
                        visible = replyingTo != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        replyingTo?.let { reply ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.SubdirectoryArrowRight, null, Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        buildAnnotatedString {
                                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                                                append("@${reply.authorDisplayName ?: reply.authorUsername}")
                                            }
                                            append(" ")
                                            append(reply.content.take(40))
                                        },
                                        fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { replyingTo = null }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Filled.Close, null, Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            placeholder = {
                                Text(
                                    if (replyingTo != null) "${Strings.communityComment}..." else Strings.communityPostYourReply,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                            },
                            modifier = Modifier.weight(1f).heightIn(max = 96.dp),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        val sendEnabled = commentText.isNotBlank() && !submitting
                        val sendAlpha by animateFloatAsState(
                            if (sendEnabled) 1f else 0.35f, tween(200), label = "sendA"
                        )
                        FilledIconButton(
                            onClick = {
                                if (sendEnabled) {
                                    submitting = true
                                    scope.launch {
                                        val parentId = replyingTo?.id
                                        val result = apiClient.addPostComment(postId, commentText.trim(), parentId)
                                        if (result is AuthResult.Success) {
                                            commentText = ""
                                            replyingTo = null
                                            refreshComments()
                                        } else if (result is AuthResult.Error) {
                                            snackbarHostState.showSnackbar(result.message)
                                        }
                                        submitting = false
                                    }
                                }
                            },
                            enabled = sendEnabled,
                            modifier = Modifier.size(40.dp).graphicsLayer { alpha = sendAlpha },
                            shape = CircleShape
                        ) {
                            if (submitting) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        ThemedBackgroundBox(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            if (loading) {
                DetailShimmer()
            } else {
                post?.let { p ->
                    LazyColumn {
                        // ═══ Post Body ═══
                        item {
                            StaggeredItem(index = 0) {
                                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                    // ── Author header ──
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { onNavigateToUser(p.authorId) }
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(46.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            if (p.authorAvatarUrl != null) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(p.authorAvatarUrl).crossfade(true).build(),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    Text(
                                                        (p.authorDisplayName ?: p.authorUsername).take(1).uppercase(),
                                                        fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(p.authorDisplayName ?: p.authorUsername,
                                                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                if (p.authorIsDeveloper) {
                                                    Spacer(Modifier.width(4.dp))
                                                    Icon(Icons.Filled.Verified, null, Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                            Text(
                                                "@${p.authorUsername} · ${formatTimeAgo(p.createdAt)}",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                            )
                                        }
                                    }

                                    // ── Content ──
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        p.content, fontSize = 16.sp, lineHeight = 25.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.93f)
                                    )

                                    // ── Tags ──
                                    if (p.tags.isNotEmpty()) {
                                        Spacer(Modifier.height(14.dp))
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            p.tags.forEach { tag ->
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = tagColor(tag).copy(alpha = 0.12f)
                                                ) {
                                                    Text("#$tag",
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                                        color = tagColor(tag))
                                                }
                                            }
                                        }
                                    }

                                    // ── Media ──
                                    if (p.media.isNotEmpty()) {
                                        Spacer(Modifier.height(14.dp))
                                        if (p.media.size == 1) {
                                            val m = p.media[0]
                                            val url = m.urlGitee ?: m.urlGithub
                                            if (url != null) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(url).crossfade(true).build(),
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxWidth()
                                                        .heightIn(max = 320.dp)
                                                        .clip(RoundedCornerShape(14.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        } else {
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                items(p.media) { m ->
                                                    val url = m.urlGitee ?: m.urlGithub
                                                    if (url != null) {
                                                        AsyncImage(
                                                            model = ImageRequest.Builder(LocalContext.current)
                                                                .data(url).crossfade(true).build(),
                                                            contentDescription = null,
                                                            modifier = Modifier.size(180.dp)
                                                                .clip(RoundedCornerShape(12.dp)),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // ── App Links ──
                                    if (p.appLinks.isNotEmpty()) {
                                        Spacer(Modifier.height(14.dp))
                                        p.appLinks.forEach { appLink ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                                                border = BorderStroke(0.5.dp,
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            ) {
                                                Row(
                                                    Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        modifier = Modifier.size(44.dp)
                                                    ) {
                                                        if (appLink.appIcon != null) {
                                                            AsyncImage(
                                                                model = ImageRequest.Builder(LocalContext.current)
                                                                    .data(appLink.appIcon).crossfade(true).build(),
                                                                contentDescription = null,
                                                                modifier = Modifier.fillMaxSize()
                                                                    .clip(RoundedCornerShape(10.dp)),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else {
                                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    if (appLink.storeModuleType == "app") Icons.Outlined.Apps
                                                                    else Icons.Outlined.Extension,
                                                                    null, Modifier.size(22.dp),
                                                                    tint = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(Modifier.width(10.dp))
                                                    Column(Modifier.weight(1f)) {
                                                        Text(
                                                            appLink.appName ?: Strings.communityApplication,
                                                            fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                                            maxLines = 1, overflow = TextOverflow.Ellipsis
                                                        )
                                                        appLink.appDescription?.let {
                                                            Text(it, fontSize = 11.sp, maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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

                                    // ── Timestamp (full) ──
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        formatTimeAgo(p.createdAt),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                            GlassDivider()
                        }

                        // ═══ Stats Row ═══
                        item {
                            StaggeredItem(index = 1) {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    StatPill("${p.likeCount}", Strings.communityLike)
                                    StatPill("${p.commentCount}", Strings.communityComment)
                                    StatPill("${p.shareCount}", Strings.communityShare)
                                    StatPill("${p.viewCount}", Strings.favorite)
                                }
                            }
                            GlassDivider()
                        }

                        // ═══ Action Bar (physics-based) ═══
                        item {
                            StaggeredItem(index = 2) {
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    DetailActionButton(
                                        icon = Icons.Outlined.FavoriteBorder,
                                        activeIcon = Icons.Filled.Favorite,
                                        label = Strings.communityLike,
                                        isActive = p.isLiked,
                                        activeColor = Color(0xFFE91E63),
                                        onClick = {
                                            scope.launch {
                                                val result = apiClient.togglePostLike(postId)
                                                if (result is AuthResult.Success) {
                                                    post = p.copy(
                                                        isLiked = result.data.liked,
                                                        likeCount = result.data.likeCount
                                                    )
                                                } else if (result is AuthResult.Error) {
                                                    snackbarHostState.showSnackbar(result.message)
                                                }
                                            }
                                        }
                                    )
                                    DetailActionButton(
                                        icon = Icons.Outlined.ChatBubbleOutline,
                                        activeIcon = Icons.Outlined.ChatBubbleOutline,
                                        label = Strings.communityComment,
                                        isActive = false,
                                        activeColor = MaterialTheme.colorScheme.primary,
                                        onClick = { /* Focus comment input */ }
                                    )
                                    DetailActionButton(
                                        icon = Icons.Outlined.Repeat,
                                        activeIcon = Icons.Filled.Repeat,
                                        label = Strings.communityShare,
                                        isActive = false,
                                        activeColor = Color(0xFF4CAF50),
                                        onClick = {
                                            scope.launch {
                                                val result = apiClient.sharePost(postId)
                                                if (result is AuthResult.Success) {
                                                    post = p.copy(shareCount = p.shareCount + 1)
                                                } else if (result is AuthResult.Error) {
                                                    snackbarHostState.showSnackbar(result.message)
                                                }
                                            }
                                        }
                                    )
                                    DetailActionButton(
                                        icon = Icons.Outlined.BookmarkBorder,
                                        activeIcon = Icons.Filled.Bookmark,
                                        label = Strings.favorite,
                                        isActive = false,
                                        activeColor = MaterialTheme.colorScheme.primary,
                                        onClick = {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    Strings.communityBookmarks
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            GlassDivider()
                        }

                        // ═══ Comments Section ═══
                        item {
                            StaggeredItem(index = 3) {
                                Text(
                                    "${Strings.communityComment} (${comments.size})",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    fontWeight = FontWeight.Bold, fontSize = 15.sp
                                )
                            }
                        }

                        if (comments.isEmpty()) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Outlined.Forum, null,
                                            Modifier.size(40.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Text(Strings.communityNoRepliesYet,
                                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(Modifier.height(2.dp))
                                        Text(Strings.communityBeFirstReply, fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
                                    }
                                }
                            }
                        }

                        itemsIndexed(comments, key = { _, c -> c.id }) { index, comment ->
                            StaggeredItem(index = index + 4) {
                                PostCommentRow(
                                    comment = comment,
                                    onUserClick = { onNavigateToUser(comment.authorId) },
                                    onReply = { replyingTo = comment },
                                    onLike = {
                                        // Comment like — future feature
                                    }
                                )
                            }
                            GlassDivider(Modifier.padding(start = 62.dp))
                        }

                        item { Spacer(Modifier.height(8.dp)) }
                    }
                } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.SearchOff, null, Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Spacer(Modifier.height(8.dp))
                        Text(Strings.communityPostNotFound, fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    // ═══ More Sheet ═══
    if (showMoreSheet) {
        ModalBottomSheet(onDismissRequest = { showMoreSheet = false }) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                // Report
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .clickable {
                            showMoreSheet = false
                            scope.launch {
                                val result = apiClient.reportPost(postId, "inappropriate")
                                if (result is AuthResult.Success) {
                                    snackbarHostState.showSnackbar(Strings.communityReport)
                                } else if (result is AuthResult.Error) {
                                    snackbarHostState.showSnackbar(result.message)
                                }
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Flag, null, Modifier.size(22.dp),
                            tint = Color(0xFFE57373))
                        Spacer(Modifier.width(14.dp))
                        Text(Strings.communityReportTitle, fontSize = 15.sp,
                            fontWeight = FontWeight.Medium, color = Color(0xFFE57373))
                    }
                }

                // Share link
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .clickable {
                            showMoreSheet = false
                            scope.launch {
                                snackbarHostState.showSnackbar(Strings.communityShare)
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Share, null, Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Spacer(Modifier.width(14.dp))
                        Text(Strings.communityShare, fontSize = 15.sp,
                            fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

// ═══════════════════════════════════════════
// Stats Pill — "42 Likes" inline display
// ═══════════════════════════════════════════

@Composable
private fun StatPill(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(count, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (label.isNotEmpty()) {
                Spacer(Modifier.width(3.dp))
                Text(label, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

// ═══════════════════════════════════════════
// Physics-based action button
// ═══════════════════════════════════════════

@Composable
private fun DetailActionButton(
    icon: ImageVector, activeIcon: ImageVector,
    label: String = "",
    isActive: Boolean, activeColor: Color,
    onClick: () -> Unit
) {
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val currentColor by animateColorAsState(
        if (isActive) activeColor else inactiveColor,
        tween(280), label = "actionClr"
    )

    var bouncing by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (bouncing) 1.3f else 1f,
        spring(dampingRatio = 0.35f, stiffness = 600f),
        label = "actionScale",
        finishedListener = { bouncing = false }
    )

    var showBurst by remember { mutableStateOf(false) }
    var burstKey by remember { mutableIntStateOf(0) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            bouncing = true
            if (!isActive) { showBurst = true; burstKey++ }
            onClick()
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            LikeBurstEffect(
                trigger = showBurst,
                color = activeColor,
                modifier = Modifier.size(36.dp)
            )
            Icon(
                if (isActive) activeIcon else icon, null,
                Modifier.size(22.dp).scale(scale), tint = currentColor
            )
        }
        if (label.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                label, fontSize = 11.sp,
                color = currentColor,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }

    LaunchedEffect(burstKey) {
        if (showBurst) {
            kotlinx.coroutines.delay(500)
            showBurst = false
        }
    }
}

// ═══════════════════════════════════════════
// Comment Row — with reply button
// ═══════════════════════════════════════════

@Composable
private fun PostCommentRow(
    comment: PostCommentItem,
    onUserClick: () -> Unit,
    onReply: () -> Unit,
    onLike: () -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row {
            Surface(
                modifier = Modifier.size(36.dp).clickable(onClick = onUserClick),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                if (comment.authorAvatarUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(comment.authorAvatarUrl).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            (comment.authorDisplayName ?: comment.authorUsername).take(1).uppercase(),
                            fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        comment.authorDisplayName ?: comment.authorUsername,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier.clickable(onClick = onUserClick)
                    )
                    if (comment.authorIsDeveloper) {
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Filled.Verified, null, Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    comment.createdAt?.let {
                        Text("  ·  ", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                        Text(formatTimeAgo(it), fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    comment.content, fontSize = 15.sp, lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )

                // Action row
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reply button
                    Row(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onReply
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.SubdirectoryArrowRight, null, Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(Modifier.width(3.dp))
                        Text(Strings.communityComment, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                    // Like count
                    if (comment.likeCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.FavoriteBorder, null, Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            Spacer(Modifier.width(3.dp))
                            Text("${comment.likeCount}", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        }
                    }
                }

                // Nested replies
                if (comment.replies.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        comment.replies.take(3).forEach { reply ->
                            Row(Modifier.padding(vertical = 4.dp)) {
                                Surface(
                                    modifier = Modifier.size(24.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            (reply.authorDisplayName ?: reply.authorUsername).take(1).uppercase(),
                                            fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(Modifier.width(6.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            reply.authorDisplayName ?: reply.authorUsername,
                                            fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        reply.createdAt?.let {
                                            Text(" · ${formatTimeAgo(it)}", fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                        }
                                    }
                                    Text(
                                        reply.content, fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 3, overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (comment.replies.size > 3) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                Strings.communityShowMoreReplies,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 30.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════
// Shimmer skeleton
// ═══════════════════════════════════════════

@Composable
private fun DetailShimmer() {
    Column(Modifier.padding(16.dp)) {
        // Author
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBlock(46.dp, 46.dp, CircleShape)
            Spacer(Modifier.width(10.dp))
            Column {
                ShimmerBlock(140.dp, 15.dp)
                Spacer(Modifier.height(6.dp))
                ShimmerBlock(100.dp, 12.dp)
            }
        }
        Spacer(Modifier.height(20.dp))
        // Content
        ShimmerBlock(300.dp, 16.dp)
        Spacer(Modifier.height(8.dp))
        ShimmerBlock(280.dp, 16.dp)
        Spacer(Modifier.height(8.dp))
        ShimmerBlock(200.dp, 16.dp)
        Spacer(Modifier.height(16.dp))
        // Tags
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { ShimmerBlock(60.dp, 22.dp, RoundedCornerShape(6.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        // Stats
        GlassDivider()
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { ShimmerBlock(50.dp, 14.dp) }
        }
        Spacer(Modifier.height(10.dp))
        GlassDivider()
        Spacer(Modifier.height(12.dp))
        // Action bar
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            repeat(4) { ShimmerBlock(28.dp, 22.dp) }
        }
        Spacer(Modifier.height(12.dp))
        GlassDivider()
        // Comments
        repeat(3) {
            Spacer(Modifier.height(14.dp))
            Row {
                ShimmerBlock(36.dp, 36.dp, CircleShape)
                Spacer(Modifier.width(10.dp))
                Column {
                    ShimmerBlock(120.dp, 13.dp)
                    Spacer(Modifier.height(6.dp))
                    ShimmerBlock(260.dp, 14.dp)
                    Spacer(Modifier.height(4.dp))
                    ShimmerBlock(180.dp, 14.dp)
                }
            }
        }
    }
}
