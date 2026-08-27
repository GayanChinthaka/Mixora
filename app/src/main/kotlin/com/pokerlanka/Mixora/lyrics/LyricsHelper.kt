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
import com.pokerlanka.mixora.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

private const val MAX_LYRICS_FETCH_MS = 25000L
private const val PER_PROVIDER_TIMEOUT_MS = 8000L
private const val PROVIDER_NONE = ""

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    val preferred =
        context.dataStore.data
            .map { preferences ->
                resolveLyricsProviders(preferences)
            }.distinctUntilChanged()

    private val _currentSearchingProvider = MutableStateFlow<String?>(null)
    val currentSearchingProvider: StateFlow<String?> = _currentSearchingProvider.asStateFlow()

    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    /** Track id -> the fetch already running for it, so duplicate callers share one round trip. */
    private val inFlight = ConcurrentHashMap<String, Deferred<LyricsWithProvider>>()

    /**
     * Shared scope for lyrics fetch operations. Uses SupervisorJob so individual
     * provider failures don't cancel sibling providers. This scope lives for the
     * lifetime of the LyricsHelper singleton (Hilt @Singleton) instead of creating
     * a new throwaway CoroutineScope per getAllLyrics call.
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
     * Resolves lyrics for one track, preferring the highest-priority provider that has them.
     *
     * Concurrent callers for the same track share one fetch: [MusicService] pre-fetches whenever
     * the lyrics pane is enabled and [Player] fetches again for display, and previously each ran
     * the whole provider fan-out on its own.
     */
    suspend fun getLyrics(mediaMetadata: MediaMetadata, skipCache: Boolean = false): LyricsWithProvider {
        currentLyricsJob?.cancel()

        if (!skipCache) {
            val cached = cache.get(mediaMetadata.id)?.firstOrNull()
            if (cached != null) {
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
        return deferred.await()
    }

    private suspend fun fetchLyrics(mediaMetadata: MediaMetadata): LyricsWithProvider {
        val orderedProviders = context.dataStore.data
            .map { preferences -> resolveLyricsProviders(preferences) }
            .first()

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }

        if (!isNetworkAvailable) {
            return LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
        }

        val result = withTimeoutOrNull(MAX_LYRICS_FETCH_MS) {
            val cleanedTitle = LyricsUtils.cleanTitleForSearch(mediaMetadata.title)
            val artists = mediaMetadata.artists.joinToString { it.name }
            val enabledProviders = orderedProviders.filter { it.isEnabled(context) }

            Timber.tag("LyricsHelper").d("Starting fetch for: $cleanedTitle by $artists")
            Timber.tag("LyricsHelper").d("Enabled providers in order: ${enabledProviders.joinToString { it.name }}")

            try {
                for (provider in enabledProviders) {
                    _currentSearchingProvider.value = provider.name
                    Timber.tag("LyricsHelper").d("Trying provider: ${provider.name}")
                    val providerResult = try {
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
                        Timber.tag("LyricsHelper").w("${provider.name} threw: ${e.message}")
                        null
                    }

                    if (providerResult != null && providerResult.isSuccess) {
                        Timber.tag("LyricsHelper").i("Got lyrics from ${provider.name}")
                        val filtered = LyricsUtils.filterLyricsCreditLines(providerResult.getOrNull()!!)
                        return@withTimeoutOrNull LyricsWithProvider(filtered, provider.name)
                    } else {
                        val errorMsg = providerResult?.exceptionOrNull()?.message ?: "timeout or not found"
                        Timber.tag("LyricsHelper").w("${provider.name} failed: $errorMsg")
                    }
                }
            } finally {
                _currentSearchingProvider.value = null
            }

            Timber.tag("LyricsHelper").w("No lyrics found after checking all providers")
            LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)
        } ?: LyricsWithProvider(LYRICS_NOT_FOUND, PROVIDER_NONE)

        if (result.lyrics != LYRICS_NOT_FOUND) {
            cache.put(mediaMetadata.id, listOf(LyricsResult(result.provider, result.lyrics)))
        }
        return result
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        duration: Int,
        album: String? = null,
        callback: (LyricsResult) -> Unit,
    ) {
        currentLyricsJob?.cancel()

        val cacheKey = "$songArtists-$songTitle".replace(" ", "")
        cache.get(cacheKey)?.let { results ->
            results.forEach { callback(it) }
            return
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }

        if (!isNetworkAvailable) return

        val allResult = mutableListOf<LyricsResult>()
        currentLyricsJob = fetchScope.launch {
            val cleanedTitle = LyricsUtils.cleanTitleForSearch(songTitle)
            val allProviders = context.dataStore.data
                .map { preferences -> resolveLyricsProviders(preferences) }
                .first()
            val enabledProviders = allProviders.filter { it.isEnabled(context) }

            val otherProviders = enabledProviders.filter { it.name != "LyricsPlus" }
            val lyricsPlusProvider = enabledProviders.find { it.name == "LyricsPlus" }

            val callbackMutex = Any()

            val otherJobs = otherProviders.map { provider ->
                launch {
                    try {
                        provider.getAllLyrics(context, mediaId, cleanedTitle, songArtists, duration, album) { lyrics ->
                            val filteredLyrics = LyricsUtils.filterLyricsCreditLines(lyrics)
                            val result = LyricsResult(provider.name, filteredLyrics)
                            synchronized(callbackMutex) {
                                allResult += result
                                callback(result)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
            otherJobs.forEach { it.join() }

            val otherLyricsCount = allResult.count { it.providerName != "LyricsPlus" }
            if (lyricsPlusProvider != null && otherLyricsCount <= 2) {
                launch {
                    try {
                        lyricsPlusProvider.getAllLyrics(context, mediaId, cleanedTitle, songArtists, duration, album) { lyrics ->
                            val filteredLyrics = LyricsUtils.filterLyricsCreditLines(lyrics)
                            val result = LyricsResult(lyricsPlusProvider.name, filteredLyrics)
                            synchronized(callbackMutex) {
                                allResult += result
                                callback(result)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }.join()
            }

            cache.put(cacheKey, allResult)
        }

        currentLyricsJob?.join()
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
