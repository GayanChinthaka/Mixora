package com.pokerlanka.innertube.models.body

import com.pokerlanka.innertube.models.Context
import com.pokerlanka.innertube.models.Continuation
import kotlinx.serialization.Serializable

@Serializable
data class BrowseBody(
    val context: Context,
    val browseId: String?,
    val params: String?,
    val continuation: String?
)
