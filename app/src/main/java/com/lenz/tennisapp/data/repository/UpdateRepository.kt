package com.lenz.tennisapp.data.repository

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.lenz.tennisapp.BuildConfig
import com.lenz.tennisapp.data.api.GitHubService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateInfo(
    val versionName: String,   // e.g. "1.1"
    val tagName: String,       // e.g. "v1.1"
    val releaseNotes: String,
    val apkUrl: String,
    val apkSizeBytes: Long
)

@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gitHub: GitHubService
) {
    companion object {
        private const val OWNER = "lenzwagner"
        private const val REPO = "Volt"
        private const val APK_FILENAME = "Volt-update.apk"
    }

    /** Returns UpdateInfo when the latest GitHub release is newer than the installed build, else null. */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val release = gitHub.getLatestRelease(OWNER, REPO)
            val latest = release.tagName.removePrefix("v").trim()
            val current = BuildConfig.VERSION_NAME.trim()
            if (!isNewer(latest, current)) return@withContext null

            val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
                ?: return@withContext null

            UpdateInfo(
                versionName = latest,
                tagName = release.tagName,
                releaseNotes = release.body?.trim().orEmpty(),
                apkUrl = apk.browserDownloadUrl,
                apkSizeBytes = apk.size
            )
        } catch (e: Exception) {
            Timber.w(e, "Update check failed")
            null
        }
    }

    /** Compares dotted version strings: "1.10" > "1.9". */
    private fun isNewer(latest: String, current: String): Boolean {
        val l = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }
        val n = maxOf(l.size, c.size)
        for (i in 0 until n) {
            val a = l.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    /**
     * Enqueues the APK download via the system DownloadManager and registers a
     * receiver that launches the install prompt once the download completes.
     */
    fun downloadAndInstall(update: UpdateInfo) {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        // Clear any stale file from a previous attempt
        val target = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), APK_FILENAME)
        if (target.exists()) target.delete()

        val request = DownloadManager.Request(Uri.parse(update.apkUrl))
            .setTitle("Volt ${update.tagName}")
            .setDescription("Update wird heruntergeladen…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, APK_FILENAME)
            .setMimeType("application/vnd.android.package-archive")

        val downloadId = dm.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id != downloadId) return
                try {
                    context.unregisterReceiver(this)
                } catch (_: Exception) {}
                launchInstall(target)
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
    }

    private fun launchInstall(apk: File) {
        if (!apk.exists()) {
            Timber.w("APK missing after download: ${apk.absolutePath}")
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}
