/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokerlanka.innertube.YouTube
import com.pokerlanka.innertube.models.AlbumItem
import com.pokerlanka.innertube.models.ArtistItem
import com.pokerlanka.innertube.models.PlaylistItem
import com.pokerlanka.innertube.models.filterYoutubeShorts
import com.pokerlanka.innertube.utils.completed
import com.pokerlanka.mixora.constants.HideYoutubeShortsKey
import com.pokerlanka.mixora.db.MusicDatabase
import com.pokerlanka.mixora.db.entities.PodcastEntity
import com.pokerlanka.mixora.ui.utils.resize
import com.pokerlanka.mixora.utils.dataStore
import com.pokerlanka.mixora.utils.get
import com.pokerlanka.mixora.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AccountContentType {
    PLAYLISTS, ALBUMS, ARTISTS, PODCASTS
}

@HiltViewModel
class AccountViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    database: MusicDatabase,
) : ViewModel() {
    val playlists = MutableStateFlow<List<PlaylistItem>?>(null)
    val albums = MutableStateFlow<List<AlbumItem>?>(null)
    val artists = MutableStateFlow<List<ArtistItem>?>(null)
    // SE "Episodes for Later" playlist shown in Podcasts tab
    val sePlaylist = MutableStateFlow<PlaylistItem?>(null)
    // RDPN "New Episodes" playlist (real thumbnail + count from YouTube)
    val rdpnPlaylist = MutableStateFlow<PlaylistItem?>(null)
    // Subscribed podcast shows (from local DB, synced from Mixora)
    val podcastPlaylists = database.subscribedPodcasts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    // Podcast host channels from Mixora library
    val podcastChannels = MutableStateFlow<List<ArtistItem>>(emptyList())

    // Selected content type for chips
    val selectedContentType = MutableStateFlow(AccountContentType.PLAYLISTS)

    private suspend fun loadPlaylists() {
        val hideYoutubeShorts = context.dataStore.get(HideYoutubeShortsKey, false)
        YouTube.library("FEmusic_liked_playlists").completed().onSuccess {
            val all = it.items.filterIsInstance<PlaylistItem>()
            // Extract SE playlist separately for Podcasts tab
            sePlaylist.value = all.find { it.id == "SE" }
            playlists.value = all
                .filterNot { it.id == "SE" }
                .filterYoutubeShorts(hideYoutubeShorts)
        }.onFailure {
            reportException(it)
        }
    }

    init {
        viewModelScope.launch {
            loadPlaylists()
            YouTube.library("FEmusic_liked_albums").completed().onSuccess {
                albums.value = it.items.filterIsInstance<AlbumItem>()
            }.onFailure {
                reportException(it)
            }
            YouTube.library("FEmusic_library_corpus_artists").completed().onSuccess {
                artists.value = it.items.filterIsInstance<ArtistItem>().map { artist ->
                    artist.copy(
                        thumbnail = artist.thumbnail?.resize(544, 544)
                    )
                }
            }.onFailure {
                reportException(it)
            }
        }
        viewModelScope.launch {
            YouTube.newEpisodesPlaylistInfo().onSuccess {
                rdpnPlaylist.value = it
            }.onFailure {
                reportException(it)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            YouTube.libraryPodcastChannels().onSuccess {
                podcastChannels.value = it.items.filterIsInstance<ArtistItem>()
            }.onFailure {
                reportException(it)
            }
        }

        // Listen for HideYoutubeShorts preference changes and reload playlists instantly
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[HideYoutubeShortsKey] ?: false }
                .distinctUntilChanged()
                .collect {
                    if (playlists.value != null) {
                        loadPlaylists()
                    }
                }
        }
    }

    fun setSelectedContentType(contentType: AccountContentType) {
        selectedContentType.value = contentType
    }
}
