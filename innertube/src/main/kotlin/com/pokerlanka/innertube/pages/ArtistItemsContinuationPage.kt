package com.pokerlanka.innertube.pages

import com.pokerlanka.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
