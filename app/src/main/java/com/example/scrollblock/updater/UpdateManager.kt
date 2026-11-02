package com.example.scrollblock.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Uygulama içi güncelleme ve otomatik APK temizleme yöneticisi:
 *  - Yeni sürümü uygulama içinde ilerleme çubuğu ile indirir ve kurulum ekranını açar.
 *  - Kurulum tamamlanıp uygulama/servis başladığında indirilen geçici APK dosyalarını
 *    otomatik olarak siler (telefonda gereksiz yer kaplamaz).
 */
object UpdateManager {

    private const val TAG = "UpdateManager"
    private const val FILE_PROVIDER_AUTHORITY = "com.scrollblock.app.fileprovider"
    private const val PREFS_UPDATER = "scrollblock_updater_prefs"
    private const val KEY_PENDING_APK_VERSION = "pending_apk_version_code"
    private const val NETWORK_APK_NAME = "network_update.apk"
    private const val PICKED_APK_NAME = "picked_update.apk"

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
     * Kurulum tamamlandıktan sonra (veya uygulama açılışında) indirilen geçici APK dosyalarını siler.
     */
    fun cleanupInstalledApks(context: Context, forceAll: Boolean = false) {
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_UPDATER, Context.MODE_PRIVATE)
            val pendingVersion = prefs.getInt(KEY_PENDING_APK_VERSION, -1)
            val currentVersion = getCurrentVersionCode(context)

            // Kurulum tamamlandıysa (mevcut sürüm >= indirilen sürüm) veya açılış temizliğiyse APK'ları sil
            val shouldDelete = forceAll || pendingVersion <= 0 || currentVersion >= pendingVersion
            if (shouldDelete) {
                deleteApkFilesInDir(context.cacheDir)
                deleteApkFilesInDir(context.filesDir)
                context.externalCacheDir?.let { deleteApkFilesInDir(it) }
                prefs.edit().remove(KEY_PENDING_APK_VERSION).apply()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error cleaning up temporary APK files: ${e.message}")
        }
    }

    private fun deleteApkFilesInDir(dir: File?) {
        if (dir == null || !dir.exists()) return
        dir.listFiles()?.forEach { file ->
            if (file.isFile && (
                    file.name.endsWith(".apk", ignoreCase = true) ||
                        file.name == NETWORK_APK_NAME ||
                        file.name == PICKED_APK_NAME
                    )
            ) {
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
     * Launches the system package installer for an APK file.
     */
    fun installApkFile(context: Context, apkFile: File) {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            FILE_PROVIDER_AUTHORITY,
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    /**
     * Copies a user-picked APK URI from storage, launches installation, and marks it for auto-deletion.
     */
    suspend fun installFromPickedUri(context: Context, pickedUri: Uri) = withContext(Dispatchers.IO) {
        cleanupInstalledApks(context, forceAll = true)
        val destFile = File(context.cacheDir, PICKED_APK_NAME)
        context.contentResolver.openInputStream(pickedUri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        withContext(Dispatchers.Main) {
            installApkFile(context, destFile)
        }
    }

    /**
     * Downloads the APK inside the app with live progress and launches the installer.
     * Once installed, [cleanupInstalledApks] automatically deletes the APK file.
     */
    suspend fun downloadAndInstall(
        context: Context,
        urlString: String,
        targetVersionCode: Int = -1,
        onProgress: (Int) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        var input: InputStream? = null
        var output: FileOutputStream? = null
        var connection: HttpURLConnection? = null

        try {
            // Yeni indirmeden önce eski APK kalıntılarını temizle
            cleanupInstalledApks(context, forceAll = true)

            val separator = if (urlString.contains("?")) "&" else "?"
            var currentUrl = "$urlString${separator}t=${System.currentTimeMillis()}"
            var redirects = 0
            val maxRedirects = 6

            while (redirects < maxRedirects) {
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
            val apkFile = File(context.cacheDir, NETWORK_APK_NAME)

            input = connection.inputStream
            output = FileOutputStream(apkFile)

            val data = ByteArray(8192)
            var total: Long = 0
            var count: Int

            while (input.read(data).also { count = it } != -1) {
                total += count
                if (fileLength > 0) {
                    val progress = ((total * 100) / fileLength).toInt()
                    withContext(Dispatchers.Main) {
                        onProgress(progress)
                    }
                }
                output.write(data, 0, count)
            }

            output.flush()

            if (targetVersionCode > 0) {
                context.applicationContext
                    .getSharedPreferences(PREFS_UPDATER, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(KEY_PENDING_APK_VERSION, targetVersionCode)
                    .apply()
            }

            withContext(Dispatchers.Main) {
                installApkFile(context, apkFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
            cleanupInstalledApks(context, forceAll = true)
            withContext(Dispatchers.Main) {
                onError("İndirme başarısız: ${e.localizedMessage ?: "Bağlantı hatası"}")
            }
        } finally {
            output?.close()
            input?.close()
            connection?.disconnect()
        }
    }
}
