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
import kotlin.random.Random

@Immutable
@Entity(tableName = "playlist")
data class PlaylistEntity(
    @PrimaryKey val id: String = generatePlaylistId(),
    val name: String,
    val browseId: String? = null,
    val createdAt: LocalDateTime? = LocalDateTime.now(),
    val lastUpdateTime: LocalDateTime? = LocalDateTime.now(),
    @ColumnInfo(name = "isEditable", defaultValue = true.toString())
    val isEditable: Boolean = true,
    val bookmarkedAt: LocalDateTime? = null,
    val remoteSongCount: Int? = null,
    val playEndpointParams: String? = null,
    val thumbnailUrl: String? = null,
    val shuffleEndpointParams: String? = null,
    val radioEndpointParams: String? = null,
    @ColumnInfo(name = "isLocal", defaultValue = false.toString())
    val isLocal: Boolean = false,
    @ColumnInfo(name = "isAutoSync", defaultValue = false.toString())
    val isAutoSync: Boolean = false
) {
    companion object {
        const val LIKED_PLAYLIST_ID = "LP_LIKED"
        const val DOWNLOADED_PLAYLIST_ID = "LP_DOWNLOADED"
        const val WEEKLY_MOST_PLAYLIST_ID = "LP_WEEKLY_MOST"
        const val MONTHLY_MOST_PLAYLIST_ID = "LP_MONTHLY_MOST"

        private val PLAYLIST_ID_CHARS = ('a'..'z') + ('A'..'Z')
        fun generatePlaylistId() = "LP" + (1..8).map { PLAYLIST_ID_CHARS.random() }.joinToString("")
    }

    val shareLink: String?
        get() {
            return if (browseId != null)
                "https://music.youtube.com/playlist?list=$browseId"
            else null
        }

    fun localToggleLike() = copy(
        bookmarkedAt = if (bookmarkedAt != null) null else LocalDateTime.now()
    )

    fun toggleLike() = localToggleLike().also {
        SongEntity.ytSyncScope.launch {
            if (browseId != null)
                YouTube.likePlaylist(browseId, bookmarkedAt == null)
        }
    }
}
