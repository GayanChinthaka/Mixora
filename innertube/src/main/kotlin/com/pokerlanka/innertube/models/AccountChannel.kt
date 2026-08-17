/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.innertube.models

data class AccountChannel(
    val name: String,
    val byline: String?,
    val channelHandle: String?,
    val thumbnailUrl: String?,
    val dataSyncId: String,
    val isSelected: Boolean,
)
