package com.pokerlanka.innertube.pages

import com.pokerlanka.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
