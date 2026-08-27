/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.lyrics

import android.content.Context
import com.pokerlanka.mixora.constants.EnableMusixmatchKey
import com.pokerlanka.mixora.constants.MusixmatchUserTokenKey
import com.pokerlanka.mixora.utils.dataStore
import com.pokerlanka.mixora.utils.get
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

object MusixmatchLyricsProvider : LyricsProvider {
    private const val TAG = "MusixmatchProvider"
    override val name = "Musixmatch"

    private val httpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
                connectTimeoutMillis = 8000
                socketTimeoutMillis = 10000
            }
            expectSuccess = false
        }
    }

    override fun isEnabled(context: Context): Boolean {
        val enabled = context.dataStore[EnableMusixmatchKey] ?: false
        val token = context.dataStore[MusixmatchUserTokenKey].orEmpty()
        return enabled && token.isNotBlank()
    }

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = runCatching {
        val userToken = context.dataStore[MusixmatchUserTokenKey].orEmpty()
        if (userToken.isBlank()) {
            throw IllegalStateException("Musixmatch user token not configured")
        }

        Timber.tag(TAG).d("Fetching lyrics for $title by $artist")
        val response = httpClient.get("https://apic-desktop.musixmatch.com/ws/1.1/macro.subtitles.get") {
            parameter("q_track", title)
            parameter("q_artist", artist)
            parameter("app_id", "web-desktop-app-v1.0")
            parameter("usertoken", userToken)
            if (duration > 0) {
                parameter("f_has_subtitle", 1)
            }
            headers {
                append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                append("Authority", "apic-desktop.musixmatch.com")
            }
        }

        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Musixmatch API returned status ${response.status}")
        }

        val jsonBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val message = jsonBody["message"]?.jsonObject ?: throw IllegalStateException("Invalid response format")
        val header = message["header"]?.jsonObject
        val statusCode = header?.get("status_code")?.jsonPrimitive?.content?.toIntOrNull() ?: 500

        if (statusCode != 200) {
            throw IllegalStateException("Musixmatch error code: $statusCode")
        }

        val macroCalls = message["body"]?.jsonObject?.get("macro_calls")?.jsonObject
            ?: throw IllegalStateException("No macro calls in response")

        // Try subtitles (synchronized) first
        val subtitlesList = macroCalls["track.subtitles.get"]?.jsonObject
            ?.get("message")?.jsonObject
            ?.get("body")?.jsonObject
            ?.get("subtitle_list")?.jsonArray

        if (!subtitlesList.isNullOrEmpty()) {
            val firstSub = subtitlesList[0].jsonObject["subtitle"]?.jsonObject
            val rawBody = firstSub?.get("subtitle_body")?.jsonPrimitive?.content
            if (!rawBody.isNullOrBlank()) {
                val parsedLrc = parseSubtitleBody(rawBody)
                if (parsedLrc.isNotBlank()) {
                    return@runCatching parsedLrc
                }
            }
        }

        // Fallback to plain lyrics
        val lyricsBody = macroCalls["track.lyrics.get"]?.jsonObject
            ?.get("message")?.jsonObject
            ?.get("body")?.jsonObject
            ?.get("lyrics")?.jsonObject
            ?.get("lyrics_body")?.jsonPrimitive?.content

        if (!lyricsBody.isNullOrBlank()) {
            return@runCatching lyricsBody.trim()
        }

        throw IllegalStateException("No lyrics found on Musixmatch")
    }

    private fun parseSubtitleBody(raw: String): String {
        return try {
            val jsonArray = Json.parseToJsonElement(raw).jsonArray
            val sb = StringBuilder()
            for (element in jsonArray) {
                val obj = element.jsonObject
                val timeObj = obj["time"]?.jsonObject ?: continue
                val mins = timeObj["minutes"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val secs = timeObj["seconds"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val hundredths = timeObj["hundredths"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                val text = obj["text"]?.jsonPrimitive?.content.orEmpty()
                sb.append(String.format(java.util.Locale.US, "[%02d:%02d.%02d]%s\n", mins, secs, hundredths, text))
            }
            sb.toString().trim()
        } catch (e: Exception) {
            raw.trim()
        }
    }

    /**
     * Attempts to automatically generate a fresh user token.
     */
    suspend fun autoGenerateToken(): Result<String> = runCatching {
        val response = httpClient.get("https://apic-desktop.musixmatch.com/ws/1.1/token.get") {
            parameter("app_id", "web-desktop-app-v1.0")
            headers {
                append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                append("Authority", "apic-desktop.musixmatch.com")
            }
        }
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val token = json["message"]?.jsonObject
            ?.get("body")?.jsonObject
            ?.get("user_token")?.jsonPrimitive?.content
        if (!token.isNullOrBlank()) {
            token
        } else {
            throw IllegalStateException("Failed to generate token (verification required)")
        }
    }
}
