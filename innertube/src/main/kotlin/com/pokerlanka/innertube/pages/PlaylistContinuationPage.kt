package com.pokerlanka.innertube.pages

import com.pokerlanka.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)
