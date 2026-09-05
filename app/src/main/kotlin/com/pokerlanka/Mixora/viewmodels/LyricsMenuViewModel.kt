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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
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
            // 1. Immediately flag UI that fetch is starting so loading indicator displays.
            //    LyricsHelper clears this marker in a finally block on every exit path.
            lyricsHelper.startFetching(mediaMetadata.id, selectedProvider)

            try {
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
                //    LYRICS_NOT_FOUND is never persisted: it is also what an offline fetch or a
                //    timeout returns, and storing that would permanently hide lyrics the song
                //    really has.
                if (result.lyrics != LyricsEntity.LYRICS_NOT_FOUND && result.lyrics.isNotBlank()) {
                    database.upsert(LyricsEntity(mediaMetadata.id, result.lyrics, result.provider))
                    if (selectedProvider != null) {
                        toast("Lyrics loaded from ${result.provider}")
                    }
                } else if (selectedProvider != null) {
                    toast("No lyrics found on $selectedProvider")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Without this the refetch failed silently and the menu looked like it did nothing.
                Timber.tag("LyricsMenuViewModel").w(e, "Refetch failed for ${mediaMetadata.id}")
                toast("Could not fetch lyrics")
            } finally {
                // Safety net for a failure before the fetch itself takes ownership of the marker
                // (for example the DB delete throwing); clearing twice is harmless.
                lyricsHelper.clearFetching(mediaMetadata.id)
            }
        }
    }

    private suspend fun toast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
