/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokerlanka.mixora.db.MusicDatabase
import com.pokerlanka.mixora.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.pokerlanka.mixora.lyrics.LyricsEntry
import com.pokerlanka.mixora.lyrics.LyricsRomanizationCoordinator
import com.pokerlanka.mixora.lyrics.LyricsUtils
import com.pokerlanka.mixora.ui.component.LyricsListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltViewModel
class LyricsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
) : ViewModel() {
    companion object {
        private val timestampRegex = Regex("\\[\\d{1,2}:\\d{2}")
        private const val LogTag = "LyricsRomanization"
    }

    private val romanizationCoordinator = LyricsRomanizationCoordinator(context, database)

    private var processJob: Job? = null

    private val _lines = MutableStateFlow<List<LyricsEntry>>(emptyList())
    val lines: StateFlow<List<LyricsEntry>> = _lines.asStateFlow()

    private val _mergedLyricsList = MutableStateFlow<List<LyricsListItem>>(emptyList())
    val mergedLyricsList: StateFlow<List<LyricsListItem>> = _mergedLyricsList.asStateFlow()

    /** True while a romanization request is in flight, for a progress affordance. */
    private val _isRomanizing = MutableStateFlow(false)
    val isRomanizing: StateFlow<Boolean> = _isRomanizing.asStateFlow()

    fun processLyrics(
        lyrics: String?,
        romanizeEnabled: Boolean,
        showIntervalIndicator: Boolean,
    ) {
        processJob?.cancel()
        processJob = viewModelScope.launch {
            val processedLines = withContext(Dispatchers.Default) {
                if (lyrics == null || lyrics == LYRICS_NOT_FOUND) {
                    emptyList()
                } else {
                    val isLrc = timestampRegex.containsMatchIn(lyrics)
                    val parsedLines = if (isLrc) LyricsUtils.parseLyrics(lyrics) else emptyList()

                    if (parsedLines.isNotEmpty()) {
                        listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + parsedLines
                    } else {
                        // Fallback for unsynced or invalid LRC
                        val baseTime = 1000000L
                        lyrics.lines()
                            .filter { it.isNotBlank() && !timestampRegex.containsMatchIn(it) }
                            .mapIndexed { index, line ->
                                LyricsEntry(baseTime + index, line)
                            }
                    }
                }
            }

            _lines.value = processedLines
            updateMergedList(processedLines, showIntervalIndicator)

            // Romanize only after the UI already shows the original text. Each entry's
            // romanizedTextFlow swaps itself in when a value arrives, so a slow or failed request
            // never blocks or empties the lyrics sheet.
            if (romanizeEnabled && lyrics != null && lyrics != LYRICS_NOT_FOUND) {
                launch {
                    romanizationCoordinator.romanize(
                        lyrics = lyrics,
                        entries = processedLines,
                        onRunningChange = { _isRomanizing.value = it },
                    )
                }
            } else {
                Timber.tag(LogTag).d(
                    "romanization not attempted: enabled=%b, hasLyrics=%b",
                    romanizeEnabled,
                    lyrics != null && lyrics != LYRICS_NOT_FOUND,
                )
            }
        }
    }

    private fun updateMergedList(lines: List<LyricsEntry>, showIntervalIndicator: Boolean) {
        val result = mutableListOf<LyricsListItem>()
        if (lines.isEmpty()) {
            _mergedLyricsList.value = result
            return
        }
        lines.forEachIndexed { i, entry ->
            if (entry.text.isNotBlank()) {
                result.add(LyricsListItem.Line(i, entry))
            }
            if (showIntervalIndicator && i < lines.size - 1) {
                val nextStart = lines[i + 1].time
                val currentEnd = if (!entry.words.isNullOrEmpty()) {
                    (entry.words.last().endTime * 1000).toLong()
                } else if (entry.text.isBlank()) {
                    entry.time
                } else {
                    null
                }

                if (currentEnd != null && currentEnd < nextStart) {
                    val gap = nextStart - currentEnd
                    if (gap > 4000L) {
                        result.add(LyricsListItem.Indicator(i, gap, currentEnd, nextStart, lines[i + 1].agent))
                    }
                }
            }
        }
        _mergedLyricsList.value = result
    }
}
