/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.db.entities

import androidx.compose.runtime.Immutable

@Immutable
data class ListeningTotals(
    val totalPlayCount: Int,
    val totalTimeListened: Long,
)
