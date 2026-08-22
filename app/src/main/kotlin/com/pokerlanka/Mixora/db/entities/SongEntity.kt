/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pokerlanka.innertube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Immutable
@Entity(
    tableName = "song",
    indices = [
        Index(
            value = ["albumId"],
        ),
    ],
)
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val duration: Int = -1, // in seconds
    val thumbnailUrl: String? = null,
    val albumId: String? = null,
    val albumName: String? = null,
    @ColumnInfo(defaultValue = "0")
    val explicit: Boolean = false,
    val year: Int? = null,
    val date: LocalDateTime? = null, // ID3 tag property
    val dateModified: LocalDateTime? = null, // file property
    val liked: Boolean = false,
    val likedDate: LocalDateTime? = null,
    val totalPlayTime: Long = 0, // in milliseconds
    val inLibrary: LocalDateTime? = null,
    val dateDownload: LocalDateTime? = null,
    @ColumnInfo(name = "isLocal", defaultValue = false.toString())
    val isLocal: Boolean = false,
    val libraryAddToken: String? = null,
    val libraryRemoveToken: String? = null,
    @ColumnInfo(defaultValue = "0")
    val lyricsOffset: Int = 0,
    @ColumnInfo(defaultValue = true.toString())
    val romanizeLyrics: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val isDownloaded: Boolean = false,
    @ColumnInfo(name = "isUploaded", defaultValue = false.toString())
    val isUploaded: Boolean = false,
    @ColumnInfo(name = "isVideo", defaultValue = false.toString())
    val isVideo: Boolean = false,
    @ColumnInfo(name = "isEpisode", defaultValue = false.toString())
    val isEpisode: Boolean = false,
    @ColumnInfo(name = "playbackPosition", defaultValue = "NULL")
    val playbackPosition: Long? = null,
    @ColumnInfo(name = "uploadEntityId", defaultValue = "NULL")
    val uploadEntityId: String? = null,
    @ColumnInfo(name = "isCached", defaultValue = "0")
    val isCached: Boolean = false,
) {
    companion object {
        /**
         * Shared scope for fire-and-forget YouTube sync calls from entity toggle methods.
         * Uses SupervisorJob so individual failures don't cancel sibling calls,
         * and lives for the process lifetime (matching the old behavior but without
         * creating a throwaway scope per call).
         */
        internal val ytSyncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    fun localToggleLike() =
        copy(
            liked = !liked,
            likedDate = if (!liked) LocalDateTime.now() else null,
        )

    fun toggleLike() =
        copy(
            liked = !liked,
            likedDate = if (!liked) LocalDateTime.now() else null,
            inLibrary = if (!liked) inLibrary ?: LocalDateTime.now() else inLibrary,
        ).also {
            ytSyncScope.launch {
                YouTube.likeVideo(id, !liked)
            }
        }

    fun toggleLibrary(syncToYouTube: Boolean = true) =
        copy(
            liked = if (inLibrary == null) liked else false,
            inLibrary = if (inLibrary == null) LocalDateTime.now() else null,
            likedDate = if (inLibrary == null) likedDate else null,
        ).also {
            if (syncToYouTube) {
                ytSyncScope.launch {
                    // Use the new reliable method that fetches fresh tokens
                    val addToLibrary = inLibrary == null
                    YouTube.toggleSongLibrary(id, addToLibrary)
                }
            }
        }

    fun toggleUploaded() =
        copy(
            isUploaded = !isUploaded,
        )
}
