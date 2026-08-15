package com.pokerlanka.mixora.utils.cipher

import android.content.Context
import timber.log.Timber
import java.io.File

/**
 * Owns the player-config table at runtime: a bundled asset shipped in the APK, used as-is.
 * There is no remote overlay — the table is exactly what shipped with this build. Parsing/
 * validation is delegated to [PlayerConfigParser]; only validated payloads ever replace the
 * in-memory map.
 *
 * Read path is lock-free: lookups hit an immutable map behind a @Volatile reference.
 */
object PlayerConfigStore {
    private const val TAG = "Mixora_CipherConfig"
    private const val ASSET_NAME = "player_configs.json"

    @Volatile
    private var bundledConfigs: Map<String, FunctionNameExtractor.HardcodedPlayerConfig> = emptyMap()

    // No remote overlay exists, so the table never changes after initialize() — always 0.
    // Kept as a stable read-only property so CipherDeobfuscator's "rebuild WebView if the
    // config table changed" check still compiles and behaves correctly (never true, since
    // there is nothing left to change it).
    val configEpoch: Int = 0

    /**
     * Synchronous: loads the bundled asset. Guarantees configs exist before any lookup.
     */
    fun initialize(context: Context) {
        bundledConfigs = when (val result = parseSource("bundled asset") { loadBundledJson(context) }) {
            null -> emptyMap()
            else -> result
        }
        if (bundledConfigs.isEmpty()) {
            Timber.tag(TAG).e("Bundled $ASSET_NAME missing or invalid — config table starts empty")
        } else {
            Timber.tag(TAG).d("Loaded bundled configs (${bundledConfigs.size} hashes)")
        }
    }

    fun get(hash: String): FunctionNameExtractor.HardcodedPlayerConfig? {
        val configs = bundledConfigs
        if (configs.isEmpty()) {
            Timber.tag(TAG).w("Config table is empty (initialize not called or bundled asset broken)")
        }
        return configs[hash]
    }

    fun knownHashes(): Set<String> = bundledConfigs.keys

    /**
     * No-op: there is no remote source to fetch a fix from. Always returns false (the table
     * never changes). Kept so callers that self-heal on an unknown player hash don't need to
     * change — they just correctly never see a heal succeed anymore.
     */
    @Suppress("UNUSED_PARAMETER")
    fun forceRefresh(missingHash: String): Boolean = false

    /**
     * No-op: there is no remote source to re-fetch after a rejected stream. Always returns
     * false (the table never changes).
     */
    @Suppress("UNUSED_PARAMETER")
    fun refreshAfterStreamRejection(playerHash: String?): Boolean = false

    private fun loadBundledJson(context: Context): String? =
        context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }

    private fun parseSource(
        label: String,
        read: () -> String?,
    ): Map<String, FunctionNameExtractor.HardcodedPlayerConfig>? {
        val text = try {
            read() ?: return null
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Could not read $label: ${e.message}")
            return null
        }
        return when (val result = PlayerConfigParser.parse(text)) {
            is PlayerConfigParser.ParseResult.Failure -> {
                Timber.tag(TAG).w("Rejected $label: ${result.reason}")
                null
            }
            is PlayerConfigParser.ParseResult.Success -> {
                if (result.skippedEntries.isNotEmpty()) {
                    Timber.tag(TAG).w("$label: skipped invalid entries ${result.skippedEntries}")
                }
                result.configs
            }
        }
    }

    /**
     * True iff [stampMs] lies within [windowMs] of [now]. The in-range check (not a plain
     * `now - stamp < window`) matters: these are wall-clock stamps, and a backward clock
     * adjustment (NTP correction, manual change) makes the delta negative — a plain
     * less-than would then hold the window for the entire skew duration. Shared with
     * [PlayerJsFetcher] for its own cache-freshness check.
     */
    internal fun withinWindow(now: Long, stampMs: Long, windowMs: Long) =
        (now - stampMs) in 0 until windowMs

    /**
     * Temp-file + rename so a process death mid-write can't leave a truncated file. Shared
     * with [PlayerJsFetcher] for its own on-disk cache.
     */
    internal fun writeAtomic(file: File, content: String) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(content)
        if (!tmp.renameTo(file)) {
            // renameTo won't overwrite an existing target on some filesystems — retry after
            // deleting it (two cheap metadata ops, still atomic) before the last-resort direct
            // write, which is both non-atomic and a second full write of the content.
            file.delete()
            if (!tmp.renameTo(file)) {
                file.writeText(content)
                tmp.delete()
            }
        }
    }
}
