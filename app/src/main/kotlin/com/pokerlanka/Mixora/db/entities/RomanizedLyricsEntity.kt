/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.db.entities

import androidx.room.Entity

/**
 * Cached AI romanization for one set of lyrics.
 *
 * Keyed on a hash of the lyrics text rather than a song id on purpose: the same lyrics can arrive
 * from several providers for the same song, and a resync or provider switch replaces the text
 * entirely. Hashing the text means a changed lyric is automatically a cache miss and a re-fetched
 * identical lyric is automatically a hit, with no invalidation bookkeeping anywhere else.
 *
 * [style] carries every user choice that changes the output (currently pinyin tone marks), so
 * flipping a romanization setting misses the cache instead of serving output from the old style.
 */
@Entity(tableName = "romanized_lyrics", primaryKeys = ["lyricsHash", "style"])
data class RomanizedLyricsEntity(
    val lyricsHash: String,
    val style: String,
    /** One romanized line per source line, newline-joined, index-aligned with the source. */
    val romanized: String,
    /** Guards against a row written by an older, differently-segmented parser. */
    val lineCount: Int,
    /** Which model produced this, so a future re-run can target stale rows. */
    val model: String,
    val createdAt: Long = System.currentTimeMillis(),
)
