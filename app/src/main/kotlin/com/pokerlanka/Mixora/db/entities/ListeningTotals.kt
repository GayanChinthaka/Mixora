/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.db.entities

import androidx.compose.runtime.Immutable

@Immutable
data class ListeningTotals(
    val totalPlayCount: Int,
    val totalTimeListened: Long,
)
