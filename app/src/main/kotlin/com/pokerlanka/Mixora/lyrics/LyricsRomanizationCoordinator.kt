/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.lyrics

import android.content.Context
import com.pokerlanka.mixora.ai.AiLyricsRomanizer
import com.pokerlanka.mixora.ai.AiServiceConfig
import com.pokerlanka.mixora.constants.AiApiKeyKey
import com.pokerlanka.mixora.constants.AiCustomEndpointKey
import com.pokerlanka.mixora.constants.AiCustomModelKey
import com.pokerlanka.mixora.constants.AiProvider
import com.pokerlanka.mixora.constants.AiProviderKey
import com.pokerlanka.mixora.constants.AiRomanizationEnabledKey
import com.pokerlanka.mixora.constants.AiSelectedModelKey
import com.pokerlanka.mixora.db.MusicDatabase
import com.pokerlanka.mixora.db.entities.RomanizedLyricsEntity
import com.pokerlanka.mixora.extensions.toEnum
import com.pokerlanka.mixora.utils.dataStore
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Resolves romanized text for a set of parsed lyric lines: cache first, then the AI provider.
 *
 * Romanization is AI-only — there is no on-device transliteration path any more — so every exit
 * here is "leave [LyricsEntry.romanizedTextFlow] as it was". The flow starts `null` and the lyrics
 * UI already falls back to the original text for a null value, which makes a disabled provider, an
 * offline device, and a failed request all degrade to the same harmless outcome.
 *
 * Shared by the player's lyrics sheet and the experimental lyrics view so the cache lookup, style
 * key, and failure handling exist exactly once.
 */
class LyricsRomanizationCoordinator(
    private val context: Context,
    private val database: MusicDatabase,
) {
    private val romanizer = AiLyricsRomanizer()

    /**
     * @param onRunningChange invoked with `true` only when a network request is actually started,
     *   so a cache hit never flashes a progress indicator.
     */
    suspend fun romanize(
        lyrics: String,
        entries: List<LyricsEntry>,
        onRunningChange: (Boolean) -> Unit = {},
    ) {
        val targets = entries.filter { it != LyricsEntry.HEAD_LYRICS_ENTRY && it.text.isNotBlank() }
        if (targets.isEmpty()) {
            Timber.tag(LogTag).d("skip: no romanizable lines in %d entries", entries.size)
            return
        }

        val prefs = context.dataStore.data.first()
        if (prefs[AiRomanizationEnabledKey] != true) {
            Timber.tag(LogTag).d("skip: AI romanization is off (Settings > Lyrics > Lyrics romanization)")
            return
        }

        val provider = prefs[AiProviderKey].toEnum(AiProvider.NONE)
        val config =
            AiServiceConfig(
                provider = provider,
                apiKey = prefs[AiApiKeyKey].orEmpty(),
                customEndpoint = prefs[AiCustomEndpointKey].orEmpty(),
                model =
                    (
                        if (provider == AiProvider.CUSTOM) {
                            prefs[AiCustomModelKey]
                        } else {
                            prefs[AiSelectedModelKey]
                        }
                    ).orEmpty(),
            )
        if (!config.canCallApi) {
            Timber.tag(LogTag).w(
                "skip: provider not usable (provider=%s, hasKey=%b, model='%s')",
                provider,
                config.apiKey.isNotBlank(),
                config.model,
            )
            return
        }

        val style = RomanizationStyleVersion
        val sourceLines = targets.map { it.text }
        val hash = withContext(Dispatchers.Default) { sha256(lyrics) }

        readCache(hash, style, sourceLines.size)?.let { cached ->
            Timber.tag(LogTag).d("cache hit: %d lines (style=%s)", cached.size, style)
            apply(targets, cached)
            return
        }

        Timber.tag(LogTag).d(
            "requesting: %d lines, provider=%s, model=%s, style=%s",
            sourceLines.size,
            provider,
            config.model,
            style,
        )
        onRunningChange(true)
        try {
            val romanized =
                romanizer.romanize(
                    config = config,
                    lines = sourceLines,
                )
            Timber.tag(LogTag).d(
                "done: %d of %d lines romanized; first='%s'",
                romanized.count { it != null },
                romanized.size,
                romanized.firstOrNull { it != null }.orEmpty(),
            )
            apply(targets, romanized)
            writeCache(hash, style, romanized, config.model)
        } catch (cancellation: CancellationException) {
            // The lyrics sheet left the composition or the track changed. Not an error, and not
            // cached, so the next play starts clean.
            Timber.tag(LogTag).d("cancelled mid-request")
            throw cancellation
        } catch (error: Exception) {
            // Deliberately not cached: a transient failure should retry on the next play rather
            // than pin an empty result for this song forever.
            Timber.tag(LogTag).w(error, "AI romanization unavailable")
        } finally {
            onRunningChange(false)
        }
    }

    private suspend fun readCache(
        hash: String,
        style: String,
        expectedLines: Int,
    ): List<String?>? {
        val cached = runCatching { database.romanizedLyrics(hash, style) }.getOrNull() ?: return null
        if (cached.lineCount != expectedLines) return null
        // A row written against a differently-segmented parse would silently shift lyrics against
        // their timestamps, so the stored line count has to agree before it is trusted.
        val lines = cached.romanized.split('\n')
        if (lines.size != expectedLines) return null
        return lines.map { it.takeIf(String::isNotEmpty) }
    }

    private suspend fun writeCache(
        hash: String,
        style: String,
        romanized: List<String?>,
        model: String,
    ) {
        runCatching {
            database.upsert(
                RomanizedLyricsEntity(
                    lyricsHash = hash,
                    style = style,
                    // An empty entry records "this line needed no romanization", which survives the
                    // newline round-trip where a null would not.
                    romanized = romanized.joinToString("\n") { it.orEmpty() },
                    lineCount = romanized.size,
                    model = model,
                ),
            )
        }.onFailure { Timber.tag(LogTag).w(it, "Could not cache romanization") }
    }

    private fun apply(
        targets: List<LyricsEntry>,
        romanized: List<String?>,
    ) {
        if (romanized.size != targets.size) {
            Timber.tag(LogTag).w("Romanization size mismatch: ${romanized.size} for ${targets.size}")
            return
        }
        targets.forEachIndexed { index, entry ->
            entry.romanizedTextFlow.value = romanized[index]
        }
    }

    private companion object {
        const val LogTag = "LyricsRomanization"

        /**
         * Part of the cache primary key, so stored rows produced by an older prompt are a miss
         * rather than stale output. Bump whenever the prompt changes what it emits.
         *
         * v1 initial; v2 added explicit Tamil/Sinhala/Telugu/Kannada/Malayalam/Bengali rules;
         * v3 dropped the pinyin tone-mark option, which previously split this into two variants.
         */
        const val RomanizationStyleVersion = "v3"

        fun sha256(value: String): String =
            MessageDigest
                .getInstance("SHA-256")
                .digest(value.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}
