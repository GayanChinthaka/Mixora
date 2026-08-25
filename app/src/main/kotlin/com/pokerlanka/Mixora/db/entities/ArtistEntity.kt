/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pokerlanka.innertube.YouTube
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Immutable
@Entity(tableName = "artist")
data class ArtistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val channelId: String? = null,
    val lastUpdateTime: LocalDateTime = LocalDateTime.now(),
    val bookmarkedAt: LocalDateTime? = null,
    @ColumnInfo(name = "isLocal", defaultValue = false.toString())
    val isLocal: Boolean = false,
    @ColumnInfo(name = "isPodcastChannel", defaultValue = false.toString())
    val isPodcastChannel: Boolean = false,
    @ColumnInfo(name = "cachedPageJson")
    val cachedPageJson: String? = null
) {
    val isYouTubeArtist: Boolean
        get() = id.startsWith("UC") || id.startsWith("FEmusic_library_privately_owned_artist")

    val isPrivatelyOwnedArtist: Boolean
        get() = id.startsWith("FEmusic_library_privately_owned_artist")

    fun localToggleLike() = copy(
        bookmarkedAt = if (bookmarkedAt != null) null else LocalDateTime.now(),
    )

    fun toggleLike() = localToggleLike().also {
        SongEntity.ytSyncScope.launch {
            val targetChannelId = channelId ?: YouTube.getChannelId(id)
            if (targetChannelId.isNotEmpty()) {
                YouTube.subscribeChannel(targetChannelId, bookmarkedAt == null)
            }
        }
    }

    companion object {
        private val ARTIST_ID_CHARS = ('a'..'z') + ('A'..'Z')
        fun generateArtistId() = "LA" + (1..8).map { ARTIST_ID_CHARS.random() }.joinToString("")
    }
}
