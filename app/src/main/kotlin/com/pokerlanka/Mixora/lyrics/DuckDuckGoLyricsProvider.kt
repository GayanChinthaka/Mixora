/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.lyrics

import android.content.Context
import com.pokerlanka.mixora.constants.EnableDuckDuckGoKey
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
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

object DuckDuckGoLyricsProvider : LyricsProvider {
    private const val TAG = "DuckDuckGoLyricsProvider"
    override val name = "DuckDuckGo"

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
        context.dataStore[EnableDuckDuckGoKey] ?: true

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = getLyricsWithSource(context, id, title, artist, duration, album).map { it.lyrics }

    override suspend fun getLyricsWithSource(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<LyricsWithSource> = withContext(Dispatchers.IO) {
        try {
            val isSinhalaContent = LyricsUtils.hasSinhalaScript(title) ||
                LyricsUtils.hasSinhalaScript(artist) ||
                title.contains("sinhala", ignoreCase = true) ||
                artist.contains("sinhala", ignoreCase = true)

            val titleCandidates = LyricsUtils.extractSearchCandidateTitles(title, artist)
            Timber.tag(TAG).d("Searching web lyrics for titles: $titleCandidates, artist: '$artist' (isSinhala=$isSinhalaContent)")

            // 1. If Sinhala, try direct slug matching on lyrics-lk.com (fast and highly accurate)
            if (isSinhalaContent) {
                val directResult = tryDirectSlugLookup(titleCandidates, artist)
                if (directResult != null) {
                    return@withContext Result.success(LyricsWithSource(directResult, "DuckDuckGo (lyrics-lk.com)"))
                }
            }

            // 2. Try DuckDuckGo search across top lyrics sites
            val ddgResult = tryDuckDuckGoSearch(titleCandidates, artist, isSinhalaContent)
            if (ddgResult != null) {
                return@withContext Result.success(ddgResult)
            }

            // 3. Fallback for Sinhala content: Search content.lyrics-lk.com directly
            if (isSinhalaContent) {
                val contentLkResult = tryContentLyricsLkSearch(titleCandidates, artist)
                if (contentLkResult != null) {
                    return@withContext Result.success(LyricsWithSource(contentLkResult, "DuckDuckGo (lyrics-lk.com)"))
                }

                // 4. Fallback for Sinhala content: Search sinhalasongbook.com
                val songbookResult = trySinhalaSongbookSearch(titleCandidates, artist)
                if (songbookResult != null) {
                    return@withContext Result.success(LyricsWithSource(songbookResult, "DuckDuckGo (sinhalasongbook.com)"))
                }
            }

            Result.failure(IllegalStateException("No lyrics found via DuckDuckGo search"))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.tag(TAG).w("Error fetching DuckDuckGo lyrics: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun tryDuckDuckGoSearch(
        titleCandidates: List<String>,
        artist: String,
        isSinhala: Boolean,
    ): LyricsWithSource? {
        val queryBase = titleCandidates.firstOrNull() ?: return null
        val queries = buildList {
            if (isSinhala) {
                add("$queryBase $artist lyrics")
                add("$queryBase sinhala lyrics")
            } else {
                add("$queryBase $artist lyrics")
            }
        }.distinct()

        for (query in queries) {
            val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
            val ddgUrl = "https://html.duckduckgo.com/html/?q=$encodedQuery"

            val html = try {
                val response = httpClient.get(ddgUrl) {
                    headers {
                        append("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    }
                }
                if (response.status == HttpStatusCode.OK) response.bodyAsText() else null
            } catch (e: Exception) {
                null
            } ?: continue

            // Extract links from DuckDuckGo search results
            val linkRegex = Regex("""href=["'](?:https?:)?//duckduckgo\.com/l/\?uddg=([^&"']+)""")
            for (match in linkRegex.findAll(html)) {
                val encodedTarget = match.groupValues[1]
                val targetUrl = try {
                    URLDecoder.decode(encodedTarget, StandardCharsets.UTF_8.toString())
                } catch (e: Exception) {
                    continue
                }

                val host = extractHost(targetUrl) ?: continue

                // Check supported lyrics targets
                if (targetUrl.contains("lyrics-lk.com/song/")) {
                    val lyrics = fetchAndExtractFromLyricsLk(targetUrl)
                    if (!lyrics.isNullOrBlank()) {
                        return LyricsWithSource(lyrics, "DuckDuckGo ($host)")
                    }
                } else if (targetUrl.contains("sinhalasongbook.com/")) {
                    val lyrics = fetchAndExtractFromSinhalaSongbook(targetUrl)
                    if (!lyrics.isNullOrBlank()) {
                        return LyricsWithSource(lyrics, "DuckDuckGo ($host)")
                    }
                } else if (targetUrl.contains("genius.com/")) {
                    val lyrics = fetchAndExtractFromGenius(targetUrl)
                    if (!lyrics.isNullOrBlank()) {
                        return LyricsWithSource(lyrics, "DuckDuckGo ($host)")
                    }
                } else if (targetUrl.contains("azlyrics.com/lyrics/")) {
                    val lyrics = fetchAndExtractFromAZLyrics(targetUrl)
                    if (!lyrics.isNullOrBlank()) {
                        return LyricsWithSource(lyrics, "DuckDuckGo ($host)")
                    }
                }
            }
        }
        return null
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

    private suspend fun tryContentLyricsLkSearch(
        titleCandidates: List<String>,
        artist: String,
    ): String? {
        val baseTitle = titleCandidates.firstOrNull() ?: return null
        val query = "$baseTitle $artist".trim()
        val searchUrl = "https://content.lyrics-lk.com/?s=" + URLEncoder.encode(query, StandardCharsets.UTF_8.toString())

        try {
            val response = httpClient.get(searchUrl) {
                headers {
                    append("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                }
            }
            if (response.status != HttpStatusCode.OK) return null
            val html = response.bodyAsText()

            // Find song links in search results: <a href="https://content.lyrics-lk.com/song/.../">
            val linkRegex = Regex("""href=["'](https://content\.lyrics-lk\.com/song/[^"']+)["']""")
            for (match in linkRegex.findAll(html)) {
                val songUrl = match.groupValues[1]
                val lyrics = fetchAndExtractFromContentLyricsLk(songUrl)
                if (!lyrics.isNullOrBlank()) {
                    Timber.tag(TAG).i("Found lyrics on content.lyrics-lk: $songUrl")
                    return lyrics
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Error during content.lyrics-lk search")
        }
        return null
    }

    private suspend fun trySinhalaSongbookSearch(
        titleCandidates: List<String>,
        artist: String,
    ): String? {
        val baseTitle = titleCandidates.firstOrNull() ?: return null
        val query = "$baseTitle $artist".trim()
        val searchUrl = "https://sinhalasongbook.com/?s=" + URLEncoder.encode(query, StandardCharsets.UTF_8.toString())

        try {
            val response = httpClient.get(searchUrl) {
                headers {
                    append("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                }
            }
            if (response.status != HttpStatusCode.OK) return null
            val html = response.bodyAsText()

            val linkRegex = Regex("""<h2 class="entry-title"><a[^>]+href=["'](https://(?:www\.)?sinhalasongbook\.com/[^"']+)["']""")
            for (match in linkRegex.findAll(html)) {
                val songUrl = match.groupValues[1]
                if (songUrl.contains("/category/") || songUrl.contains("/tag/") || songUrl.contains("/product/")) continue
                val lyrics = fetchAndExtractFromSinhalaSongbook(songUrl)
                if (!lyrics.isNullOrBlank()) {
                    Timber.tag(TAG).i("Found lyrics on sinhalasongbook: $songUrl")
                    return lyrics
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Error during sinhalasongbook search")
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

            // Check if page contains Sinhala script pre tag first: <pre ... data-lyrics-si="true">...</pre>
            val siPreRegex = Regex("""<pre[^>]*data-lyrics-si=["']true["'][^>]*>([\s\S]*?)</pre>""", RegexOption.IGNORE_CASE)
            val siMatch = siPreRegex.find(html)
            if (siMatch != null) {
                val rawLyrics = siMatch.groupValues[1]
                val cleaned = cleanExtractedLyrics(rawLyrics)
                if (cleaned != null) return cleaned
            }

            // Fallback to standard <pre> tag
            val preRegex = Regex("""<pre[^>]*>([\s\S]*?)</pre>""", RegexOption.IGNORE_CASE)
            val match = preRegex.find(html) ?: return null
            return cleanExtractedLyrics(match.groupValues[1])
        } catch (e: Exception) {
            return null
        }
    }

    private suspend fun fetchAndExtractFromContentLyricsLk(url: String): String? {
        try {
            val response = httpClient.get(url) {
                headers {
                    append("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                }
            }
            if (response.status != HttpStatusCode.OK) return null
            val html = response.bodyAsText()

            // Look for <div class="lyric-content ...">...</div>
            val lyricContentRegex = Regex("""<div class=["']lyric-content[^"']*["']>([\s\S]*?)</div>\s*<!-- \.entry-content -->""", RegexOption.IGNORE_CASE)
            val match = lyricContentRegex.find(html)
            if (match != null) {
                val cleaned = cleanExtractedLyrics(match.groupValues[1])
                if (cleaned != null) return cleaned
            }
            return null
        } catch (e: Exception) {
            return null
        }
    }

    private suspend fun fetchAndExtractFromSinhalaSongbook(url: String): String? {
        try {
            val response = httpClient.get(url) {
                headers {
                    append("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                }
            }
            if (response.status != HttpStatusCode.OK) return null
            val html = response.bodyAsText()

            // <pre style="font-weight: bold;">...</pre>
            val preRegex = Regex("""<pre[^>]*>([\s\S]*?)</pre>""", RegexOption.IGNORE_CASE)
            val match = preRegex.find(html) ?: return null
            return cleanExtractedLyrics(match.groupValues[1])
        } catch (e: Exception) {
            return null
        }
    }

    private suspend fun fetchAndExtractFromGenius(url: String): String? {
        try {
            val response = httpClient.get(url) {
                headers {
                    append("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                }
            }
            if (response.status != HttpStatusCode.OK) return null
            val html = response.bodyAsText()

            val containerRegex = Regex("""<div[^>]*data-lyrics-container=["']true["'][^>]*>([\s\S]*?)</div>""", RegexOption.IGNORE_CASE)
            val matches = containerRegex.findAll(html).toList()
            if (matches.isNotEmpty()) {
                val combined = matches.joinToString("\n") { it.groupValues[1] }
                return cleanExtractedLyrics(combined)
            }
            return null
        } catch (e: Exception) {
            return null
        }
    }

    private suspend fun fetchAndExtractFromAZLyrics(url: String): String? {
        try {
            val response = httpClient.get(url) {
                headers {
                    append("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                }
            }
            if (response.status != HttpStatusCode.OK) return null
            val html = response.bodyAsText()

            val marker = "<!-- Usage of azlyrics.com content by any third-party lyrics provider is prohibited by our licensing agreement. Sorry about that. -->"
            if (html.contains(marker)) {
                val afterMarker = html.substringAfter(marker)
                val divContent = afterMarker.substringAfter("<div>").substringBefore("</div>")
                return cleanExtractedLyrics(divContent)
            }
            return null
        } catch (e: Exception) {
            return null
        }
    }

    private fun cleanExtractedLyrics(raw: String): String? {
        val text = raw
            .replace("<br\\s*/?>".toRegex(RegexOption.IGNORE_CASE), "\n")
            .replace("<p[^>]*>".toRegex(RegexOption.IGNORE_CASE), "")
            .replace("</p>".toRegex(RegexOption.IGNORE_CASE), "\n")
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
            line.startsWith("Chord", ignoreCase = true) ||
                line.startsWith("Artist:", ignoreCase = true) ||
                line.startsWith("Key:", ignoreCase = true) ||
                line.startsWith("Beat:", ignoreCase = true) ||
                line.startsWith("INTRO", ignoreCase = true) ||
                line.startsWith("CHORUS", ignoreCase = true) ||
                line.startsWith("INTER", ignoreCase = true) ||
                line.startsWith("VERSE", ignoreCase = true) ||
                line.matches(Regex("""^\|[\sA-G#bm0-9\-/|∆]+$"""))
        }

        val result = filtered.joinToString("\n").trim()
        return if (result.lines().size >= 3) result else null
    }

    private fun extractHost(url: String): String? {
        return try {
            val uri = URI(url)
            val host = uri.host ?: return null
            if (host.startsWith("www.")) host.removePrefix("www.") else host
        } catch (e: Exception) {
            null
        }
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
