package com.webtoapp.ui.shell

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.util.DownloadHelper

/**
 * Shell Activity 的权限处理委托
 *
 * 封装所有 ActivityResultLauncher 和权限请求逻辑。
 * 必须在 Activity.onCreate() 之前（即 Activity 初始化阶段）实例化。
 */
class ShellPermissionDelegate(private val activity: AppCompatActivity) {

    // Permission请求相关
    private var pendingPermissionRequest: PermissionRequest? = null
    private var pendingGeolocationOrigin: String? = null
    private var pendingGeolocationCallback: GeolocationPermissions.Callback? = null

    // 待下载信息（权限请求后使用）
    private var pendingDownload: PendingDownload? = null

    private data class PendingDownload(
        val url: String,
        val userAgent: String,
        val contentDisposition: String,
        val mimeType: String,
        val contentLength: Long
    )

    // ===== Activity Result Launchers =====

    val fileChooserLauncher = activity.registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        onFileChooserResult?.invoke(uris.toTypedArray())
    }

    // 外部设置的文件选择回调
    var onFileChooserResult: ((Array<android.net.Uri>) -> Unit)? = null

    // Storage权限请求
    private val storagePermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Permission已授予，执行下载
            pendingDownload?.let { download ->
                DownloadHelper.handleDownload(
                    context = activity,
                    url = download.url,
                    userAgent = download.userAgent,
                    contentDisposition = download.contentDisposition,
                    mimeType = download.mimeType,
                    contentLength = download.contentLength,
                    method = DownloadHelper.DownloadMethod.DOWNLOAD_MANAGER,
                    scope = activity.lifecycleScope
                )
            }
        } else {
            Toast.makeText(activity, Strings.storagePermissionRequired, Toast.LENGTH_SHORT).show()
            // 尝试使用浏览器下载
            pendingDownload?.let { download ->
                DownloadHelper.openInBrowser(activity, download.url)
            }
        }
        pendingDownload = null
    }

    // Permission请求launcher（用于摄像头、麦克风等）
    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        AppLogger.d("ShellActivity", "Permission result received: $permissions")
        val allGranted = permissions.values.all { it }
        AppLogger.d("ShellActivity", "All permissions granted: $allGranted")
        pendingPermissionRequest?.let { request ->
            if (allGranted) {
                AppLogger.d("ShellActivity", "Granting WebView permission request")
                request.grant(request.resources)
            } else {
                AppLogger.d("ShellActivity", "Denying WebView permission request")
                request.deny()
            }
            pendingPermissionRequest = null
        } ?: run {
            AppLogger.w("ShellActivity", "pendingPermissionRequest is null!")
        }
    }

    // 位置权限请求launcher
    private val locationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        pendingGeolocationCallback?.invoke(pendingGeolocationOrigin, granted, false)
        pendingGeolocationOrigin = null
        pendingGeolocationCallback = null
    }

    // 通知权限请求launcher（Android 13+）
    private val notificationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            AppLogger.d("ShellActivity", "通知权限已授予")
        } else {
            AppLogger.d("ShellActivity", "通知权限被拒绝")
        }
    }

    // ===== Public Methods =====

    /**
     * 请求通知权限（Android 13+）
     */
    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * 处理WebView权限请求，先请求Android系统权限
     */
    fun handlePermissionRequest(request: PermissionRequest) {
        val resources = request.resources
        val androidPermissions = mutableListOf<String>()

        AppLogger.d("ShellActivity", "handlePermissionRequest called, resources: ${resources.joinToString()}")

        resources.forEach { resource ->
            AppLogger.d("ShellActivity", "Processing resource: $resource")
            when (resource) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                    androidPermissions.add(Manifest.permission.CAMERA)
                    AppLogger.d("ShellActivity", "Added CAMERA permission request")
                }
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                    androidPermissions.add(Manifest.permission.RECORD_AUDIO)
                    androidPermissions.add(Manifest.permission.MODIFY_AUDIO_SETTINGS)
                    AppLogger.d("ShellActivity", "Added RECORD_AUDIO + MODIFY_AUDIO_SETTINGS permission request")
                }
                PermissionRequest.RESOURCE_MIDI_SYSEX -> {
                    // MIDI SysEx 不需要额外 Android 运行时权限，直接授权
                    AppLogger.d("ShellActivity", "MIDI_SYSEX resource, no Android permission needed")
                }
                PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> {
                    // Protected Media ID 不需要额外 Android 运行时权限，直接授权
                    AppLogger.d("ShellActivity", "PROTECTED_MEDIA_ID resource, no Android permission needed")
                }
                else -> {
                    AppLogger.d("ShellActivity", "Unknown resource: $resource, will grant directly")
                }
            }
        }

        AppLogger.d("ShellActivity", "Android permissions to request: ${androidPermissions.joinToString()}")

        if (androidPermissions.isEmpty()) {
            // 不需要Android权限，直接授权WebView
            AppLogger.d("ShellActivity", "No Android permissions needed, granting WebView request directly")
            request.grant(resources)
        } else {
            // 需要先请求Android权限
            AppLogger.d("ShellActivity", "Requesting Android permissions...")
            pendingPermissionRequest = request
            permissionLauncher.launch(androidPermissions.toTypedArray())
        }
    }

    /**
     * 处理地理位置权限请求
     */
    fun handleGeolocationPermission(origin: String?, callback: GeolocationPermissions.Callback?) {
        pendingGeolocationOrigin = origin
        pendingGeolocationCallback = callback
        locationPermissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    /**
     * 处理下载（带权限检查）
     */
    fun handleDownloadWithPermission(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long,
        webView: android.webkit.WebView?
    ) {
        // Create Blob 下载回调
        val onBlobDownload: ((String, String) -> Unit) = { blobUrl, filename ->
            val safeBlobUrl = org.json.JSONObject.quote(blobUrl)
            val safeFilename = org.json.JSONObject.quote(filename)
            // 大文件使用分块处理避免 DOM 冻结
            webView?.evaluateJavascript("""
                (function() {
                    try {
                        const blobUrl = $safeBlobUrl;
                        const filename = $safeFilename;
                        const LARGE_FILE_THRESHOLD = 10 * 1024 * 1024;
                        const CHUNK_SIZE = 1024 * 1024;
                        
                        function uint8ToBase64(u8) {
                            const S = 8192; const p = [];
                            for (let i = 0; i < u8.length; i += S) p.push(String.fromCharCode.apply(null, u8.subarray(i, i + S)));
                            return btoa(p.join(''));
                        }
                        
                        function processChunked(blob, fname) {
                            const mimeType = blob.type || 'application/octet-stream';
                            if (!window.AndroidDownload || !window.AndroidDownload.startChunkedDownload) {
                                processSmall(blob, fname); return;
                            }
                            const did = window.AndroidDownload.startChunkedDownload(fname, mimeType, blob.size);
                            let off = 0, ci = 0; const tc = Math.ceil(blob.size / CHUNK_SIZE);
                            function next() {
                                if (off >= blob.size) { window.AndroidDownload.finishChunkedDownload(did); return; }
                                blob.slice(off, off + CHUNK_SIZE).arrayBuffer().then(function(ab) {
                                    window.AndroidDownload.appendChunk(did, uint8ToBase64(new Uint8Array(ab)), ci, tc);
                                    off += CHUNK_SIZE; ci++;
                                    setTimeout(next, 0);
                                });
                            }
                            next();
                        }
                        
                        function processSmall(blob, fname) {
                            const reader = new FileReader();
                            reader.onloadend = function() {
                                const base64Data = reader.result.split(',')[1];
                                const mimeType = blob.type || 'application/octet-stream';
                                if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                                    window.AndroidDownload.saveBase64File(base64Data, fname, mimeType);
                                }
                            };
                            reader.readAsDataURL(blob);
                        }
                        
                        if (blobUrl.startsWith('data:')) {
                            const parts = blobUrl.split(',');
                            const meta = parts[0];
                            const base64Data = parts[1];
                            const mimeMatch = meta.match(/data:([^;]+)/);
                            const mimeType = mimeMatch ? mimeMatch[1] : 'application/octet-stream';
                            if (window.AndroidDownload && window.AndroidDownload.saveBase64File) {
                                window.AndroidDownload.saveBase64File(base64Data, filename, mimeType);
                            }
                        } else if (blobUrl.startsWith('blob:')) {
                            fetch(blobUrl)
                                .then(function(r) { return r.blob(); })
                                .then(function(blob) {
                                    if (blob.size > LARGE_FILE_THRESHOLD) {
                                        processChunked(blob, filename);
                                    } else {
                                        processSmall(blob, filename);
                                    }
                                })
                                .catch(function(err) {
                                    console.error('[DownloadHelper] Blob fetch failed:', err);
                                    if (window.AndroidDownload && window.AndroidDownload.showToast) {
                                        window.AndroidDownload.showToast('${Strings.downloadFailedWithReason}' + err.message);
                                    }
                                });
                        }
                    } catch(e) {
                        console.error('[DownloadHelper] Error:', e);
                    }
                })();
            """.trimIndent(), null)
        }

        // Android 10+ 不需要存储权限即可使用 DownloadManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            DownloadHelper.handleDownload(
                context = activity,
                url = url,
                userAgent = userAgent,
                contentDisposition = contentDisposition,
                mimeType = mimeType,
                contentLength = contentLength,
                method = DownloadHelper.DownloadMethod.DOWNLOAD_MANAGER,
                scope = activity.lifecycleScope,
                onBlobDownload = onBlobDownload
            )
            return
        }

        // Android 9 及以下需要检查存储权限
        val hasPermission = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            DownloadHelper.handleDownload(
                context = activity,
                url = url,
                userAgent = userAgent,
                contentDisposition = contentDisposition,
                mimeType = mimeType,
                contentLength = contentLength,
                method = DownloadHelper.DownloadMethod.DOWNLOAD_MANAGER,
                scope = activity.lifecycleScope,
                onBlobDownload = onBlobDownload
            )
        } else {
            // Save下载信息，请求权限
            pendingDownload = PendingDownload(url, userAgent, contentDisposition, mimeType, contentLength)
            storagePermissionLauncher.launch(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            )
        }
    }
}
