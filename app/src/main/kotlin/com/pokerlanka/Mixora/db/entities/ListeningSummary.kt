/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.db.entities

import androidx.compose.runtime.Immutable

@Immutable
data class ListeningSummary(
    val totalPlayCount: Int = 0,
    val totalTimeListened: Long = 0L,
    val uniqueSongsCount: Int = 0,
    val uniqueArtistsCount: Int = 0,
    val uniqueAlbumsCount: Int = 0,
)
