/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Turns a list of lyric lines into their Latin-script equivalents via the configured AI provider.
 *
 * The caller passes lines already split by the lyrics parser and gets back a list of the same
 * size, so index alignment with timestamps is structural rather than something the model has to
 * be trusted with. A `null` at some index means "no romanization for this line" — either it needs
 * none or the request failed — and the UI shows the original text there.
 */
class AiLyricsRomanizer {
    /**
     * @return one entry per input line; `null` where the line needs no romanization.
     * @throws AiServiceException if every batch failed, so the caller can avoid caching nothing.
     */
    suspend fun romanize(
        config: AiServiceConfig,
        lines: List<String>,
    ): List<String?> {
        if (lines.isEmpty()) return emptyList()

        // Choruses repeat, so the same line often appears 4-6 times in one song. Sending each
        // occurrence would multiply cost and let the model drift between repeats; collapsing to
        // distinct values makes rule 6 of the prompt hold by construction.
        val distinct = lines.filter { needsRomanization(it) }.distinct()
        if (distinct.isEmpty()) {
            Timber.tag(LogTag).d(
                "no non-Latin lines among %d; nothing to send (sample='%s')",
                lines.size,
                lines.firstOrNull().orEmpty().take(40),
            )
            return List(lines.size) { null }
        }
        Timber.tag(LogTag).d(
            "%d lines -> %d distinct non-Latin (sample='%s')",
            lines.size,
            distinct.size,
            distinct.first().take(40),
        )

        val results = HashMap<String, String>(distinct.size)
        var failures = 0
        val batches = distinct.chunkedByBudget()
        batches.forEach { batch ->
            val romanized = requestWithRetry(config, batch)
            if (romanized == null) {
                failures++
            } else {
                batch.forEachIndexed { index, source ->
                    romanized[index].takeIf { it != source }?.let { results[source] = it }
                }
            }
        }
        if (failures == batches.size) {
            throw AiServiceException("AI romanization failed for every batch")
        }

        return lines.map { line -> results[line] }
    }

    private suspend fun requestWithRetry(
        config: AiServiceConfig,
        batch: List<String>,
    ): List<String>? {
        var attempt = 0
        while (attempt < MaxAttempts) {
            try {
                return AiTextService.romanizeLines(
                    config = config,
                    lines = batch,
                )
            } catch (cancellation: CancellationException) {
                // Cancellation is not a failure: the caller navigated away or the lyrics changed.
                // Swallowing it here broke structured concurrency and, worse, surfaced as
                // "AI romanization failed for every batch" — blaming the provider for our own
                // lifecycle. It must always propagate.
                throw cancellation
            } catch (error: Exception) {
                attempt++
                val message = error.message.orEmpty()
                // A shape or count violation is deterministic for this input: the same prompt
                // will fail the same way, so only transport-ish failures are worth another turn.
                val retryable = RetryableStatuses.any { message.contains("($it)") }
                if (attempt >= MaxAttempts || !retryable) {
                    Timber.tag(LogTag).w(error, "Romanization batch of ${batch.size} lines failed")
                    return null
                }
                delay(RetryBaseDelayMs * (1L shl (attempt - 1)))
            }
        }
        return null
    }

    /**
     * Splits into request-sized batches. Mirrors the character budget the translator used: large
     * enough that a normal song is one request, small enough to stay clear of the output cap.
     */
    private fun List<String>.chunkedByBudget(): List<List<String>> {
        val chunks = ArrayList<List<String>>()
        val current = ArrayList<String>()
        var currentChars = 0
        forEach { line ->
            if (current.isNotEmpty() &&
                (current.size >= MaxItemsPerBatch || currentChars + line.length > MaxCharsPerBatch)
            ) {
                chunks.add(current.toList())
                current.clear()
                currentChars = 0
            }
            current.add(line)
            currentChars += line.length
        }
        if (current.isNotEmpty()) chunks.add(current.toList())
        return chunks
    }

    private companion object {
        const val MaxItemsPerBatch = 80
        const val MaxCharsPerBatch = 6000
        const val MaxAttempts = 3
        const val RetryBaseDelayMs = 1500L
        const val LogTag = "AiRomanizer"

        /** Rate limiting and upstream hiccups; a 4xx about the request itself will not fix itself. */
        val RetryableStatuses = listOf(429, 500, 502, 503, 504)
    }
}

/**
 * True when a line contains anything outside Latin/common ranges and so might need converting.
 *
 * This replaces the previous per-language detection entirely: the model identifies the script
 * itself, so the only question worth answering on-device is "is there anything here to send",
 * which also keeps fully-Latin lyrics from costing a single token.
 */
internal fun needsRomanization(line: String): Boolean =
    line.any { char ->
        val code = char.code
        // Everything past Latin Extended-B / IPA / spacing modifiers is a candidate script:
        // Greek, Cyrillic, Devanagari, Thai, Arabic, Hangul, CJK. Combining diacriticals
        // (U+0300..U+036F) are skipped because decomposed Latin text such as "é" lives
        // there and is already Latin.
        code > 0x02FF && code !in 0x0300..0x036F && !char.isWhitespace()
    }
