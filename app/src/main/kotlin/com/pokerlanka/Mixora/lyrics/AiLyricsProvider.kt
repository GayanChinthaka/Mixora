/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.lyrics

import android.content.Context
import com.pokerlanka.mixora.ai.AiServiceConfig
import com.pokerlanka.mixora.ai.AiTextService
import com.pokerlanka.mixora.constants.AiApiKeyKey
import com.pokerlanka.mixora.constants.AiCustomEndpointKey
import com.pokerlanka.mixora.constants.AiCustomModelKey
import com.pokerlanka.mixora.constants.AiProvider
import com.pokerlanka.mixora.constants.AiProviderKey
import com.pokerlanka.mixora.constants.AiSelectedModelKey
import com.pokerlanka.mixora.constants.EnableAiLyricsKey
import com.pokerlanka.mixora.utils.dataStore
import com.pokerlanka.mixora.utils.get
import com.pokerlanka.mixora.extensions.toEnum
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

object AiLyricsProvider : LyricsProvider {
    private const val TAG = "AiLyricsProvider"
    override val name = "AiLyrics"

    override fun isEnabled(context: Context): Boolean {
        val enabled = context.dataStore[EnableAiLyricsKey] ?: false
        val provider = context.dataStore[AiProviderKey].toEnum(AiProvider.NONE)
        val apiKey = context.dataStore[AiApiKeyKey].orEmpty()
        return enabled && provider != AiProvider.NONE && apiKey.isNotBlank()
    }

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val provider = context.dataStore[AiProviderKey].toEnum(AiProvider.NONE)
            val apiKey = context.dataStore[AiApiKeyKey].orEmpty()
            val customEndpoint = context.dataStore[AiCustomEndpointKey].orEmpty()
            val selectedModel = context.dataStore[AiSelectedModelKey].orEmpty()
            val customModel = context.dataStore[AiCustomModelKey].orEmpty()
            val model = if (provider == AiProvider.CUSTOM) customModel else selectedModel

            val config = AiServiceConfig(
                provider = provider,
                apiKey = apiKey,
                customEndpoint = customEndpoint,
                model = model,
            )

            if (!config.canCallApi || model.isBlank()) {
                return@withContext Result.failure(IllegalStateException("AI service is not configured"))
            }

            Timber.tag(TAG).d("Requesting lyrics from AI (${config.provider} / $model) for '$title' by '$artist'")

            val systemPrompt = """
                You are a music lyrics assistant. Provide the accurate and complete lyrics for the requested song.
                Rules:
                1. Output ONLY the song lyrics. Do NOT include greetings, intro, outro, explanations, markdown commentary, or credits.
                2. If the song is in Sinhala or another regional language, output the lyrics in its original script (or Latin script if commonly sung/written that way).
                3. Format the lyrics with natural line breaks and verse spacing.
                4. If you do not know the lyrics with high confidence, reply with exactly: NOT_FOUND
            """.trimIndent()

            val userPrompt = """
                Song Title: $title
                Artist: $artist
            """.trimIndent()

            val response = AiTextService.complete(
                config = config,
                systemPrompt = systemPrompt,
                userPrompt = userPrompt,
                temperature = 0.1,
                maxTokens = 4096,
            )

            val cleaned = cleanAiLyrics(response)
            if (cleaned != null) {
                Timber.tag(TAG).i("AI lyrics retrieved successfully for '$title'")
                Result.success(cleaned)
            } else {
                Timber.tag(TAG).w("AI replied with NOT_FOUND or invalid lyrics for '$title'")
                Result.failure(IllegalStateException("AI could not find lyrics"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Timber.tag(TAG).e(e, "Error during AI lyrics fetch")
            Result.failure(e)
        }
    }

    private fun cleanAiLyrics(response: String): String? {
        var text = response.trim()

        if (text.equals("NOT_FOUND", ignoreCase = true) || text.contains("NOT_FOUND", ignoreCase = true) && text.lines().size <= 2) {
            return null
        }

        // Strip enclosing markdown code fences if model wrapped in ```
        if (text.startsWith("```")) {
            text = text.substringAfter("\n").substringBeforeLast("```").trim()
        }

        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.size < 3) {
            return null
        }

        return text
    }
}
