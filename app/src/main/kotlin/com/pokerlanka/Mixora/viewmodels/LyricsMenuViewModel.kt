/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokerlanka.mixora.db.MusicDatabase
import com.pokerlanka.mixora.db.entities.LyricsEntity
import com.pokerlanka.mixora.lyrics.LyricsHelper
import com.pokerlanka.mixora.models.MediaMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LyricsMenuViewModel
@Inject
constructor(
    val lyricsHelper: LyricsHelper,
    val database: MusicDatabase,
) : ViewModel() {

    fun refetchLyrics(
        mediaMetadata: MediaMetadata,
        lyricsEntity: LyricsEntity?,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Delete from Room DB to reset state in UI
            database.query {
                lyricsEntity?.let(::delete)
            }
            // 2. Clear in-memory cache and fetch fresh lyrics
            lyricsHelper.clearCache(mediaMetadata.id)
            val lyricsWithProvider = lyricsHelper.getLyrics(mediaMetadata, skipCache = true)
            // 3. Upsert newly fetched lyrics to Room DB
            database.query {
                upsert(LyricsEntity(mediaMetadata.id, lyricsWithProvider.lyrics, lyricsWithProvider.provider))
            }
        }
    }
}
