/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.lyrics

import android.content.Context
import com.pokerlanka.mixora.constants.DeezerUserTokenKey
import com.pokerlanka.mixora.constants.EnableDeezerKey
import com.pokerlanka.mixora.utils.dataStore
import com.pokerlanka.mixora.utils.get
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.net.URLEncoder

object DeezerLyricsProvider : LyricsProvider {
    private const val TAG = "DeezerProvider"
    override val name = "Deezer"

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
        val enabled = context.dataStore[EnableDeezerKey] ?: false
        val token = context.dataStore[DeezerUserTokenKey].orEmpty()
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
        val token = context.dataStore[DeezerUserTokenKey].orEmpty()
        if (token.isBlank()) {
            throw IllegalStateException("Deezer user token/ARL not configured")
        }

        Timber.tag(TAG).d("Searching Deezer for $title by $artist")
        val cleanQuery = URLEncoder.encode("$artist $title", "UTF-8")
        val searchResponse = httpClient.get("https://api.deezer.com/search?q=$cleanQuery") {
            headers {
                append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }
        }

        if (searchResponse.status != HttpStatusCode.OK) {
            throw IllegalStateException("Deezer search failed with ${searchResponse.status}")
        }

        val searchJson = Json.parseToJsonElement(searchResponse.bodyAsText()).jsonObject
        val dataArray = searchJson["data"]?.jsonArray
        if (dataArray.isNullOrEmpty()) {
            throw IllegalStateException("No tracks found on Deezer")
        }

        val firstTrack = dataArray[0].jsonObject
        val trackId = firstTrack["id"]?.jsonPrimitive?.content ?: throw IllegalStateException("No track ID")

        // Try fetching lyrics for track ID
        val lyricsUrl = "https://api.lyrics.ovh/v1/${URLEncoder.encode(artist, "UTF-8")}/${URLEncoder.encode(title, "UTF-8")}"
        val lyricsResponse = httpClient.get(lyricsUrl) {
            headers {
                append("User-Agent", "Mozilla/5.0")
            }
        }

        if (lyricsResponse.status == HttpStatusCode.OK) {
            val lyricJson = Json.parseToJsonElement(lyricsResponse.bodyAsText()).jsonObject
            val lyrics = lyricJson["lyrics"]?.jsonPrimitive?.content
            if (!lyrics.isNullOrBlank()) {
                return@runCatching lyrics.trim()
            }
        }

        throw IllegalStateException("No lyrics content found on Deezer")
    }
}
