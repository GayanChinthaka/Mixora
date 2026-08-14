package com.pokerlanka.innertube.pages

import com.pokerlanka.innertube.models.Album
import com.pokerlanka.innertube.models.AlbumItem
import com.pokerlanka.innertube.models.Artist
import com.pokerlanka.innertube.models.ArtistItem
import com.pokerlanka.innertube.models.MusicResponsiveListItemRenderer
import com.pokerlanka.innertube.models.MusicTwoRowItemRenderer
import com.pokerlanka.innertube.models.PlaylistItem
import com.pokerlanka.innertube.models.SongItem
import com.pokerlanka.innertube.models.YTItem
import com.pokerlanka.innertube.models.oddElements
import com.pokerlanka.innertube.utils.parseTime

data class LibraryAlbumsPage(
    val albums: List<AlbumItem>,
    val continuation: String?,
) {
    companion object {
        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
            return AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = null,
                        year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
        }
    }
}
