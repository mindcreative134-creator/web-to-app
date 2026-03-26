package com.webtoapp.core.bgm

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.webtoapp.util.GsonProvider
import com.google.gson.annotations.SerializedName
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.webtoapp.core.network.NetworkModule
import okhttp3.Request

// ==================== Data Models ====================

/**
 * 在线音乐曲目
 */
data class OnlineMusicTrack(
    val id: String,
    val name: String,
    val artist: String,
    val coverUrl: String? = null,
    val playUrl: String? = null,
    val duration: Long = 0,
    val sourceChannelId: String = "",
    val lrcText: String? = null,
    val searchQuery: String? = null,
    val resultIndex: Int = 0
)

/**
 * 搜索响应
 */
data class MusicSearchResponse(
    val tracks: List<OnlineMusicTrack>,
    val hasMore: Boolean = false,
    val total: Int = 0
)

/**
 * 渠道连接状态
 */
data class ChannelStatus(
    val channelId: String,
    val isAvailable: Boolean,
    val latencyMs: Long = 0,
    val errorMessage: String? = null
)

// ==================== Backward Compatibility ====================

data class OnlineMusicData(
    @SerializedName("name") val name: String,
    @SerializedName("picurl") val coverUrl: String?,
    @SerializedName("id") val id: Long,
    @SerializedName("singers") val singers: List<OnlineSinger>?,
    @SerializedName("url") val url: String,
    @SerializedName("pay") val isPaid: Boolean = false
)

data class OnlineSinger(
    @SerializedName("name") val name: String,
    @SerializedName("id") val id: Long
)

fun OnlineMusicTrack.toOnlineMusicData(): OnlineMusicData {
    return OnlineMusicData(
        name = this.name,
        coverUrl = this.coverUrl,
        id = this.id.toLongOrNull() ?: 0L,
        singers = listOf(OnlineSinger(name = this.artist, id = 0)),
        url = this.playUrl ?: "",
        isPaid = false
    )
}

// ==================== Channel Interface ====================

/**
 * 音乐渠道抽象类
 */
abstract class MusicChannel {
    abstract val id: String
    abstract val displayName: String
    abstract val description: String

    abstract suspend fun search(query: String, page: Int = 1): Result<MusicSearchResponse>
    abstract suspend fun getTrackDetail(track: OnlineMusicTrack): Result<OnlineMusicTrack>
    abstract suspend fun testConnection(): ChannelStatus

    companion object {
        private val sharedGson get() = GsonProvider.gson
    }

    protected val client get() = NetworkModule.defaultClient
    protected val gson: Gson get() = sharedGson

    protected fun executeRequest(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) response.body?.string() else null
        } catch (e: Exception) {
            AppLogger.e("MusicChannel", "Request failed: $url", e)
            null
        }
    }

    protected fun measureLatency(testBlock: () -> Boolean): ChannelStatus {
        val start = System.currentTimeMillis()
        return try {
            val ok = testBlock()
            val latency = System.currentTimeMillis() - start
            ChannelStatus(id, ok, latency, if (!ok) "API returned error" else null)
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            ChannelStatus(id, false, latency, e.message)
        }
    }
}

// ==================== Channel: NetEase Cloud Music (oiapi.net) ====================

class NetEaseChannel : MusicChannel() {
    override val id = "netease"
    override val displayName = "网易云音乐"
    override val description = "NetEase Cloud Music"
    private val baseUrl = "https://oiapi.net/api/Music_163"

    override suspend fun search(query: String, page: Int): Result<MusicSearchResponse> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val body = executeRequest("$baseUrl?name=$encoded")
                    ?: return@withContext Result.failure(Exception("网络请求失败"))
                val json = JsonParser.parseString(body).asJsonObject
                if (json.get("code")?.asInt != 0)
                    return@withContext Result.failure(Exception(json.get("message")?.asString ?: "搜索失败"))

                val tracks = mutableListOf<OnlineMusicTrack>()
                val data = json.get("data")
                if (data != null && data.isJsonArray) {
                    data.asJsonArray.forEachIndexed { idx, el ->
                        val obj = el.asJsonObject
                        tracks.add(OnlineMusicTrack(
                            id = obj.get("id")?.asString ?: "$idx",
                            name = obj.get("name")?.asString ?: "",
                            artist = obj.getAsJsonArray("singers")
                                ?.joinToString("、") { it.asJsonObject.get("name")?.asString ?: "" }
                                ?: "未知歌手",
                            coverUrl = obj.get("picurl")?.asString,
                            // 搜索列表没有 URL，playUrl 必须通过 getTrackDetail 获取
                            playUrl = null,
                            sourceChannelId = id,
                            searchQuery = query,
                            resultIndex = idx + 1
                        ))
                    }
                } else if (data != null && data.isJsonObject) {
                    // 只有一条结果时返回对象，且包含 url
                    val obj = data.asJsonObject
                    tracks.add(OnlineMusicTrack(
                        id = obj.get("id")?.asString ?: "0",
                        name = obj.get("name")?.asString ?: "",
                        artist = obj.getAsJsonArray("singers")
                            ?.joinToString("、") { it.asJsonObject.get("name")?.asString ?: "" }
                            ?: "未知歌手",
                        coverUrl = obj.get("picurl")?.asString,
                        playUrl = obj.get("url")?.asString,
                        sourceChannelId = id,
                        searchQuery = query,
                        resultIndex = 1
                    ))
                }
                Result.success(MusicSearchResponse(tracks, total = tracks.size))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getTrackDetail(track: OnlineMusicTrack): Result<OnlineMusicTrack> =
        withContext(Dispatchers.IO) {
            try {
                // 优先使用 searchQuery + resultIndex 获取指定歌曲
                val query = track.searchQuery
                val url = if (query != null) {
                    val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                    "$baseUrl?name=$encoded&n=${track.resultIndex}"
                } else {
                    "$baseUrl?id=${track.id}"
                }
                AppLogger.i("NetEaseChannel", "Getting detail: $url")
                val body = executeRequest(url)
                    ?: return@withContext Result.failure(Exception("网络请求失败"))
                val json = JsonParser.parseString(body).asJsonObject
                if (json.get("code")?.asInt != 0)
                    return@withContext Result.failure(Exception("获取详情失败"))
                val data = json.getAsJsonObject("data")
                    ?: return@withContext Result.failure(Exception("数据为空"))
                
                val playUrl = data.get("url")?.asString
                if (playUrl.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("无法获取播放链接（可能是付费歌曲）"))
                }
                
                Result.success(track.copy(
                    playUrl = playUrl,
                    coverUrl = data.get("picurl")?.asString ?: track.coverUrl
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun testConnection(): ChannelStatus = withContext(Dispatchers.IO) {
        measureLatency {
            val body = executeRequest("$baseUrl?name=test") ?: return@measureLatency false
            val json = JsonParser.parseString(body).asJsonObject
            json.get("code")?.asInt == 0
        }
    }
}

// ==================== Channel: Cenguigui Base (shared logic for kugou/kuwo/netease_alt) ====================

abstract class CenguiguiChannel : MusicChannel() {
    abstract val apiPath: String
    private val apiBase = "https://api.cenguigui.cn/api"

    override suspend fun search(query: String, page: Int): Result<MusicSearchResponse> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val body = executeRequest("$apiBase/$apiPath/?msg=$encoded")
                    ?: return@withContext Result.failure(Exception("网络请求失败"))
                val json = JsonParser.parseString(body).asJsonObject
                val code = json.get("code")?.asInt ?: -1
                if (code != 200)
                    return@withContext Result.failure(Exception(json.get("msg")?.asString ?: "搜索失败"))

                val tracks = mutableListOf<OnlineMusicTrack>()
                val data = json.get("data")
                if (data != null && data.isJsonArray) {
                    // 列表搜索结果，没有 playUrl，需要通过 detail 获取
                    data.asJsonArray.forEachIndexed { idx, el ->
                        val obj = el.asJsonObject
                        tracks.add(parseListItem(obj, query, idx + 1))
                    }
                } else if (data != null && data.isJsonObject) {
                    // 单曲结果，包含 playUrl
                    tracks.add(parseDetailItem(data.asJsonObject, query, 1))
                }
                Result.success(MusicSearchResponse(tracks, total = tracks.size))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getTrackDetail(track: OnlineMusicTrack): Result<OnlineMusicTrack> =
        withContext(Dispatchers.IO) {
            try {
                if (!track.playUrl.isNullOrBlank()) return@withContext Result.success(track)
                val query = track.searchQuery ?: return@withContext Result.failure(Exception("无搜索关键词"))
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val url = "$apiBase/$apiPath/?msg=$encoded&n=${track.resultIndex}"
                AppLogger.i("CenguiguiChannel", "Getting detail: $url")
                val body = executeRequest(url)
                    ?: return@withContext Result.failure(Exception("网络请求失败"))
                val json = JsonParser.parseString(body).asJsonObject
                if (json.get("code")?.asInt != 200)
                    return@withContext Result.failure(Exception("获取详情失败"))
                val data = json.getAsJsonObject("data")
                    ?: return@withContext Result.failure(Exception("数据为空"))
                
                val detail = parseDetailItem(data, track.searchQuery, track.resultIndex)
                if (detail.playUrl.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("无法获取播放链接"))
                }
                
                Result.success(detail)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun testConnection(): ChannelStatus = withContext(Dispatchers.IO) {
        measureLatency {
            val body = executeRequest("$apiBase/$apiPath/?msg=test&n=1") ?: return@measureLatency false
            val json = JsonParser.parseString(body).asJsonObject
            json.get("code")?.asInt == 200
        }
    }

    private fun parseListItem(obj: com.google.gson.JsonObject, query: String, index: Int): OnlineMusicTrack {
        return OnlineMusicTrack(
            id = obj.get("rid")?.asString ?: obj.get("hash")?.asString ?: "$index",
            name = obj.get("name")?.asString ?: obj.get("song_name")?.asString ?: "",
            artist = obj.get("artist")?.asString ?: "未知歌手",
            coverUrl = obj.get("pic")?.asString ?: obj.get("img")?.asString,
            // 列表结果没有 playUrl
            playUrl = null,
            sourceChannelId = id,
            searchQuery = query,
            resultIndex = index
        )
    }

    private fun parseDetailItem(obj: com.google.gson.JsonObject, query: String?, index: Int): OnlineMusicTrack {
        return OnlineMusicTrack(
            id = obj.get("rid")?.asString ?: obj.get("song_id")?.asString ?: "$index",
            name = obj.get("name")?.asString ?: obj.get("song_name")?.asString ?: "",
            artist = obj.get("artist")?.asString ?: "未知歌手",
            coverUrl = obj.get("pic")?.asString ?: obj.get("img")?.asString,
            playUrl = obj.get("url")?.asString ?: obj.get("play_url")?.asString,
            lrcText = obj.get("lrc")?.asString,
            sourceChannelId = id,
            searchQuery = query,
            resultIndex = index
        )
    }
}

class NetEaseAltChannel : CenguiguiChannel() {
    override val id = "netease_alt"
    override val displayName = "网易云音乐(备用)"
    override val description = "NetEase Cloud Music (Alt)"
    override val apiPath = "netease"
}

class KugouChannel : CenguiguiChannel() {
    override val id = "kugou"
    override val displayName = "酷狗音乐"
    override val description = "Kugou Music"
    override val apiPath = "kugou"
}

class KuwoChannel : CenguiguiChannel() {
    override val id = "kuwo"
    override val displayName = "酷我音乐"
    override val description = "Kuwo Music"
    override val apiPath = "kuwo"
}

// ==================== Channel: iTunes Search ====================

class ITunesChannel : MusicChannel() {
    override val id = "itunes"
    override val displayName = "iTunes"
    override val description = "Apple Music (30s Preview)"
    private val baseUrl = "https://itunes.apple.com/search"

    override suspend fun search(query: String, page: Int): Result<MusicSearchResponse> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val body = executeRequest("$baseUrl?term=$encoded&media=music&limit=20")
                    ?: return@withContext Result.failure(Exception("网络请求失败"))
                val json = JsonParser.parseString(body).asJsonObject
                val results = json.getAsJsonArray("results") ?: return@withContext Result.success(
                    MusicSearchResponse(emptyList())
                )
                val tracks = results.mapIndexed { idx, el ->
                    val obj = el.asJsonObject
                    OnlineMusicTrack(
                        id = obj.get("trackId")?.asString ?: "$idx",
                        name = obj.get("trackName")?.asString ?: "",
                        artist = obj.get("artistName")?.asString ?: "Unknown",
                        coverUrl = obj.get("artworkUrl100")?.asString,
                        playUrl = obj.get("previewUrl")?.asString,
                        duration = obj.get("trackTimeMillis")?.asLong ?: 0,
                        sourceChannelId = id,
                        resultIndex = idx + 1
                    )
                }
                Result.success(MusicSearchResponse(tracks, total = json.get("resultCount")?.asInt ?: 0))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getTrackDetail(track: OnlineMusicTrack): Result<OnlineMusicTrack> {
        // iTunes 的 previewUrl 在搜索结果中就已经有了
        return Result.success(track)
    }

    override suspend fun testConnection(): ChannelStatus = withContext(Dispatchers.IO) {
        measureLatency {
            val body = executeRequest("$baseUrl?term=test&media=music&limit=1")
                ?: return@measureLatency false
            val json = JsonParser.parseString(body).asJsonObject
            (json.get("resultCount")?.asInt ?: 0) >= 0
        }
    }
}

// ==================== Channel: QiShui Music (汽水音乐/抖音) ====================

class QiShuiChannel : MusicChannel() {
    override val id = "qishui"
    override val displayName = "汽水音乐"
    override val description = "QiShui Music (Douyin)"
    private val baseUrl = "https://api.cenguigui.cn/api/qishui"

    override suspend fun search(query: String, page: Int): Result<MusicSearchResponse> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                // 汽水音乐API一次返回一首，通过 n 参数获取不同结果
                val tracks = mutableListOf<OnlineMusicTrack>()
                for (n in 1..5) {
                    try {
                        val body = executeRequest("$baseUrl/?msg=$encoded&type=json&n=$n")
                            ?: continue
                        val json = JsonParser.parseString(body).asJsonObject
                        if (json.get("code")?.asInt != 200) continue
                        val data = json.getAsJsonObject("data") ?: continue
                        
                        val title = data.get("title")?.asString ?: continue
                        val singer = data.get("singer")?.asString ?: "未知歌手"
                        val musicUrl = data.get("music")?.asString
                        val cover = data.get("cover")?.asString
                        val lrc = data.get("lrc")?.asString
                        val isPay = data.get("pay")?.asString == "pay"
                        
                        tracks.add(OnlineMusicTrack(
                            id = "qishui_${n}_${title.hashCode()}",
                            name = title + if (isPay) " (替换源)" else "",
                            artist = singer,
                            coverUrl = cover,
                            playUrl = musicUrl,
                            lrcText = lrc,
                            sourceChannelId = id,
                            searchQuery = query,
                            resultIndex = n
                        ))
                    } catch (e: Exception) {
                        AppLogger.w("QiShuiChannel", "Failed to get result $n: ${e.message}")
                    }
                }
                
                if (tracks.isEmpty()) {
                    return@withContext Result.failure(Exception("未找到相关歌曲"))
                }
                Result.success(MusicSearchResponse(tracks, total = tracks.size))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getTrackDetail(track: OnlineMusicTrack): Result<OnlineMusicTrack> {
        // 搜索结果已包含完整信息
        if (!track.playUrl.isNullOrBlank()) return Result.success(track)
        
        return withContext(Dispatchers.IO) {
            try {
                val query = track.searchQuery ?: return@withContext Result.failure(Exception("无搜索关键词"))
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val body = executeRequest("$baseUrl/?msg=$encoded&type=json&n=${track.resultIndex}")
                    ?: return@withContext Result.failure(Exception("网络请求失败"))
                val json = JsonParser.parseString(body).asJsonObject
                if (json.get("code")?.asInt != 200)
                    return@withContext Result.failure(Exception("获取详情失败"))
                val data = json.getAsJsonObject("data")
                    ?: return@withContext Result.failure(Exception("数据为空"))
                
                val musicUrl = data.get("music")?.asString
                if (musicUrl.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("无法获取播放链接"))
                }
                
                Result.success(track.copy(
                    playUrl = musicUrl,
                    coverUrl = data.get("cover")?.asString ?: track.coverUrl,
                    lrcText = data.get("lrc")?.asString ?: track.lrcText
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun testConnection(): ChannelStatus = withContext(Dispatchers.IO) {
        measureLatency {
            val body = executeRequest("$baseUrl/?msg=test&type=json&n=1") ?: return@measureLatency false
            val json = JsonParser.parseString(body).asJsonObject
            json.get("code")?.asInt == 200
        }
    }
}

// ==================== API Manager ====================

/**
 * 在线音乐 API 管理器
 * 支持多渠道搜索、播放、下载
 */
object OnlineMusicApi {

    private const val TAG = "OnlineMusicApi"

    val channels: List<MusicChannel> = listOf(
        QiShuiChannel(),
        KuwoChannel(),
        NetEaseChannel(),
        NetEaseAltChannel(),
        KugouChannel(),
        ITunesChannel()
    )

    private val channelMap: Map<String, MusicChannel> = channels.associateBy { it.id }
    private val channelStatusCache = mutableMapOf<String, ChannelStatus>()

    fun getChannel(channelId: String): MusicChannel? = channelMap[channelId]

    /**
     * 测试所有渠道连接
     */
    suspend fun testAllChannels(): Map<String, ChannelStatus> {
        val results = mutableMapOf<String, ChannelStatus>()
        for (channel in channels) {
            try {
                val status = channel.testConnection()
                results[channel.id] = status
                channelStatusCache[channel.id] = status
                AppLogger.i(TAG, "Channel ${channel.displayName}: ${if (status.isAvailable) "✓" else "✗"} (${status.latencyMs}ms)")
            } catch (e: Exception) {
                val status = ChannelStatus(channel.id, false, 0, e.message)
                results[channel.id] = status
                channelStatusCache[channel.id] = status
            }
        }
        return results
    }

    /**
     * 测试单个渠道
     */
    suspend fun testChannel(channelId: String): ChannelStatus {
        val channel = channelMap[channelId]
            ?: return ChannelStatus(channelId, false, 0, "渠道不存在")
        val status = channel.testConnection()
        channelStatusCache[channelId] = status
        return status
    }

    /**
     * 获取缓存的渠道状态
     */
    fun getCachedStatus(channelId: String): ChannelStatus? = channelStatusCache[channelId]

    /**
     * 搜索音乐
     */
    suspend fun search(channelId: String, query: String, page: Int = 1): Result<MusicSearchResponse> {
        val channel = channelMap[channelId]
            ?: return Result.failure(Exception("渠道不存在: $channelId"))
        return channel.search(query, page)
    }

    /**
     * 获取曲目详情（含播放链接）
     */
    suspend fun getTrackDetail(track: OnlineMusicTrack): Result<OnlineMusicTrack> {
        val channel = channelMap[track.sourceChannelId]
            ?: return Result.failure(Exception("渠道不存在: ${track.sourceChannelId}"))
        return channel.getTrackDetail(track)
    }
}
