/**
 * YTmusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.ytmusic.lyrics

import android.content.Context
import com.pokerlanka.ytmusic.betterlyrics.BetterLyrics
import com.pokerlanka.ytmusic.constants.EnableBetterLyricsKey
import com.pokerlanka.ytmusic.utils.dataStore
import com.pokerlanka.ytmusic.utils.get

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
