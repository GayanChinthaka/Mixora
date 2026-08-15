/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.lyrics

import android.content.Context
import com.pokerlanka.mixora.betterlyrics.BetterLyrics
import com.pokerlanka.mixora.constants.EnableBetterLyricsKey
import com.pokerlanka.mixora.utils.dataStore
import com.pokerlanka.mixora.utils.get

object BetterLyricsProvider : LyricsProvider {
    override val name = "BetterLyrics"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableBetterLyricsKey] ?: true

    override suspend fun getLyrics(
        context: Context,
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = BetterLyrics.getLyrics(title, artist, duration, album)
}
