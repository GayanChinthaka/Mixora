/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.lyrics

import android.content.Context
import android.util.LruCache
import com.pokerlanka.mixora.constants.LyricsProviderOrderKey
import com.pokerlanka.mixora.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.pokerlanka.mixora.models.MediaMetadata
import com.pokerlanka.mixora.utils.NetworkConnectivityObserver
import com.pokerlanka.mixora.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Hard backstop for one track's whole fan-out. Providers are staggered rather than run strictly one
 * after another, so the worst case is
 * `(providerCount - 1) * PROVIDER_STAGGER_MS + PER_PROVIDER_TIMEOUT_MS`, which stays comfortably
 * inside this budget. Previously the per-provider timeouts could add up to roughly twice this value,
 * and the providers at the bottom of the user's order were never reached at all.
 */
private const val MAX_LYRICS_FETCH_MS = 25000L
private const val PER_PROVIDER_TIMEOUT_MS = 8000L

/**
 * How long a provider is given to answer before the next one down the order is also started. Short
 * enough that a dead provider does not stall the queue, long enough that the common case (the top
 * provider answers) still costs a single request.
 */
private const val PROVIDER_STAGGER_MS = 1500L
private const val SINGLE_PROVIDER_TIMEOUT_MS = 15000L
private const val PROVIDER_NONE = ""

@Singleton
class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    /**
     * The fetches currently running, keyed by track id, with the provider each one is waiting on
     * (null until the first provider is reached).
     *
     * This used to be a pair of global `isFetching` / `currentSearchingProvider` flags. With two
     * tracks in flight - which happens on every skip, and whenever a refetch overlaps the automatic
     * fetch - whichever finished first cleared the flag for both, and the provider label could name
     * a provider that belonged to a different song.
     */
    private val _activeFetches = MutableStateFlow<Map<String, String?>>(emptyMap())
    val activeFetches: StateFlow<Map<String, String?>> = _activeFetches.asStateFlow()

    /** Marks [mediaId] as fetching so the UI can show a spinner before the fetch actually starts. */
    fun startFetching(mediaId: String, providerName: String? = null) {
        _activeFetches.update { it + (mediaId to providerName) }
    }

    private fun setSearchingProvider(mediaId: String, providerName: String?) {
        _activeFetches.update { current ->
            if (current.containsKey(mediaId)) current + (mediaId to providerName) else current
        }
    }

    /**
     * Clears the fetching marker for [mediaId]. Every fetch entry point already does this in a
     * finally block; callers that call [startFetching] themselves use this as a safety net for
     * failures that happen before the fetch takes over.
     */
    fun clearFetching(mediaId: String) {
        _activeFetches.update { it - mediaId }
    }

    private fun finishFetching(mediaId: String) = clearFetching(mediaId)

    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)

    /** Track id -> the fetch already running for it, so duplicate callers share one round trip. */
    private val inFlight = ConcurrentHashMap<String, Deferred<LyricsWithProvider>>()

    /**
     * Shared scope for lyrics fetch operations. Uses SupervisorJob so individual
     * provider failures don't cancel sibling providers. This scope lives for the
     * lifetime of the LyricsHelper singleton (Hilt @Singleton) instead of creating
     * a new throwaway CoroutineScope per fetch.
     */
    private val fetchScope = CoroutineScope(SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    fun clearCache(mediaId: String? = null) {
        if (mediaId != null) {
            cache.remove(mediaId)
            inFlight.remove(mediaId)?.cancel()
        } else {
            cache.evictAll()
            inFlight.values.forEach { it.cancel() }
            inFlight.clear()
        }
    }

    /**
     * Drops the fetches belonging to tracks that are no longer being displayed.
     *
     * Fetches run on the singleton [fetchScope], so a track change cancels the caller's coroutine
     * but not the work: skipping quickly through a queue used to leave one full provider fan-out
     * running per skipped song, all competing for the network with the track actually playing.
     */
    private fun cancelFetchesOtherThan(mediaId: String) {
        inFlight.keys.filter { it != mediaId }.forEach { staleId ->
            inFlight.remove(staleId)?.cancel()
        }
    }

    /**
     * Resolves lyrics for one track, preferring the highest-priority provider that has them.
     *
     * Concurrent callers for the same track share one fetch, so the player UI re-subscribing (for
     * example toggling between the artwork and the lyrics pane) does not start a second fan-out.
     */
    suspend fun getLyrics(mediaMetadata: MediaMetadata, skipCache: Boolean = false): LyricsWithProvider {
        cancelFetchesOtherThan(mediaMetadata.id)

        if (!skipCache) {
            val cached = cache.get(mediaMetadata.id)?.firstOrNull()
            if (cached != null) {
                finishFetching(mediaMetadata.id)
                return LyricsWithProvider(cached.lyrics, cached.providerName)
            }
        } else {
            cache.remove(mediaMetadata.id)
            inFlight.remove(mediaMetadata.id)?.cancel()
        }

        val deferred =
            inFlight.computeIfAbsent(mediaMetadata.id) { id ->
                fetchScope.async { fetchLyrics(mediaMetadata) }.also { started ->
                    started.invokeOnCompletion { inFlight.remove(id, started) }
                }
            }

        return try {
            deferred.await()
        } catch (e: CancellationException) {
            // The shared fetch was dropped (track change, refetch). Only treat that as our own
            // cancellation when this coroutine really was cancelled too.
            coroutineContext.ensureActive()
            LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
        }
    }

    private suspend fun fetchLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider {
        startFetching(mediaMetadata.id)
        try {
            val orderedProviders = context.dataStore.data
                .map { preferences -> resolveLyricsProviders(preferences) }
                .first()

            val isNetworkAvailable = try {
                networkConnectivity.isCurrentlyConnected()
            } catch (e: Exception) {
                true
            }

            // Offline is not the same as "this song has no lyrics", so this result is deliberately
            // never cached - neither here nor by the callers that persist to Room.
            if (!isNetworkAvailable) {
                return LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
            }

            val result = withTimeoutOrNull(MAX_LYRICS_FETCH_MS) {
                val cleanedTitle = LyricsUtils.cleanTitleForSearch(mediaMetadata.title)
                val artists = mediaMetadata.artists.joinToString { it.name }
                val enabledProviders = orderedProviders.filter { it.isEnabled(context) }

                Timber.tag(TAG).d("Starting fetch for: $cleanedTitle by $artists")
                Timber.tag(TAG).d("Enabled providers in order: ${enabledProviders.joinToString { it.name }}")

                if (enabledProviders.isEmpty()) {
                    return@withTimeoutOrNull LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
                }

                coroutineScope {
                    val attempts = enabledProviders.mapIndexed { index, provider ->
                        provider to async {
                            // Staggered start: the provider at position N only reaches the network
                            // once everything above it has had a fair chance to answer.
                            if (index > 0) delay(index * PROVIDER_STAGGER_MS)
                            Timber.tag(TAG).d("Trying provider: ${provider.name}")
                            try {
                                withTimeoutOrNull(PER_PROVIDER_TIMEOUT_MS) {
                                    provider.getLyrics(
                                        context,
                                        mediaMetadata.id,
                                        cleanedTitle,
                                        artists,
                                        mediaMetadata.duration,
                                        mediaMetadata.album?.title,
                                    )
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                Timber.tag(TAG).w("${provider.name} threw: ${e.message}")
                                null
                            }
                        }
                    }

                    try {
                        // Results are consumed in the user's priority order even though the
                        // requests overlap, so a faster low-priority provider can never outrank the
                        // preferred one.
                        for ((provider, attempt) in attempts) {
                            setSearchingProvider(mediaMetadata.id, provider.name)
                            val providerResult = attempt.await()
                            val lyrics = providerResult?.getOrNull()
                            if (providerResult != null && providerResult.isSuccess && !lyrics.isNullOrBlank()) {
                                Timber.tag(TAG).i("Got lyrics from ${provider.name}")
                                return@coroutineScope LyricsWithProvider(
                                    LyricsUtils.filterLyricsCreditLines(lyrics),
                                    provider.name,
                                )
                            }
                            val errorMsg = providerResult?.exceptionOrNull()?.message ?: "timeout or not found"
                            Timber.tag(TAG).w("${provider.name} failed: $errorMsg")
                        }

                        Timber.tag(TAG).w("No lyrics found after checking all providers")
                        LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
                    } finally {
                        // Stop whatever is still in flight before coroutineScope waits on it.
                        attempts.forEach { (_, attempt) -> attempt.cancel() }
                        setSearchingProvider(mediaMetadata.id, null)
                    }
                }
            } ?: LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)

            if (result.lyrics != LYRICS_NOT_FOUND) {
                cache.put(mediaMetadata.id, listOf(LyricsResult(result.provider, result.lyrics)))
            }
            return result
        } finally {
            finishFetching(mediaMetadata.id)
        }
    }

    suspend fun getLyricsFromProvider(mediaMetadata: MediaMetadata, providerName: String): LyricsWithProvider {
        cancelFetchesOtherThan(mediaMetadata.id)
        clearCache(mediaMetadata.id)

        // Everything below runs inside this try so that the early returns cannot leave the track
        // marked as fetching. They previously sat above the try/finally, which is why refetching
        // while offline left the spinner and the "Searching lyrics on ..." label up for good.
        try {
            val provider = LyricsProviderRegistry.getProviderByName(providerName)
                ?: return LyricsWithProvider(LYRICS_NOT_FOUND, providerName)

            val isNetworkAvailable = try {
                networkConnectivity.isCurrentlyConnected()
            } catch (e: Exception) {
                true
            }

            if (!isNetworkAvailable) {
                return LyricsWithProvider(LYRICS_NOT_FOUND, providerName)
            }

            startFetching(mediaMetadata.id, provider.name)

            val cleanedTitle = LyricsUtils.cleanTitleForSearch(mediaMetadata.title)
            val artists = mediaMetadata.artists.joinToString { it.name }
            Timber.tag(TAG).d("Fetching from specific provider: ${provider.name} for $cleanedTitle by $artists")

            val providerResult = try {
                withTimeoutOrNull(SINGLE_PROVIDER_TIMEOUT_MS) {
                    provider.getLyrics(
                        context,
                        mediaMetadata.id,
                        cleanedTitle,
                        artists,
                        mediaMetadata.duration,
                        mediaMetadata.album?.title,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag(TAG).w("${provider.name} threw: ${e.message}")
                null
            }

            if (providerResult != null && providerResult.isSuccess) {
                val rawLyrics = providerResult.getOrNull()
                if (!rawLyrics.isNullOrBlank()) {
                    Timber.tag(TAG).i("Got lyrics from ${provider.name}")
                    val filtered = LyricsUtils.filterLyricsCreditLines(rawLyrics)
                    cache.put(mediaMetadata.id, listOf(LyricsResult(provider.name, filtered)))
                    return LyricsWithProvider(filtered, provider.name)
                }
            }
            Timber.tag(TAG).w("No lyrics found from ${provider.name}")
            return LyricsWithProvider(LYRICS_NOT_FOUND, provider.name)
        } finally {
            finishFetching(mediaMetadata.id)
        }
    }

    private fun resolveLyricsProviders(preferences: androidx.datastore.preferences.core.Preferences): List<LyricsProvider> {
        val providerOrder = preferences[LyricsProviderOrderKey].orEmpty()
        if (providerOrder.isNotBlank()) {
            return LyricsProviderRegistry.getOrderedProviders(providerOrder)
        }

        return LyricsProviderRegistry.getDefaultProviderOrder()
            .mapNotNull { LyricsProviderRegistry.getProviderByName(it) }
    }

    companion object {
        private const val TAG = "LyricsHelper"

        // Lyrics are a few KB of text each; three entries meant re-fetching after skipping
        // back two tracks. The Room LyricsEntity table is still the durable cache.
        private const val MAX_CACHE_SIZE = 30
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)

data class LyricsWithProvider(
    val lyrics: String,
    val provider: String,
)
