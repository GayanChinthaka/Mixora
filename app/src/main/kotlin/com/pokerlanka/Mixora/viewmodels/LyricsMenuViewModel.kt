/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.viewmodels

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokerlanka.mixora.db.MusicDatabase
import com.pokerlanka.mixora.db.entities.LyricsEntity
import com.pokerlanka.mixora.lyrics.LyricsHelper
import com.pokerlanka.mixora.models.MediaMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class LyricsMenuViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    val lyricsHelper: LyricsHelper,
    val database: MusicDatabase,
) : ViewModel() {

    fun refetchLyrics(
        mediaMetadata: MediaMetadata,
        selectedProvider: String? = null,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Immediately flag UI that fetch is starting so loading indicator displays
            lyricsHelper.startFetching(selectedProvider)

            // 2. Clear in-memory cache and cancel in-flight jobs for this track
            lyricsHelper.clearCache(mediaMetadata.id)

            // 3. Delete existing lyrics from DB synchronously
            database.deleteLyricsById(mediaMetadata.id)

            // 4. Fetch fresh lyrics (from selected provider or across all providers)
            val result = if (selectedProvider != null) {
                lyricsHelper.getLyricsFromProvider(mediaMetadata, selectedProvider)
            } else {
                lyricsHelper.getLyrics(mediaMetadata, skipCache = true)
            }

            // 5. Save to DB ONLY if valid lyrics are found.
            // DO NOT save LYRICS_NOT_FOUND to Room DB!
            if (result.lyrics != LyricsEntity.LYRICS_NOT_FOUND && result.lyrics.isNotBlank()) {
                database.upsert(LyricsEntity(mediaMetadata.id, result.lyrics, result.provider))
                if (selectedProvider != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Lyrics loaded from ${result.provider}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else if (selectedProvider != null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "No lyrics found on $selectedProvider", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
