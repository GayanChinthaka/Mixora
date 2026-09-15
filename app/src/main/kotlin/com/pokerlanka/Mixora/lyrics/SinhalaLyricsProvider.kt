/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.lyrics

import android.content.Context
import com.pokerlanka.mixora.constants.EnableSinhalaLyricsKey
import com.pokerlanka.mixora.utils.dataStore
import com.pokerlanka.mixora.utils.get
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

object SinhalaLyricsProvider : LyricsProvider {
    private const val TAG = "SinhalaLyricsProvider"
    override val name = "SinhalaLyrics"

    private val httpClient by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
                connectTimeoutMillis = 6000
                socketTimeoutMillis = 10000
            }
            expectSuccess = false
        }
    }

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableSinhalaLyricsKey] ?: true

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val isSinhalaContent = LyricsUtils.hasSinhalaScript(title) ||
                LyricsUtils.hasSinhalaScript(artist) ||
                title.contains("sinhala", ignoreCase = true) ||
                artist.contains("sinhala", ignoreCase = true)

            val titleCandidates = LyricsUtils.extractSearchCandidateTitles(title, artist)
            Timber.tag(TAG).d("Searching Sinhala lyrics for titles: $titleCandidates, artist: '$artist' (isSinhala=$isSinhalaContent)")

            // 1. Try direct slug matching on lyrics-lk.com (fast HTTP GET)
            val directResult = tryDirectSlugLookup(titleCandidates, artist)
            if (directResult != null) {
                return@withContext Result.success(directResult)
            }

            // 2. Try DuckDuckGo search fallback ONLY if track has Sinhala content
            if (isSinhalaContent) {
                val searchResult = trySearchFallback(titleCandidates, artist)
                if (searchResult != null) {
                    return@withContext Result.success(searchResult)
                }
            }

            Result.failure(IllegalStateException("No Sinhala lyrics found"))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.tag(TAG).w("Error fetching Sinhala lyrics: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun tryDirectSlugLookup(
        titleCandidates: List<String>,
        artist: String,
    ): String? {
        val artistSlugs = generateArtistSlugs(artist)
        for (rawTitle in titleCandidates) {
            val titleSlugs = generateTitleSlugs(rawTitle)
            for (aSlug in artistSlugs) {
                for (tSlug in titleSlugs) {
                    val url = "https://lyrics-lk.com/song/$aSlug/$tSlug"
                    val lyrics = fetchAndExtractFromLyricsLk(url)
                    if (!lyrics.isNullOrBlank()) {
                        Timber.tag(TAG).i("Found direct lyrics on lyrics-lk: $url")
                        return lyrics
                    }
                }
            }
        }
        return null
    }

    private suspend fun trySearchFallback(
        titleCandidates: List<String>,
        artist: String,
    ): String? {
        val queryBase = titleCandidates.firstOrNull() ?: return null
        val queries = listOf(
            "$queryBase $artist lyrics",
            "$queryBase sinhala lyrics",
            "$queryBase lyrics",
        ).distinct()

        for (query in queries) {
            val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val ddgUrl = "https://html.duckduckgo.com/html/?q=$encodedQuery"

            val response = try {
                httpClient.get(ddgUrl) {
                    headers {
                        append("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    }
                }
            } catch (e: Exception) {
                continue
            }

            if (response.status != HttpStatusCode.OK) continue
            val html = response.bodyAsText()

            // Find song links in search results
            val linkRegex = Regex("""href=["'](?:https?:)?//duckduckgo\.com/l/\?uddg=([^&"']+)""")
            for (match in linkRegex.findAll(html)) {
                val encodedTarget = match.groupValues[1]
                val targetUrl = try {
                    URLDecoder.decode(encodedTarget, StandardCharsets.UTF_8.toString())
                } catch (e: Exception) {
                    continue
                }

                if (targetUrl.contains("lyrics-lk.com/song/")) {
                    val lyrics = fetchAndExtractFromLyricsLk(targetUrl)
                    if (!lyrics.isNullOrBlank()) {
                        Timber.tag(TAG).i("Found lyrics via DuckDuckGo: $targetUrl")
                        return lyrics
                    }
                }
            }
        }
        return null
    }

    private suspend fun fetchAndExtractFromLyricsLk(url: String): String? {
        try {
            val response = httpClient.get(url) {
                headers {
                    append("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                }
            }
            if (response.status != HttpStatusCode.OK) return null
            val html = response.bodyAsText()

            // <pre class="...">lyrics</pre>
            val preRegex = Regex("""<pre[^>]*>([\s\S]*?)</pre>""", RegexOption.IGNORE_CASE)
            val match = preRegex.find(html) ?: return null
            val rawLyrics = match.groupValues[1]

            return cleanExtractedLyrics(rawLyrics)
        } catch (e: Exception) {
            return null
        }
    }

    private fun cleanExtractedLyrics(raw: String): String? {
        val text = raw
            .replace("<br\\s*/?>".toRegex(RegexOption.IGNORE_CASE), "\n")
            .replace("<[^>]+>".toRegex(), "")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .trim()

        val lines = text.lines().map { it.trim() }
        val filtered = lines.filterNot { line ->
            line.startsWith("Chord") || line.startsWith("Artist:") || line.startsWith("Key:")
        }

        val result = filtered.joinToString("\n").trim()
        return if (result.lines().size >= 3) result else null
    }

    private fun slugify(input: String): String {
        val converted = if (LyricsUtils.hasSinhalaScript(input)) {
            LyricsUtils.transliterateSinhalaToLatin(input)
        } else {
            input
        }
        return converted.lowercase(Locale.ROOT)
            .replace(Regex("""[^a-z0-9]+"""), "-")
            .trim('-')
    }

    private fun generateArtistSlugs(artist: String): List<String> {
        val slugs = mutableListOf<String>()
        val primaryArtist = artist.split(Regex("""[,&/|]|\sfeat\.|\sft\.""", RegexOption.IGNORE_CASE)).firstOrNull()?.trim() ?: artist
        val fullSlug = slugify(primaryArtist)
        if (fullSlug.isNotBlank()) slugs.add(fullSlug)

        val parts = fullSlug.split("-").filter { it.isNotBlank() }
        if (parts.size > 1 && parts[0].length >= 3) {
            slugs.add(parts[0])
            if (parts.size >= 3 && parts[0].length <= 2 && parts[1].length <= 2) {
                slugs.add(parts.drop(2).joinToString("-"))
            }
        }
        return slugs.distinct()
    }

    private fun generateTitleSlugs(title: String): List<String> {
        val slugs = mutableListOf<String>()
        val baseSlug = slugify(title)
        if (baseSlug.isNotBlank()) slugs.add(baseSlug)

        val parts = baseSlug.split("-").filter { it.isNotBlank() }
        if (parts.size > 3) {
            slugs.add(parts.take(3).joinToString("-"))
        }
        return slugs.distinct()
    }
}
