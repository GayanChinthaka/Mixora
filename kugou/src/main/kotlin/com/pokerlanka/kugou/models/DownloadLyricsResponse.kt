package com.pokerlanka.kugou.models

import kotlinx.serialization.Serializable

@Serializable
data class DownloadLyricsResponse(
    val status: Int = 200,
    val info: String = "",
    val content: String = "",
)
