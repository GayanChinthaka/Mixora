package com.pokerlanka.kugou

import com.pokerlanka.kugou.models.DownloadLyricsResponse
import com.pokerlanka.kugou.models.Keyword
import com.pokerlanka.kugou.models.SearchLyricsResponse
import com.pokerlanka.kugou.models.SearchSongResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.encodeURLParameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json
import java.lang.Integer.min
import kotlin.math.abs

@OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
private val client = HttpClient {
    expectSuccess = true

    install(ContentNegotiation) {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }
        json(json)
        json(json, ContentType.Text.Html)
        json(json, ContentType.Text.Plain)
    }

    install(ContentEncoding) {
        gzip()
        deflate()
    }
}

private const val PAGE_SIZE = 8
private const val HEAD_CUT_LIMIT = 30

/**
 * KuGou Lyrics Library
 * Modified from [ViMusic](https://github.com/vfsfitvnm/ViMusic)
 */
object KuGou {
    var useTraditionalChinese: Boolean = false

    private fun isTitleMatch(candidateTitle: String, targetTitle: String): Boolean {
        if (candidateTitle.isEmpty() || targetTitle.isEmpty()) return false
        if (candidateTitle.equals(targetTitle, ignoreCase = true)) return true
        if (candidateTitle.contains(targetTitle, ignoreCase = true) || targetTitle.contains(candidateTitle, ignoreCase = true)) return true
        val cleanCand = candidateTitle.replace(Regex("""[^a-zA-Z0-9\u4e00-\u9fa5]"""), "").lowercase()
        val cleanTarget = targetTitle.replace(Regex("""[^a-zA-Z0-9\u4e00-\u9fa5]"""), "").lowercase()
        if (cleanCand.isNotEmpty() && cleanCand == cleanTarget) return true
        if (cleanCand.isNotEmpty() && (cleanCand.contains(cleanTarget) || cleanTarget.contains(cleanCand))) return true
        return false
    }

    private suspend fun tryDownloadCandidate(candidate: SearchLyricsResponse.Candidate): String? {
        val resp = runCatching { downloadLyrics(candidate.id, candidate.accesskey) }.getOrNull()
        if (resp != null && resp.status == 200 && resp.content.isNotBlank()) {
            val decoded = runCatching {
                Base64.Default.decode(resp.content).decodeToString().normalize()
            }.getOrNull()
            if (!decoded.isNullOrBlank()) {
                return decoded
            }
        }
        return null
    }

    suspend fun getLyrics(title: String, artist: String, duration: Int, album: String? = null): Result<String> =
        runCatching {
            val keyword = generateKeyword(title, artist, album)
            val normalizedTargetTitle = normalizeTitle(keyword.title).trim()

            // Strategy 1: Search songs by title + artist and check candidate matches
            val searchSongResp = runCatching { searchSongs(keyword) }.getOrNull()
            if (searchSongResp != null) {
                for (song in searchSongResp.data.info) {
                    val songTitle = normalizeTitle(song.songname).trim()
                    if (!isTitleMatch(songTitle, normalizedTargetTitle)) continue

                    if (duration == -1 || abs(song.duration - duration) <= DURATION_TOLERANCE) {
                        val hashCandidates = runCatching { searchLyricsByHash(song.hash).candidates }.getOrDefault(emptyList())
                        for (candidate in hashCandidates) {
                            val lyrics = tryDownloadCandidate(candidate)
                            if (lyrics != null) return@runCatching lyrics
                        }
                    }
                }
            }

            // Strategy 2: Search lyrics directly by keyword and match candidate song name
            val keywordCandidates = runCatching { searchLyricsByKeyword(keyword, duration).candidates }.getOrDefault(emptyList())
            for (candidate in keywordCandidates) {
                val candidateTitle = normalizeTitle(candidate.song).trim()
                if (isTitleMatch(candidateTitle, normalizedTargetTitle)) {
                    val lyrics = tryDownloadCandidate(candidate)
                    if (lyrics != null) return@runCatching lyrics
                }
            }

            throw IllegalStateException("No lyrics candidate")
        }

    suspend fun getLyricsCandidate(
        keyword: Keyword, duration: Int
    ): SearchLyricsResponse.Candidate? {
        val normalizedTargetTitle = normalizeTitle(keyword.title).trim()
        val searchSongResp = runCatching { searchSongs(keyword) }.getOrNull()
        if (searchSongResp != null) {
            for (song in searchSongResp.data.info) {
                val songTitle = normalizeTitle(song.songname).trim()
                if (!isTitleMatch(songTitle, normalizedTargetTitle)) continue
                if (duration == -1 || abs(song.duration - duration) <= DURATION_TOLERANCE) {
                    val candidate = searchLyricsByHash(song.hash).candidates.firstOrNull()
                    if (candidate != null) return candidate
                }
            }
        }
        val keywordCandidates = runCatching { searchLyricsByKeyword(keyword, duration).candidates }.getOrDefault(emptyList())
        return keywordCandidates.firstOrNull { isTitleMatch(normalizeTitle(it.song).trim(), normalizedTargetTitle) }
    }

    suspend fun searchSongs(keyword: Keyword) =
        client.get("https://mobileservice.kugou.com/api/v3/search/song") {
            parameter("version", 9108)
            parameter("plat", 0)
            parameter("pagesize", PAGE_SIZE)
            parameter("showtype", 0)
            val searchQuery = "${keyword.title} - ${keyword.artist}"
            url.encodedParameters.append(
                "keyword",
                searchQuery.encodeURLParameter(spaceToPlus = false)
            )
        }.body<SearchSongResponse>()

    private suspend fun searchLyricsByKeyword(keyword: Keyword, duration: Int) =
        client.get("https://lyrics.kugou.com/search") {
            parameter("ver", 1)
            parameter("man", "yes")
            parameter("client", "pc")
            parameter(
                "duration", duration.takeIf { it != -1 }?.times(1000)
            )
            val searchQuery = "${keyword.title} - ${keyword.artist}"
            url.encodedParameters.append(
                "keyword",
                searchQuery.encodeURLParameter(spaceToPlus = false)
            )
        }.body<SearchLyricsResponse>()

    private suspend fun searchLyricsByHash(hash: String) =
        client.get("https://lyrics.kugou.com/search") {
            parameter("ver", 1)
            parameter("man", "yes")
            parameter("client", "pc")
            parameter("hash", hash)
        }.body<SearchLyricsResponse>()

    private suspend fun downloadLyrics(id: Long, accessKey: String) =
        client.get("https://lyrics.kugou.com/download") {
            parameter("fmt", "lrc")
            parameter("charset", "utf8")
            parameter("client", "pc")
            parameter("ver", 1)
            parameter("id", id)
            parameter("accesskey", accessKey)
        }.body<DownloadLyricsResponse>()

    private fun normalizeTitle(title: String) =
        title.replace("\\(.*\\)".toRegex(), "").replace("（.*）".toRegex(), "")
            .replace("「.*」".toRegex(), "").replace("『.*』".toRegex(), "")
            .replace("<.*>".toRegex(), "").replace("《.*》".toRegex(), "")
            .replace("〈.*〉".toRegex(), "").replace("＜.*＞".toRegex(), "")

    private fun normalizeArtist(artist: String) =
        artist.replace(", ", "、").replace(" & ", "、").replace(".", "").replace("和", "、")
            .replace("\\(.*\\)".toRegex(), "").replace("（.*）".toRegex(), "")

    fun generateKeyword(title: String, artist: String, album: String? = null) =
        Keyword(normalizeTitle(title), normalizeArtist(artist), album)

    private fun String.normalize(): String =
        lines().filter { line -> line.matches(ACCEPTED_REGEX) }
            .let { lines ->
                // Remove useless information such as singer, writer, composer, guitar, etc.
                var headCutLine = 0
                for (i in min(HEAD_CUT_LIMIT, lines.lastIndex) downTo 0) {
                    if (lines[i].matches(BANNED_REGEX)) {
                        headCutLine = i + 1
                        break
                    }
                }
                val filteredLines = lines.drop(headCutLine)

                var tailCutLine = 0
                for (i in min(lines.size - HEAD_CUT_LIMIT, lines.lastIndex) downTo 0) {
                    if (lines[lines.lastIndex - i].matches(BANNED_REGEX)) {
                        tailCutLine = i + 1
                        break
                    }
                }
                val finalLines = filteredLines.dropLast(tailCutLine)

                return@let finalLines.joinToString("\n")
            }

    @Suppress("RegExpRedundantEscape")
    private val ACCEPTED_REGEX = "\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\].*".toRegex()
    private val BANNED_REGEX = ".+].+[:：].+".toRegex()

    private const val DURATION_TOLERANCE = 8
}
