package com.example.scrollblock.updater

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Play Protect uyumlu uygulama içi güncelleme ve otomatik APK temizleme yöneticisi:
 *  - Yeni sürümü tarayıcı açmadan, uygulama içindeki ilerleme çubuğu (%0..%100) ile indirir
 *    ve doğrudan telefonun genel "İndirilenler" (Dosyalarım -> İndirilenler / Downloads) klasörüne
 *    "ScrollBlock-guncelleme.apk" adıyla kaydeder.
 *  - REQUEST_INSTALL_PACKAGES izni kullanmadığı için Google Play Protect "zararlı uygulama"
 *    uyarısı tetiklemez.
 *  - Kurulum tamamlanıp yeni sürüm açıldığında (currentVersion >= pendingVersion) indirilen APK
 *    dosyasını İndirilenler klasöründen otomatik olarak siler.
 */
object UpdateManager {

    private const val TAG = "UpdateManager"
    private const val PREFS_UPDATER = "scrollblock_updater_prefs"
    private const val KEY_PENDING_APK_VERSION = "pending_apk_version_code"
    private const val KEY_LAST_DOWNLOAD_URI = "last_download_media_uri"
    private const val KEY_LAST_DOWNLOAD_ID = "last_download_id"
    const val DOWNLOAD_FILE_NAME = "ScrollBlock-guncelleme.apk"

    const val DEFAULT_GITHUB_APK_URL =
        "https://raw.githubusercontent.com/sytex10/scrollblock/main/release/ScrollBlock-release.apk"

    const val VERSION_CHECK_URL =
        "https://raw.githubusercontent.com/sytex10/scrollblock/main/version.json"

    private const val GITHUB_API_VERSION_URL =
        "https://api.github.com/repos/sytex10/scrollblock/contents/version.json?ref=main"

    data class RemoteVersionInfo(
        val versionCode: Int,
        val versionName: String,
        val changelog: String,
        val apkUrl: String
    )

    /**
     * Kurulum tamamlandıktan sonra (currentVersion >= pendingVersion) veya yeni indirme öncesinde
     * (forceAll = true) indirilen APK dosyasını İndirilenler klasöründen otomatik olarak siler.
     */
    fun cleanupInstalledApks(context: Context, forceAll: Boolean = false) {
        try {
            val appCtx = context.applicationContext
            val prefs = appCtx.getSharedPreferences(PREFS_UPDATER, Context.MODE_PRIVATE)
            val pendingVersion = prefs.getInt(KEY_PENDING_APK_VERSION, -1)
            val lastUriStr = prefs.getString(KEY_LAST_DOWNLOAD_URI, null)
            val lastDownloadId = prefs.getLong(KEY_LAST_DOWNLOAD_ID, -1L)
            val currentVersion = getCurrentVersionCode(appCtx)

            val shouldDelete = forceAll || (pendingVersion > 0 && currentVersion >= pendingVersion)
            if (!shouldDelete) return

            // 1. MediaStore üzerinden İndirilenler klasörüne yazılan APK'yı sil
            if (!lastUriStr.isNullOrEmpty()) {
                try {
                    appCtx.contentResolver.delete(Uri.parse(lastUriStr), null, null)
                } catch (_: Exception) { }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    appCtx.contentResolver.delete(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
                        arrayOf("ScrollBlock%.apk")
                    )
                } catch (_: Exception) { }
            }

            // 2. DownloadManager kaydı varsa sil
            if (lastDownloadId > 0L) {
                try {
                    val dm = appCtx.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                    dm?.remove(lastDownloadId)
                } catch (_: Exception) { }
            }

            // 3. Genel İndirilenler ve uygulama önbellek klasörlerindeki APK kalıntılarını sil
            deleteApkFilesInDir(appCtx.cacheDir)
            deleteApkFilesInDir(appCtx.filesDir)
            appCtx.externalCacheDir?.let { deleteApkFilesInDir(it) }
            appCtx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { deleteApkFilesInDir(it) }
            try {
                val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                publicDownloads?.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.startsWith("ScrollBlock", ignoreCase = true) &&
                        file.name.endsWith(".apk", ignoreCase = true)
                    ) {
                        try { file.delete() } catch (_: Exception) { }
                    }
                }
            } catch (_: Exception) { }

            prefs.edit()
                .remove(KEY_PENDING_APK_VERSION)
                .remove(KEY_LAST_DOWNLOAD_URI)
                .remove(KEY_LAST_DOWNLOAD_ID)
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up temporary APK files: ${e.message}")
        }
    }

    private fun deleteApkFilesInDir(dir: File?) {
        if (dir == null || !dir.exists()) return
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".apk", ignoreCase = true)) {
                try {
                    file.delete()
                } catch (_: Exception) { }
            }
        }
    }

    private fun getCurrentVersionCode(context: Context): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (_: Exception) {
            0
        }
    }

    /**
     * Checks GitHub for the latest version information from version.json.
     * Queries both the live GitHub Contents API (instant, uncached) and raw.githubusercontent.com
     * and returns whichever reports the newest versionCode.
     */
    suspend fun checkRemoteVersion(): RemoteVersionInfo? = withContext(Dispatchers.IO) {
        val apiResult = fetchVersionFromUrl(
            urlString = "$GITHUB_API_VERSION_URL&_t=${System.currentTimeMillis()}",
            acceptHeader = "application/vnd.github.v3.raw"
        )
        val rawResult = fetchVersionFromUrl(
            urlString = "$VERSION_CHECK_URL?t=${System.currentTimeMillis()}",
            acceptHeader = "application/json"
        )

        return@withContext when {
            apiResult != null && rawResult != null ->
                if (apiResult.versionCode >= rawResult.versionCode) apiResult else rawResult
            apiResult != null -> apiResult
            else -> rawResult
        }
    }

    private fun fetchVersionFromUrl(urlString: String, acceptHeader: String): RemoteVersionInfo? {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.useCaches = false
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", "ScrollBlock-Updater")
            connection.setRequestProperty("Accept", acceptHeader)
            connection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
            connection.setRequestProperty("Pragma", "no-cache")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(text)
                return RemoteVersionInfo(
                    versionCode = obj.getInt("versionCode"),
                    versionName = obj.getString("versionName"),
                    changelog = obj.optString("changelog", ""),
                    apkUrl = obj.optString("apkUrl", DEFAULT_GITHUB_APK_URL)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check remote version from $urlString: ${e.message}")
        } finally {
            connection?.disconnect()
        }
        return null
    }

    /**
     * Uygulama içinde yeni sürümü gerçek zamanlı (%0..%100) indirir ve doğrudan telefonun
     * genel "İndirilenler" (Dosyalarım -> İndirilenler) klasörüne "ScrollBlock-guncelleme.apk"
     * olarak kaydeder. İndirme tamamlandığında İndirilenler klasörünü açar.
     * Kurulum tamamlanıp yeni sürüm açıldığında [cleanupInstalledApks] APK'yı otomatik siler.
     */
    suspend fun downloadAndInstall(
        context: Context,
        urlString: String,
        targetVersionCode: Int = -1,
        autoOpenDownloads: Boolean = false,
        onProgress: (Int) -> Unit = {},
        onDownloadCompleted: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        var input: InputStream? = null
        var output: OutputStream? = null
        var connection: HttpURLConnection? = null
        var insertedUri: Uri? = null

        try {
            val appCtx = context.applicationContext
            // Yeni indirmeye başlamadan önce eski APK kalıntılarını temizle
            cleanupInstalledApks(appCtx, forceAll = true)

            val separator = if (urlString.contains("?")) "&" else "?"
            var currentUrl = "$urlString${separator}t=${System.currentTimeMillis()}"
            var redirects = 0

            while (redirects < 6) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
                connection.useCaches = false
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.setRequestProperty("User-Agent", "ScrollBlock-Updater")

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == 307 || status == 308
                ) {
                    val location = connection.getHeaderField("Location") ?: break
                    currentUrl = location
                    connection.disconnect()
                    redirects++
                } else {
                    break
                }
            }

            if (connection == null || connection.responseCode != HttpURLConnection.HTTP_OK) {
                val code = connection?.responseCode ?: -1
                withContext(Dispatchers.Main) {
                    onError("Sunucu bağlantı hatası: HTTP $code")
                }
                return@withContext
            }

            val fileLength = connection.contentLength
            input = connection.inputStream

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = appCtx.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, DOWNLOAD_FILE_NAME)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                insertedUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (insertedUri != null) {
                    output = resolver.openOutputStream(insertedUri)
                }
            }

            if (output == null) {
                val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!publicDir.exists()) publicDir.mkdirs()
                val destFile = File(publicDir, DOWNLOAD_FILE_NAME)
                if (destFile.exists()) destFile.delete()
                output = FileOutputStream(destFile)
            }

            val buffer = ByteArray(8192)
            var total: Long = 0
            var count: Int

            while (input.read(buffer).also { count = it } != -1) {
                total += count
                output.write(buffer, 0, count)
                if (fileLength > 0) {
                    val pct = ((total * 100L) / fileLength).toInt().coerceIn(0, 100)
                    withContext(Dispatchers.Main) {
                        onProgress(pct)
                    }
                }
            }
            output.flush()
            output.close()
            output = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && insertedUri != null) {
                val completeValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                appCtx.contentResolver.update(insertedUri, completeValues, null, null)
            }

            appCtx.getSharedPreferences(PREFS_UPDATER, Context.MODE_PRIVATE)
                .edit()
                .apply {
                    if (targetVersionCode > 0) {
                        putInt(KEY_PENDING_APK_VERSION, targetVersionCode)
                    }
                    if (insertedUri != null) {
                        putString(KEY_LAST_DOWNLOAD_URI, insertedUri.toString())
                    }
                }
                .apply()

            withContext(Dispatchers.Main) {
                onProgress(100)
                onDownloadCompleted()
                if (autoOpenDownloads) {
                    openSystemDownloads(context)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            cleanupInstalledApks(context, forceAll = true)
            withContext(Dispatchers.Main) {
                onError("İndirme başarısız: ${e.localizedMessage ?: "Bağlantı hatası"}")
            }
        } finally {
            try { output?.close() } catch (_: Exception) { }
            try { input?.close() } catch (_: Exception) { }
            connection?.disconnect()
        }
    }

    fun openSystemDownloads(context: Context) {
        try {
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not open system downloads view: ${e.message}")
        }
    }
}
