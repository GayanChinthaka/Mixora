/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.lyrics

import android.content.Context

data class LyricsWithSource(
    val lyrics: String,
    val sourceName: String,
)

interface LyricsProvider {
    val name: String

    fun isEnabled(context: Context): Boolean

    suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ): Result<String>

    suspend fun getLyricsWithSource(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ): Result<LyricsWithSource> =
        getLyrics(context, id, title, artist, duration, album).map { LyricsWithSource(it, name) }
}
