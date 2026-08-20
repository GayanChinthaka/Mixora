/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
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
