package com.example.scrollblock.updater

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Play Protect uyumlu uygulama içi güncelleme ve otomatik APK temizleme yöneticisi:
 *  - Yeni sürümü tarayıcı açmadan, uygulama içindeki ilerleme çubuğu (%0..%100) ile
 *    Android'in sistem DownloadManager servisi üzerinden indirir.
 *  - REQUEST_INSTALL_PACKAGES izni kullanmadığı için Google Play Protect "zararlı uygulama"
 *    uyarısı tetiklemez.
 *  - Kurulum tamamlanıp yeni sürüm açıldığında indirilen APK kaydını ve dosyasını
 *    otomatik olarak siler (telefonda gereksiz yer kaplamaz).
 */
object UpdateManager {

    private const val TAG = "UpdateManager"
    private const val PREFS_UPDATER = "scrollblock_updater_prefs"
    private const val KEY_PENDING_APK_VERSION = "pending_apk_version_code"
    private const val KEY_LAST_DOWNLOAD_ID = "last_download_id"
    private const val DOWNLOAD_FILE_NAME = "ScrollBlock-update.apk"

    const val DEFAULT_GITHUB_APK_URL =
        "https://github.com/sytex10/scrollblock/raw/main/release/ScrollBlock-release.apk"

    const val VERSION_CHECK_URL =
        "https://raw.githubusercontent.com/sytex10/scrollblock/main/version.json"

    data class RemoteVersionInfo(
        val versionCode: Int,
        val versionName: String,
        val changelog: String,
        val apkUrl: String
    )

    /**
     * Kurulum tamamlandıktan sonra (veya yeni indirme öncesinde) indirilen APK dosyasını siler.
     */
    fun cleanupInstalledApks(context: Context, forceAll: Boolean = false) {
        try {
            val appCtx = context.applicationContext
            val prefs = appCtx.getSharedPreferences(PREFS_UPDATER, Context.MODE_PRIVATE)
            val pendingVersion = prefs.getInt(KEY_PENDING_APK_VERSION, -1)
            val lastDownloadId = prefs.getLong(KEY_LAST_DOWNLOAD_ID, -1L)
            val currentVersion = getCurrentVersionCode(appCtx)

            val shouldDelete = forceAll || (pendingVersion > 0 && currentVersion >= pendingVersion)
            if (shouldDelete) {
                if (lastDownloadId > 0L) {
                    try {
                        val dm = appCtx.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                        dm?.remove(lastDownloadId)
                    } catch (_: Exception) { }
                }
                deleteApkFilesInDir(appCtx.cacheDir)
                deleteApkFilesInDir(appCtx.filesDir)
                appCtx.externalCacheDir?.let { deleteApkFilesInDir(it) }
                appCtx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { deleteApkFilesInDir(it) }

                prefs.edit()
                    .remove(KEY_PENDING_APK_VERSION)
                    .remove(KEY_LAST_DOWNLOAD_ID)
                    .apply()
            }
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
     * Checks GitHub for the latest version information from version.json (bypasses CDN cache).
     */
    suspend fun checkRemoteVersion(): RemoteVersionInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val cacheBustedUrl = "$VERSION_CHECK_URL?t=${System.currentTimeMillis()}"
            val url = URL(cacheBustedUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.useCaches = false
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", "ScrollBlock-Updater")
            connection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
            connection.setRequestProperty("Pragma", "no-cache")

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val text = connection.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(text)
                return@withContext RemoteVersionInfo(
                    versionCode = obj.getInt("versionCode"),
                    versionName = obj.getString("versionName"),
                    changelog = obj.optString("changelog", ""),
                    apkUrl = obj.optString("apkUrl", DEFAULT_GITHUB_APK_URL)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check remote version: ${e.message}")
        } finally {
            connection?.disconnect()
        }
        return@withContext null
    }

    /**
     * Uygulama içinde sistem DownloadManager ile yeni sürümü indirir, %0..%100 canlı ilerleme
     * bildirir ve tamamlandığında sistem İndirilenler penceresini açar.
     * Kurulum tamamlandığında [cleanupInstalledApks] indirilen APK'yı otomatik olarak siler.
     */
    suspend fun downloadAndInstall(
        context: Context,
        urlString: String,
        targetVersionCode: Int = -1,
        onProgress: (Int) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val appCtx = context.applicationContext
            cleanupInstalledApks(appCtx, forceAll = true)

            val dm = appCtx.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (dm == null) {
                withContext(Dispatchers.Main) {
                    onError("Sistem indirme yöneticisi başlatılamadı")
                }
                return@withContext
            }

            val separator = if (urlString.contains("?")) "&" else "?"
            val cacheBustedUrl = "$urlString${separator}t=${System.currentTimeMillis()}"

            val request = DownloadManager.Request(Uri.parse(cacheBustedUrl)).apply {
                setTitle("ScrollBlock Güncellemesi")
                setDescription("Yeni sürüm indiriliyor...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                setDestinationInExternalFilesDir(
                    appCtx,
                    Environment.DIRECTORY_DOWNLOADS,
                    DOWNLOAD_FILE_NAME
                )
            }

            val downloadId = dm.enqueue(request)
            appCtx.getSharedPreferences(PREFS_UPDATER, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_DOWNLOAD_ID, downloadId)
                .apply {
                    if (targetVersionCode > 0) {
                        putInt(KEY_PENDING_APK_VERSION, targetVersionCode)
                    }
                }
                .apply()

            var isRunning = true
            while (isRunning) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                dm.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val downloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                        val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else DownloadManager.STATUS_RUNNING
                        val downloaded = if (downloadedIdx >= 0) cursor.getLong(downloadedIdx) else 0L
                        val total = if (totalIdx >= 0) cursor.getLong(totalIdx) else -1L

                        if (total > 0) {
                            val pct = ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
                            withContext(Dispatchers.Main) {
                                onProgress(pct)
                            }
                        }

                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                isRunning = false
                                withContext(Dispatchers.Main) {
                                    onProgress(100)
                                    openSystemDownloads(context)
                                }
                            }
                            DownloadManager.STATUS_FAILED -> {
                                isRunning = false
                                cleanupInstalledApks(appCtx, forceAll = true)
                                withContext(Dispatchers.Main) {
                                    onError("İndirme başarısız oldu. Lütfen tekrar deneyin.")
                                }
                            }
                        }
                    } else {
                        isRunning = false
                    }
                }
                if (isRunning) {
                    delay(150L)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            cleanupInstalledApks(context, forceAll = true)
            withContext(Dispatchers.Main) {
                onError("İndirme başarısız: ${e.localizedMessage ?: "Bağlantı hatası"}")
            }
        }
    }

    private fun openSystemDownloads(context: Context) {
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
