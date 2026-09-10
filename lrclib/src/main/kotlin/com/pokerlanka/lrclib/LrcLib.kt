package com.pokerlanka.lrclib

import com.pokerlanka.lrclib.models.Track
import com.pokerlanka.lrclib.models.bestMatchingFor
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.math.abs

object LrcLib {
    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }

            defaultRequest {
                url("https://lrclib.net")
            }

            expectSuccess = true
        }
    }

    // Patterns to clean from title
    private val titleCleanupPatterns = listOf(
        Regex("""\s*\(.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\]""", RegexOption.IGNORE_CASE),
        Regex("""\s*【.*?】"""),
        Regex("""\s*\|.*$"""),
        Regex("""\s*-\s*(official|video|audio|lyrics|lyric|visualizer).*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE),
    )

    // Patterns to extract primary artist
    private val artistSeparators = listOf(" & ", " and ", ", ", " x ", " X ", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with ")

    private fun cleanTitle(title: String, artist: String? = null): String {
        var cleaned = title.trim()
        for (pattern in titleCleanupPatterns) {
            cleaned = cleaned.replace(pattern, "")
        }
        cleaned = cleaned.trim()

        if (!artist.isNullOrBlank()) {
            val primaryArtist = cleanArtist(artist)
            val prefix = "$primaryArtist - "
            if (cleaned.startsWith(prefix, ignoreCase = true)) {
                cleaned = cleaned.substring(prefix.length).trim()
            }
        } else if (cleaned.contains(" - ")) {
            val parts = cleaned.split(" - ", limit = 2)
            if (parts.size == 2 && parts[1].trim().isNotBlank()) {
                cleaned = parts[1].trim()
            }
        }
        return cleaned
    }

    private fun cleanArtist(artist: String): String {
        var cleaned = artist.trim()
        // Get primary artist (first one before any separator)
        for (separator in artistSeparators) {
            if (cleaned.contains(separator, ignoreCase = true)) {
                cleaned = cleaned.split(separator, ignoreCase = true, limit = 2)[0]
                break
            }
        }
        return cleaned.trim()
    }

    private suspend fun queryLyricsWithParams(
        trackName: String? = null,
        artistName: String? = null,
        albumName: String? = null,
        query: String? = null,
    ): List<Track> = runCatching {
        client.get("/api/search") {
            if (query != null) parameter("q", query)
            if (trackName != null) parameter("track_name", trackName)
            if (artistName != null) parameter("artist_name", artistName)
            if (albumName != null) parameter("album_name", albumName)
        }.body<List<Track>>()
    }.getOrDefault(emptyList())

    private suspend fun queryLyrics(
        artist: String,
        title: String,
    ): List<Track> {
        val cleanedArtist = cleanArtist(artist)
        val cleanedTitle = cleanTitle(title, artist)
        
        // Strategy 1: Search with cleaned track_name and artist_name directly
        var results = queryLyricsWithParams(
            trackName = cleanedTitle,
            artistName = cleanedArtist
        ).filter { it.syncedLyrics != null || it.plainLyrics != null }
        if (results.isNotEmpty()) return results

        // Strategy 2: Use q parameter with combined artist + title
        results = queryLyricsWithParams(
            query = "$cleanedArtist $cleanedTitle"
        ).filter { it.syncedLyrics != null || it.plainLyrics != null }
        if (results.isNotEmpty()) return results

        // Strategy 3: Try original title with artist if different from cleaned title
        if (cleanedTitle != title.trim()) {
            results = queryLyricsWithParams(
                trackName = title.trim(),
                artistName = cleanedArtist
            ).filter { it.syncedLyrics != null || it.plainLyrics != null }
            if (results.isNotEmpty()) return results
        }

        // Strategy 4: Use q parameter with just title
        results = queryLyricsWithParams(
            query = cleanedTitle
        ).filter { it.syncedLyrics != null || it.plainLyrics != null }
        if (results.isNotEmpty()) return results

        // Strategy 5: Fallback to searching trackName only (last resort when artist is unknown)
        results = queryLyricsWithParams(
            trackName = cleanedTitle
        ).filter { it.syncedLyrics != null || it.plainLyrics != null }
        return results
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ) = runCatching {
        val cleanedArtist = cleanArtist(artist)
        val cleanedTitle = cleanTitle(title, artist)
        val tracks = queryLyrics(artist, title)

        // Match on duration *and* name. queryLyrics widens the search by dropping the artist when
        // the strict query comes back empty, so a duration-only pick regularly landed on a cover or
        // an unrelated same-titled song.
        val res = tracks.bestMatchingFor(duration, cleanedTitle, cleanedArtist)?.let { track ->
            track.syncedLyrics ?: track.plainLyrics
        }?.let(LrcLib::Lyrics)

        if (res != null) {
            return@runCatching res.text
        } else {
            throw IllegalStateException("Lyrics unavailable")
        }
    }

    @JvmInline
    value class Lyrics(
        val text: String,
    ) {
        val sentences
            get() =
                runCatching {
                    buildMap {
                        put(0L, "")
                        text.trim().lines().filter { it.length >= 10 }.forEach {
                            put(
                                it[8].digitToInt() * 10L +
                                    it[7].digitToInt() * 100 +
                                    it[5].digitToInt() * 1000 +
                                    it[4].digitToInt() * 10000 +
                                    it[2].digitToInt() * 60 * 1000 +
                                    it[1].digitToInt() * 600 * 1000,
                                it.substring(10),
                            )
                        }
                    }
                }.getOrNull()
    }
}


