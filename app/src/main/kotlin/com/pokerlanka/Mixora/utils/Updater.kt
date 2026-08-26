/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pokerlanka.mixora.BuildConfig
import com.pokerlanka.mixora.MainActivity
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.constants.CheckForUpdatesKey
import com.pokerlanka.mixora.constants.LastNotifiedUpdateTimeKey
import com.pokerlanka.mixora.constants.LastNotifiedUpdateVersionKey
import com.pokerlanka.mixora.constants.LastUpdateCheckTimeKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class GitCommit(
    val sha: String,
    val message: String,
    val author: String,
    val date: String,
    val url: String,
    val authorAvatarUrl: String? = null,
)

data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String?,
    val publishedAt: String,
    val htmlUrl: String,
    val downloadUrl: String? = null,
)

object Updater {
    private const val GITHUB_REPO_PRIMARY = "GayanChinthaka/Mixora"
    private const val GITHUB_REPO_SECONDARY = "MixoraGroup/Mixora"
    const val GITHUB_RELEASES_URL = "https://github.com/$GITHUB_REPO_PRIMARY/releases"
    private const val UPDATE_NOTIFICATION_ID = 2026

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val semVerRegex =
        Regex("""(?i)\bv?(\d+)\.(\d+)\.(\d+)(?:-([0-9A-Za-z.-]+))?(?:\+[0-9A-Za-z.-]+)?\b""")

    private data class SemVer(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val preRelease: List<String> = emptyList(),
    ) : Comparable<SemVer> {
        override fun compareTo(other: SemVer): Int {
            if (major != other.major) return major.compareTo(other.major)
            if (minor != other.minor) return minor.compareTo(other.minor)
            if (patch != other.patch) return patch.compareTo(other.patch)

            val thisIsStable = preRelease.isEmpty()
            val otherIsStable = other.preRelease.isEmpty()
            if (thisIsStable && !otherIsStable) return 1
            if (!thisIsStable && otherIsStable) return -1

            val maxIndex = minOf(preRelease.size, other.preRelease.size)
            for (i in 0 until maxIndex) {
                val c = preRelease[i].compareTo(other.preRelease[i])
                if (c != 0) return c
            }
            return preRelease.size.compareTo(other.preRelease.size)
        }
    }

    private fun parseSemVerOrNull(text: String): SemVer? {
        val match = semVerRegex.find(text) ?: return null
        val major = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
        val minor = match.groupValues.getOrNull(2)?.toIntOrNull() ?: return null
        val patch = match.groupValues.getOrNull(3)?.toIntOrNull() ?: return null
        val preReleaseText = match.groupValues.getOrNull(4)?.takeIf { it.isNotBlank() }
        val preRelease = preReleaseText?.split('.')?.filter { it.isNotBlank() } ?: emptyList()
        return SemVer(major, minor, patch, preRelease)
    }

    fun isUpdateAvailable(latestVersion: String, currentVersion: String): Boolean {
        val latestSemVer = parseSemVerOrNull(latestVersion)
        val currentSemVer = parseSemVerOrNull(currentVersion)
        return if (latestSemVer != null && currentSemVer != null) {
            latestSemVer > currentSemVer
        } else {
            latestVersion.trim() != currentVersion.trim() && latestVersion.isNotBlank()
        }
    }

    suspend fun getLatestRelease(): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        val repos = listOf(GITHUB_REPO_PRIMARY, GITHUB_REPO_SECONDARY)
        var lastException: Exception? = null

        for (repo in repos) {
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/repos/$repo/releases/latest")
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Mixora-App")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body.string()
                        if (!body.isNullOrBlank()) {
                            val json = JSONObject(body)
                            val tagName = json.optString("tag_name")
                            val name = json.optString("name", tagName)
                            val releaseBody = json.optString("body")
                            val publishedAt = json.optString("published_at")
                            val htmlUrl = json.optString("html_url", GITHUB_RELEASES_URL)

                            var apkDownloadUrl: String? = null
                            val assets = json.optJSONArray("assets")
                            if (assets != null) {
                                for (i in 0 until assets.length()) {
                                    val asset = assets.getJSONObject(i)
                                    val assetName = asset.optString("name")
                                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                                        apkDownloadUrl = asset.optString("browser_download_url")
                                        break
                                    }
                                }
                            }

                            return@withContext Result.success(
                                ReleaseInfo(
                                    tagName = tagName,
                                    name = name,
                                    body = releaseBody,
                                    publishedAt = publishedAt,
                                    htmlUrl = htmlUrl,
                                    downloadUrl = apkDownloadUrl ?: htmlUrl
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        Result.failure(lastException ?: Exception("Could not fetch latest release"))
    }

    suspend fun checkAutoUpdate(
        context: Context,
        force: Boolean = false,
        onNewVersionFound: ((String) -> Unit)? = null
    ): Result<ReleaseInfo?> = withContext(Dispatchers.IO) {
        try {
            val settings = context.dataStore.data.first()
            val isAutoCheckEnabled = settings[CheckForUpdatesKey] ?: true
            if (!isAutoCheckEnabled && !force) {
                Timber.d("Updater: Auto-check is disabled in settings")
                return@withContext Result.success(null)
            }

            val lastCheckTime = settings[LastUpdateCheckTimeKey] ?: 0L
            val minInterval = TimeUnit.MINUTES.toMillis(15)
            val now = System.currentTimeMillis()

            if (!force && (now - lastCheckTime < minInterval)) {
                Timber.d("Updater: Skipping check, checked recently (${(now - lastCheckTime) / 1000}s ago)")
                return@withContext Result.success(null)
            }

            Timber.i("Updater: Checking for updates from GitHub...")
            val result = getLatestRelease()
            context.safeDataStoreEdit { pref ->
                pref[LastUpdateCheckTimeKey] = now
            }

            result.fold(
                onSuccess = { release ->
                    Timber.i("Updater: Found latest release tag: ${release.tagName}, current: ${BuildConfig.VERSION_NAME}")
                    val isAvailable = isUpdateAvailable(release.tagName, BuildConfig.VERSION_NAME)
                    if (isAvailable) {
                        Timber.i("Updater: New update is available: ${release.tagName}")
                        onNewVersionFound?.invoke(release.tagName)
                        
                        val lastNotifiedVersion = settings[LastNotifiedUpdateVersionKey]
                        val lastNotifiedTime = settings[LastNotifiedUpdateTimeKey] ?: 0L
                        val notifyCooldown = TimeUnit.HOURS.toMillis(12)
                        val shouldNotify = force || (lastNotifiedVersion != release.tagName) || (now - lastNotifiedTime > notifyCooldown)

                        if (shouldNotify) {
                            Timber.i("Updater: Triggering update notification for ${release.tagName}")
                            showUpdateNotification(context, release)
                            context.safeDataStoreEdit { pref ->
                                pref[LastNotifiedUpdateVersionKey] = release.tagName
                                pref[LastNotifiedUpdateTimeKey] = now
                            }
                        } else {
                            Timber.d("Updater: Notification was recently shown for this release")
                        }
                    } else {
                        Timber.i("Updater: App is up to date (${BuildConfig.VERSION_NAME})")
                    }
                    Result.success(if (isAvailable) release else null)
                },
                onFailure = { error ->
                    Timber.w(error, "Updater: Failed to check for updates automatically")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Updater: Error in checkAutoUpdate")
            Result.failure(e)
        }
    }

    fun showUpdateNotification(context: Context, release: ReleaseInfo) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            val channel = NotificationChannel(
                "updates",
                context.getString(R.string.update_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.update_channel_desc)
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Timber.w("Updater: POST_NOTIFICATIONS permission not granted; cannot display notification")
                return
            }
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_UPDATES
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val downloadUrl = release.downloadUrl ?: release.htmlUrl
        val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val downloadPendingIntent = PendingIntent.getActivity(
            context,
            1,
            downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.update_available_title)
        val text = "Mixora ${release.tagName} is available. Tap to view and update."

        val notification = NotificationCompat.Builder(context, "updates")
            .setSmallIcon(R.drawable.small_icon)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.download,
                "Download",
                downloadPendingIntent
            )
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(UPDATE_NOTIFICATION_ID, notification)
            Timber.i("Updater: Update notification posted successfully for ${release.tagName}")
        }.onFailure {
            Timber.w(it, "Updater: Failed to post update notification")
        }
    }

    suspend fun getRecentCommits(limit: Int = 15): Result<List<GitCommit>> = withContext(Dispatchers.IO) {
        val repos = listOf(GITHUB_REPO_PRIMARY, GITHUB_REPO_SECONDARY)
        var lastException: Exception? = null

        for (repo in repos) {
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/repos/$repo/commits?per_page=$limit")
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Mixora-App")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body.string()
                        if (!body.isNullOrBlank()) {
                            val jsonArray = JSONArray(body)
                            val commits = mutableListOf<GitCommit>()

                            for (i in 0 until jsonArray.length()) {
                                val item = jsonArray.getJSONObject(i)
                                val sha = item.optString("sha").take(7)
                                val htmlUrl = item.optString("html_url")
                                val commitObj = item.optJSONObject("commit")
                                val message = commitObj?.optString("message")?.lines()?.firstOrNull() ?: ""
                                val authorObj = commitObj?.optJSONObject("author")
                                val authorName = authorObj?.optString("name") ?: "Unknown"
                                val date = authorObj?.optString("date") ?: ""

                                val authorUser = item.optJSONObject("author")
                                val avatarUrl = authorUser?.optString("avatar_url")

                                commits.add(
                                    GitCommit(
                                        sha = sha,
                                        message = message,
                                        author = authorName,
                                        date = formatDate(date),
                                        url = htmlUrl,
                                        authorAvatarUrl = avatarUrl
                                    )
                                )
                            }
                            return@withContext Result.success(commits)
                        }
                    }
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        Result.failure(lastException ?: Exception("Could not fetch commits"))
    }

    private fun formatDate(rawDate: String): String {
        return try {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = isoFormat.parse(rawDate) ?: return rawDate
            val displayFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            displayFormat.format(date)
        } catch (_: Exception) {
            rawDate.take(10)
        }
    }
}
