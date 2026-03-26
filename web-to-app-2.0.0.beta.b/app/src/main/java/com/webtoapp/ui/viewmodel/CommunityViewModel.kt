package com.webtoapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.cloud.*
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 社区 ViewModel — 正确处理 AuthResult 包装类型
 */
class CommunityViewModel(
    private val cloudApiClient: CloudApiClient
) : ViewModel() {

    companion object {
        private const val TAG = "CommunityViewModel"
    }

    // ─── 模块详情 ───
    private val _moduleDetail = MutableStateFlow<CommunityModuleDetail?>(null)
    val moduleDetail: StateFlow<CommunityModuleDetail?> = _moduleDetail.asStateFlow()

    private val _moduleDetailLoading = MutableStateFlow(false)
    val moduleDetailLoading: StateFlow<Boolean> = _moduleDetailLoading.asStateFlow()

    // ─── 评论 ───
    private val _comments = MutableStateFlow<List<ModuleComment>>(emptyList())
    val comments: StateFlow<List<ModuleComment>> = _comments.asStateFlow()

    private val _commentsLoading = MutableStateFlow(false)
    val commentsLoading: StateFlow<Boolean> = _commentsLoading.asStateFlow()

    // ─── 收藏列表 ───
    private val _favorites = MutableStateFlow<List<CommunityModuleDetail>>(emptyList())
    val favorites: StateFlow<List<CommunityModuleDetail>> = _favorites.asStateFlow()

    private val _favoritesLoading = MutableStateFlow(false)
    val favoritesLoading: StateFlow<Boolean> = _favoritesLoading.asStateFlow()

    // ─── 用户主页 ───
    private val _userProfile = MutableStateFlow<CommunityUserProfile?>(null)
    val userProfile: StateFlow<CommunityUserProfile?> = _userProfile.asStateFlow()

    private val _userModules = MutableStateFlow<List<CommunityModuleDetail>>(emptyList())
    val userModules: StateFlow<List<CommunityModuleDetail>> = _userModules.asStateFlow()

    private val _userProfileLoading = MutableStateFlow(false)
    val userProfileLoading: StateFlow<Boolean> = _userProfileLoading.asStateFlow()

    // ─── 用户帖子 ───
    private val _userPosts = MutableStateFlow<List<CommunityPostItem>>(emptyList())
    val userPosts: StateFlow<List<CommunityPostItem>> = _userPosts.asStateFlow()

    // ─── 用户在线活动 ───
    private val _userActivity = MutableStateFlow<UserActivityInfo?>(null)
    val userActivity: StateFlow<UserActivityInfo?> = _userActivity.asStateFlow()

    // ─── 通知 ───
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _notificationsLoading = MutableStateFlow(false)
    val notificationsLoading: StateFlow<Boolean> = _notificationsLoading.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // ─── 动态 Feed（占位，服务端尚未实现） ───
    private val _activityFeed = MutableStateFlow<List<FeedItem>>(emptyList())
    val activityFeed: StateFlow<List<FeedItem>> = _activityFeed.asStateFlow()

    private val _activityFeedLoading = MutableStateFlow(false)
    val activityFeedLoading: StateFlow<Boolean> = _activityFeedLoading.asStateFlow()

    // ─── 通用消息 ───
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() { _message.value = null }

    // ─── 帖子 Feed ───
    private val _posts = MutableStateFlow<List<CommunityPostItem>>(emptyList())
    val posts: StateFlow<List<CommunityPostItem>> = _posts.asStateFlow()

    private val _feedLoading = MutableStateFlow(true)
    val feedLoading: StateFlow<Boolean> = _feedLoading.asStateFlow()

    private val _feedLoadingMore = MutableStateFlow(false)
    val feedLoadingMore: StateFlow<Boolean> = _feedLoadingMore.asStateFlow()

    private val _feedRefreshing = MutableStateFlow(false)
    val feedRefreshing: StateFlow<Boolean> = _feedRefreshing.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    private var currentPage = 1
    private var hasMore = true

    fun setSelectedTag(tag: String?) {
        _selectedTag.value = tag
        loadPosts(page = 1)
    }

    fun loadPosts(page: Int = 1, append: Boolean = false) {
        viewModelScope.launch {
            if (page == 1 && !append) _feedLoading.value = true else _feedLoadingMore.value = true
            try {
                val result = cloudApiClient.listCommunityPosts(tag = _selectedTag.value, page = page)
                handleResult(result, { response ->
                    val newPosts = response.posts
                    _posts.value = if (append) _posts.value + newPosts else newPosts
                    currentPage = page
                    hasMore = newPosts.size >= 20
                })
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load posts", e)
            } finally {
                _feedLoading.value = false
                _feedLoadingMore.value = false
                _feedRefreshing.value = false
            }
        }
    }

    fun loadMorePosts() {
        if (_feedLoadingMore.value || !hasMore || _posts.value.isEmpty()) return
        loadPosts(page = currentPage + 1, append = true)
    }

    fun refreshPosts() {
        _feedRefreshing.value = true
        loadPosts(page = 1)
    }

    fun togglePostLike(postId: Int) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.togglePostLike(postId)
                when (result) {
                    is AuthResult.Success -> {
                        _posts.value = _posts.value.map { p ->
                            if (p.id == postId) p.copy(
                                isLiked = result.data.liked,
                                likeCount = result.data.likeCount
                            ) else p
                        }
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Like failed: ${result.message}")
                        _message.value = result.message
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Like exception", e)
                _message.value = e.message ?: "操作失败"
            }
        }
    }

    fun sharePost(postId: Int) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.sharePost(postId)
                when (result) {
                    is AuthResult.Success -> {
                        _posts.value = _posts.value.map { p ->
                            if (p.id == postId) p.copy(shareCount = p.shareCount + 1) else p
                        }
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Share failed: ${result.message}")
                        _message.value = result.message
                    }
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "转发失败"
            }
        }
    }

    fun reportPost(postId: Int, reason: String = "inappropriate") {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.reportPost(postId, reason)
                when (result) {
                    is AuthResult.Success -> _message.value = "举报已提交"
                    is AuthResult.Error -> _message.value = result.message
                }
            } catch (e: Exception) {
                _message.value = e.message ?: "举报失败"
            }
        }
    }

    // ═══ 辅助：解包 AuthResult ═══
    private inline fun <T> handleResult(
        result: AuthResult<T>,
        onSuccess: (T) -> Unit,
        errorMessage: String? = null
    ) {
        when (result) {
            is AuthResult.Success -> onSuccess(result.data)
            is AuthResult.Error -> {
                AppLogger.e(TAG, "API error: ${result.message}")
                // 优先显示服务端返回的真实错误信息
                _message.value = result.message ?: errorMessage ?: "操作失败"
            }
        }
    }

    // ═══ 模块详情 ═══

    fun loadModuleDetail(moduleId: Int) {
        viewModelScope.launch {
            _moduleDetailLoading.value = true
            try {
                // 直接查询单个模块（高效），失败时回退到搜索方式
                val result = cloudApiClient.getStoreModuleById(moduleId)
                when (result) {
                    is AuthResult.Success -> {
                        _moduleDetail.value = result.data.toCommunityDetail()
                    }
                    is AuthResult.Error -> {
                        // 回退：服务端可能未实现 GET /modules/{id}，用搜索兜底
                        val fallback = cloudApiClient.listStoreModules(page = 1, size = 1, search = moduleId.toString())
                        handleResult(fallback, { (modules, _) ->
                            val match = modules.firstOrNull { it.id == moduleId }
                            _moduleDetail.value = match?.toCommunityDetail()
                        }, "加载模块详情失败")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load module detail", e)
                _message.value = "加载模块详情失败"
            } finally {
                _moduleDetailLoading.value = false
            }
        }
    }

    fun voteModule(moduleId: Int, voteType: String) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.voteModule(moduleId, voteType)
                handleResult(result, { loadModuleDetail(moduleId) }, "投票失败")
            } catch (e: Exception) {
                _message.value = "投票失败"
            }
        }
    }

    fun toggleFavorite(moduleId: Int) {
        viewModelScope.launch {
            try {
                val current = _moduleDetail.value
                val result = if (current?.isFavorited == true) {
                    cloudApiClient.removeFavorite(moduleId)
                } else {
                    cloudApiClient.addFavorite(moduleId)
                }
                handleResult(result, {
                    _message.value = if (current?.isFavorited == true) "已取消收藏" else "已收藏"
                    loadModuleDetail(moduleId)
                }, "操作失败")
            } catch (e: Exception) {
                _message.value = "操作失败"
            }
        }
    }

    fun reportModule(moduleId: Int, reason: String, details: String?) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.reportModule(moduleId, reason, details)
                handleResult(result, { _message.value = "举报已提交" }, "举报失败")
            } catch (e: Exception) {
                _message.value = "举报失败"
            }
        }
    }

    // ═══ 评论 ═══

    fun loadComments(moduleId: Int) {
        viewModelScope.launch {
            _commentsLoading.value = true
            try {
                val result = cloudApiClient.listComments(moduleId)
                handleResult(result, { _comments.value = it })
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load comments", e)
            } finally {
                _commentsLoading.value = false
            }
        }
    }

    fun addComment(moduleId: Int, content: String, parentId: Int? = null) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.addComment(moduleId, content, parentId)
                handleResult(result, {
                    _message.value = "评论已发布"
                    loadComments(moduleId)
                }, "评论失败")
            } catch (e: Exception) {
                _message.value = "评论失败"
            }
        }
    }

    // ═══ 收藏列表 ═══

    fun loadFavorites() {
        viewModelScope.launch {
            _favoritesLoading.value = true
            try {
                val result = cloudApiClient.listFavorites()
                handleResult(result, { (modules, _) ->
                    _favorites.value = modules.map { it.toCommunityDetail() }
                })
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load favorites", e)
            } finally {
                _favoritesLoading.value = false
            }
        }
    }

    // ─── 用户团队作品 ───
    private val _userTeamWorks = MutableStateFlow<List<TeamWorkItem>>(emptyList())
    val userTeamWorks: StateFlow<List<TeamWorkItem>> = _userTeamWorks.asStateFlow()

    // ═══ 用户主页 ═══

    fun loadUserProfile(userId: Int) {
        viewModelScope.launch {
            _userProfileLoading.value = true
            try {
                val result = cloudApiClient.getUserProfile(userId)
                handleResult(result, { profile ->
                    _userProfile.value = profile
                    // 加载用户模块 — 使用精确接口
                    loadUserModulesInternal(profile.id)
                    // 加载用户团队作品
                    loadUserTeamWorks(userId)
                    // 加载用户帖子
                    loadUserPosts(userId)
                    // 加载用户在线活动
                    loadUserActivity(userId)
                }, "加载用户信息失败")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load user profile", e)
                _message.value = "加载用户信息失败"
            } finally {
                _userProfileLoading.value = false
            }
        }
    }

    private fun loadUserModulesInternal(userId: Int) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.getUserModules(userId)
                handleResult(result, { (modules, _) ->
                    _userModules.value = modules.map { it.toCommunityDetail() }
                })
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load user modules", e)
            }
        }
    }

    fun loadUserTeamWorks(userId: Int) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.getUserTeamWorks(userId)
                if (result is AuthResult.Success) {
                    _userTeamWorks.value = result.data.works
                } else if (result is AuthResult.Error) {
                    AppLogger.e(TAG, "Failed to load team works: ${result.message}")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load user team works", e)
            }
        }
    }

    fun loadUserPosts(userId: Int) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.getUserPosts(userId)
                if (result is AuthResult.Success) {
                    _userPosts.value = result.data.posts
                } else if (result is AuthResult.Error) {
                    AppLogger.e(TAG, "Failed to load user posts: ${result.message}")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load user posts", e)
            }
        }
    }

    fun loadUserActivity(userId: Int) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.getUserActivity(userId)
                if (result is AuthResult.Success) {
                    _userActivity.value = result.data
                } else if (result is AuthResult.Error) {
                    AppLogger.e(TAG, "Failed to load user activity: ${result.message}")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load user activity", e)
            }
        }
    }

    fun toggleFollow(userId: Int) {
        viewModelScope.launch {
            try {
                val current = _userProfile.value
                val result = if (current?.isFollowing == true) {
                    cloudApiClient.unfollowUser(userId)
                } else {
                    cloudApiClient.followUser(userId)
                }
                handleResult(result, {
                    _message.value = if (current?.isFollowing == true) "已取消关注" else "已关注"
                    loadUserProfile(userId)
                }, "操作失败")
            } catch (e: Exception) {
                _message.value = "操作失败"
            }
        }
    }

    // ═══ 通知 ═══

    fun loadNotifications() {
        viewModelScope.launch {
            _notificationsLoading.value = true
            try {
                val result = cloudApiClient.listNotifications()
                handleResult(result, { (notifications, _) ->
                    _notifications.value = notifications
                })
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load notifications", e)
            } finally {
                _notificationsLoading.value = false
            }
        }
    }

    fun loadUnreadCount() {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.getUnreadNotificationCount()
                handleResult(result, { _unreadCount.value = it })
            } catch (e: Exception) { /* silent */ }
        }
    }

    fun markNotificationRead(notificationId: Int) {
        viewModelScope.launch {
            try {
                cloudApiClient.markNotificationRead(notificationId)
                loadNotifications()
                loadUnreadCount()
            } catch (e: Exception) { /* silent */ }
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            try {
                cloudApiClient.markAllNotificationsRead()
                loadNotifications()
                _unreadCount.value = 0
                _message.value = "已全部标为已读"
            } catch (e: Exception) {
                _message.value = "操作失败"
            }
        }
    }

    // ═══ 动态 Feed ═══

    fun loadFeed() {
        viewModelScope.launch {
            _activityFeedLoading.value = true
            try {
                // 使用帖子列表接口而非旧的模块 Feed 接口
                val result = cloudApiClient.listCommunityPosts(page = 1, size = 20)
                handleResult(result, { response ->
                    _activityFeed.value = emptyList() // Feed items are separate
                })
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load feed", e)
            } finally {
                _activityFeedLoading.value = false
            }
        }
    }

    // ─── 关注动态 Feed ───

    private val _followingPosts = MutableStateFlow<List<CommunityPostItem>>(emptyList())
    val followingPosts: StateFlow<List<CommunityPostItem>> = _followingPosts.asStateFlow()

    private val _followingLoading = MutableStateFlow(false)
    val followingLoading: StateFlow<Boolean> = _followingLoading.asStateFlow()

    fun loadFollowingFeed(page: Int = 1) {
        viewModelScope.launch {
            _followingLoading.value = true
            try {
                val result = cloudApiClient.getFollowingFeed(page)
                handleResult(result, { posts ->
                    _followingPosts.value = posts
                })
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load following feed", e)
            } finally {
                _followingLoading.value = false
            }
        }
    }

    // ─── 趋势 Feed ───

    private val _trendingPosts = MutableStateFlow<List<CommunityPostItem>>(emptyList())
    val trendingPosts: StateFlow<List<CommunityPostItem>> = _trendingPosts.asStateFlow()

    private val _trendingLoading = MutableStateFlow(false)
    val trendingLoading: StateFlow<Boolean> = _trendingLoading.asStateFlow()

    fun loadTrendingFeed(page: Int = 1) {
        viewModelScope.launch {
            _trendingLoading.value = true
            try {
                val result = cloudApiClient.getTrendingFeed(page)
                handleResult(result, { posts ->
                    _trendingPosts.value = posts
                })
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load trending feed", e)
            } finally {
                _trendingLoading.value = false
            }
        }
    }

    // ─── 粉丝/关注列表 ───

    private val _followersList = MutableStateFlow<List<CommunityUserProfile>>(emptyList())
    val followersList: StateFlow<List<CommunityUserProfile>> = _followersList.asStateFlow()

    private val _followingList = MutableStateFlow<List<CommunityUserProfile>>(emptyList())
    val followingList: StateFlow<List<CommunityUserProfile>> = _followingList.asStateFlow()

    fun loadUserFollowers(userId: Int) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.getUserFollowers(userId)
                handleResult(result, { _followersList.value = it })
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load followers", e)
            }
        }
    }

    fun loadUserFollowing(userId: Int) {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.getUserFollowing(userId)
                handleResult(result, { _followingList.value = it })
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load following", e)
            }
        }
    }

    // ─── 用户搜索 ───

    private val _searchResults = MutableStateFlow<List<CommunityUserProfile>>(emptyList())
    val searchResults: StateFlow<List<CommunityUserProfile>> = _searchResults.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading: StateFlow<Boolean> = _searchLoading.asStateFlow()

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        _searchLoading.value = true
        viewModelScope.launch {
            try {
                val result = cloudApiClient.searchUsers(query)
                handleResult(result, { _searchResults.value = it })
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to search users", e)
            } finally {
                _searchLoading.value = false
            }
        }
    }

    // ─── 热门/精选模块 ───

    private val _trendingModules = MutableStateFlow<List<StoreModuleInfo>>(emptyList())
    val trendingModules: StateFlow<List<StoreModuleInfo>> = _trendingModules.asStateFlow()

    private val _featuredModules = MutableStateFlow<List<StoreModuleInfo>>(emptyList())
    val featuredModules: StateFlow<List<StoreModuleInfo>> = _featuredModules.asStateFlow()

    fun loadTrendingModules() {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.getTrendingModules()
                handleResult(result, { _trendingModules.value = it })
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load trending modules", e)
            }
        }
    }

    fun loadFeaturedModules() {
        viewModelScope.launch {
            try {
                val result = cloudApiClient.getFeaturedModules()
                handleResult(result, { _featuredModules.value = it })
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load featured modules", e)
            }
        }
    }
}

// ═══ StoreModuleInfo → CommunityModuleDetail 转换 ═══
private fun StoreModuleInfo.toCommunityDetail() = CommunityModuleDetail(
    id = this.id,
    name = this.name,
    description = this.description,
    icon = this.icon,
    category = this.category,
    tags = this.tags,
    versionName = this.versionName,
    downloads = this.downloads,
    rating = this.rating,
    ratingCount = this.ratingCount,
    isFeatured = this.isFeatured,
    authorName = this.authorName,
    authorId = 0, // StoreModuleInfo 暂无 authorId
    shareCode = this.shareCode,
    userVote = null,
    isFavorited = false,
    createdAt = this.createdAt,
    updatedAt = null
)
