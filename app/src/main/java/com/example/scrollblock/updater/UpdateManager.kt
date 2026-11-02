package com.example.scrollblock.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Play Protect uyumlu sürüm denetleyici:
 *  - Uygulama içinde REQUEST_INSTALL_PACKAGES veya paket yükleyici (package-archive) KULLANMAZ
 *    (Erişilebilirlik servisi ile birlikte paket yükleyici izni kullanmak Play Protect tarafından
 *    "Dropper" olarak işaretlenip engellenmesine yol açar).
 *  - Yeni sürüm bulunduğunda indirme bağlantısını sistemin güvenli tarayıcısı üzerinden açar.
 */
object UpdateManager {

    private const val TAG = "UpdateManager"

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
     * Opens the direct APK download URL in the system browser so the user can download and install
     * without triggering Play Protect's Accessibility+PackageInstaller heuristic.
     */
    fun openBrowserDownload(context: Context, urlString: String = DEFAULT_GITHUB_APK_URL) {
        try {
            val separator = if (urlString.contains("?")) "&" else "?"
            val finalUrl = "$urlString${separator}t=${System.currentTimeMillis()}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open browser download", e)
        }
    }
}
