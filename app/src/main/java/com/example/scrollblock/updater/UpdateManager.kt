package com.example.scrollblock.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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

object UpdateManager {

    private const val TAG = "UpdateManager"
    private const val FILE_PROVIDER_AUTHORITY = "com.scrollblock.app.fileprovider"

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
     * Checks GitHub for the latest version information from version.json.
     */
    suspend fun checkRemoteVersion(): RemoteVersionInfo? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(VERSION_CHECK_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", "ScrollBlock-Updater")
            connection.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")

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
     * Checks if the app has permission to install packages on Android 8.0+.
     */
    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Opens the system settings screen to allow installing unknown apps.
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Launches the system package installer for an APK file.
     */
    fun installApkFile(context: Context, apkFile: File) {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK file", e)
            throw e
        }
    }

    /**
     * Copies a user-picked APK URI from Downloads/storage and launches installation.
     */
    suspend fun installFromPickedUri(context: Context, pickedUri: Uri) = withContext(Dispatchers.IO) {
        val destFile = File(context.cacheDir, "picked_update.apk")
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
     * Downloads an APK from a direct URL (with GitHub 302 redirect support) and triggers installation.
     */
    suspend fun downloadAndInstall(
        context: Context,
        urlString: String,
        onProgress: (Int) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        var input: InputStream? = null
        var output: FileOutputStream? = null
        var connection: HttpURLConnection? = null

        try {
            var currentUrl = urlString
            var redirects = 0
            val maxRedirects = 6

            while (redirects < maxRedirects) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = true
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
            val apkFile = File(context.cacheDir, "network_update.apk")

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

            withContext(Dispatchers.Main) {
                installApkFile(context, apkFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download error", e)
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
