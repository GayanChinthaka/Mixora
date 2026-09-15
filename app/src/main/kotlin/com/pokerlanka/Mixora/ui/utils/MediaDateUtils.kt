/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.pokerlanka.innertube.YouTube
import com.pokerlanka.innertube.models.MediaInfo
import com.pokerlanka.mixora.R
import java.util.concurrent.ConcurrentHashMap

data class FormattedMediaDate(
    val label: String,
    val dateText: String,
) {
    val fullText: String
        get() = "$label: $dateText"
}

object MediaDateCache {
    private val cache = ConcurrentHashMap<String, MediaInfo>()

    suspend fun getMediaInfo(videoId: String): MediaInfo? {
        cache[videoId]?.let { return it }
        val result = YouTube.getMediaInfo(videoId).getOrNull()
        if (result != null) {
            cache[videoId] = result
        }
        return result
    }

    fun getCached(videoId: String): MediaInfo? = cache[videoId]
}

@Composable
fun formatMediaDate(
    uploadDate: String?,
    relativeDate: String?,
    year: Int? = null,
): FormattedMediaDate? {
    val isStreamed = (uploadDate?.contains("stream", ignoreCase = true) == true) ||
            (relativeDate?.contains("stream", ignoreCase = true) == true)
    val isPremiered = (uploadDate?.contains("premier", ignoreCase = true) == true) ||
            (relativeDate?.contains("premier", ignoreCase = true) == true)

    val label = when {
        isStreamed -> stringResource(R.string.streamed_date)
        isPremiered -> stringResource(R.string.premiere_date)
        else -> stringResource(R.string.upload_date)
    }

    val dateText = remember(uploadDate, relativeDate, year) {
        when {
            !uploadDate.isNullOrBlank() && !relativeDate.isNullOrBlank() -> {
                if (uploadDate.contains(relativeDate, ignoreCase = true) || relativeDate.contains(uploadDate, ignoreCase = true)) {
                    uploadDate
                } else {
                    "$uploadDate ($relativeDate)"
                }
            }
            !uploadDate.isNullOrBlank() -> uploadDate
            !relativeDate.isNullOrBlank() -> relativeDate
            year != null -> year.toString()
            else -> null
        }
    } ?: return null

    return FormattedMediaDate(label, dateText)
}
