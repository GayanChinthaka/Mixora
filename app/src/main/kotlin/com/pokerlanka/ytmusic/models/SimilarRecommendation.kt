/**
 * YTmusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.ytmusic.models

import com.pokerlanka.innertube.models.YTItem
import com.pokerlanka.ytmusic.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
