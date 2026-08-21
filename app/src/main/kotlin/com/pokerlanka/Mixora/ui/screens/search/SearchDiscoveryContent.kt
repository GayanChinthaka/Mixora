/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pokerlanka.innertube.models.AlbumItem
import com.pokerlanka.innertube.models.ArtistItem
import com.pokerlanka.innertube.models.SongItem
import com.pokerlanka.innertube.models.WatchEndpoint
import com.pokerlanka.innertube.pages.MoodAndGenres
import com.pokerlanka.mixora.LocalNavController
import com.pokerlanka.mixora.LocalPlayerAwareWindowInsets
import com.pokerlanka.mixora.LocalPlayerConnection
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.constants.DisableBlurKey
import com.pokerlanka.mixora.models.toMediaMetadata
import com.pokerlanka.mixora.playback.queues.YouTubeQueue
import com.pokerlanka.mixora.search.SearchDiscoveryUiModel
import com.pokerlanka.mixora.ui.component.LocalMenuState
import com.pokerlanka.mixora.ui.component.NavigationTitle
import com.pokerlanka.mixora.ui.component.YouTubeGridItem
import com.pokerlanka.mixora.ui.component.YouTubeListItem
import com.pokerlanka.mixora.ui.component.shimmer.ShimmerHost
import com.pokerlanka.mixora.ui.component.shimmer.TextPlaceholder
import com.pokerlanka.mixora.ui.menu.YouTubeAlbumMenu
import com.pokerlanka.mixora.ui.menu.YouTubeArtistMenu
import com.pokerlanka.mixora.ui.menu.YouTubeSongMenu
import com.pokerlanka.mixora.utils.rememberPreference
import com.pokerlanka.mixora.viewmodels.SearchDiscoveryScreenState
import com.pokerlanka.mixora.viewmodels.SearchDiscoveryTab
import com.pokerlanka.mixora.viewmodels.SearchDiscoveryViewModel

/**
 * What the search tab shows before anything is typed: mood/genre buckets plus
 * song, artist and album picks seeded from listening history. Replaces the bare
 * search-history list that used to be the only thing on an empty query.
 */
@Composable
fun SearchDiscoveryContent(
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    viewModel: SearchDiscoveryViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val (disableBlur) = rememberPreference(DisableBlurKey, false)
    val lazyListState = rememberLazyListState()

    val tonalStart = MaterialTheme.colorScheme.primaryContainer
    val tonalMiddle = MaterialTheme.colorScheme.secondaryContainer

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background),
    ) {
        if (!disableBlur && !pureBlack) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(GlowHeight)
                        .align(Alignment.TopCenter)
                        .drawWithCache {
                            val brush =
                                Brush.verticalGradient(
                                    0f to tonalStart.copy(alpha = 0.30f),
                                    0.42f to tonalMiddle.copy(alpha = 0.14f),
                                    1f to Color.Transparent,
                                )
                            onDrawBehind { drawRect(brush) }
                        },
            )
        }

        LazyColumn(
            state = lazyListState,
            // The hosting Scaffold's top bar already consumes the top inset, so taking it
            // again here would leave a gap between the search field and the tab row.
            contentPadding =
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                    .asPaddingValues(),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "discovery_tabs", contentType = "discovery_tabs") {
                SearchDiscoveryTabs(
                    selectedTab = selectedTab,
                    onTabSelected = viewModel::selectTab,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            when (val currentState = state) {
                SearchDiscoveryScreenState.Loading -> {
                    item(key = "discovery_loading", contentType = "discovery_loading") {
                        SearchDiscoveryLoading()
                    }
                }

                SearchDiscoveryScreenState.Empty -> {
                    item(key = "discovery_empty", contentType = "discovery_message") {
                        SearchStateMessage(message = stringResource(R.string.no_results_found))
                    }
                }

                is SearchDiscoveryScreenState.Error -> {
                    item(key = "discovery_error", contentType = "discovery_message") {
                        SearchStateMessage(
                            message = stringResource(currentState.messageResId),
                            action = {
                                Button(onClick = viewModel::retry) {
                                    Text(stringResource(R.string.retry))
                                }
                            },
                        )
                    }
                }

                is SearchDiscoveryScreenState.Success -> {
                    when (selectedTab) {
                        SearchDiscoveryTab.EXPLORE -> {
                            item(key = "discovery_moods_title", contentType = "section_title") {
                                NavigationTitle(title = stringResource(R.string.mood_and_genres))
                            }
                            item(key = "discovery_moods", contentType = "mood_grid") {
                                SearchMoodAndGenresGrid(
                                    data = currentState.data,
                                    onItemClick = { item ->
                                        navController.navigate(
                                            "youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}",
                                        )
                                    },
                                )
                            }
                        }

                        SearchDiscoveryTab.SUGGESTIONS -> {
                            item(key = "discovery_songs", contentType = "suggestion_songs") {
                                SuggestedSongsSection(songs = currentState.data.suggestedSongs)
                            }
                            item(key = "discovery_artists", contentType = "suggestion_artists") {
                                SuggestedArtistsSection(artists = currentState.data.suggestedArtists)
                            }
                            item(key = "discovery_albums", contentType = "suggestion_albums") {
                                TrendingAlbumsSection(albums = currentState.data.trendingAlbums)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchDiscoveryTabs(
    selectedTab: SearchDiscoveryTab,
    onTabSelected: (SearchDiscoveryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = remember { SearchDiscoveryTab.entries }
    PrimaryTabRow(
        selectedTabIndex = tabs.indexOf(selectedTab),
        modifier = modifier,
        containerColor = Color.Transparent,
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        painter =
                            painterResource(
                                when (tab) {
                                    SearchDiscoveryTab.EXPLORE -> R.drawable.explore_outlined
                                    SearchDiscoveryTab.SUGGESTIONS -> R.drawable.auto_awesome
                                },
                            ),
                        contentDescription = null,
                    )
                },
                text = {
                    Text(
                        text =
                            stringResource(
                                when (tab) {
                                    SearchDiscoveryTab.EXPLORE -> R.string.search_discovery_explore
                                    SearchDiscoveryTab.SUGGESTIONS -> R.string.search_discovery_suggestions
                                },
                            ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
private fun SearchMoodAndGenresGrid(
    data: SearchDiscoveryUiModel,
    onItemClick: (MoodAndGenres.Item) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (data.moodAndGenres.isEmpty()) return

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columnCount = (maxWidth.value / MoodCellMinWidth.value).toInt().coerceAtLeast(1)
        val rowCount = ((data.moodAndGenres.size + columnCount - 1) / columnCount).coerceAtLeast(1)

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = MoodCellMinWidth),
            contentPadding = PaddingValues(6.dp),
            userScrollEnabled = false,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height((MoodCardHeight + 12.dp) * rowCount + 12.dp),
        ) {
            items(
                items = data.moodAndGenres,
                key = { item -> "${item.title}:${item.endpoint.browseId}:${item.endpoint.params}" },
                contentType = { "mood_card" },
            ) { item ->
                SearchMoodCard(
                    title = item.title,
                    stripeColor = item.stripeColor,
                    onClick = { onItemClick(item) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                )
            }
        }
    }
}

/**
 * Mood tile styled after ArchiveTune's: the API's stripe colour tinted toward the
 * active theme and laid under a scrim so the label stays readable in either theme.
 */
@Composable
private fun SearchMoodCard(
    title: String,
    stripeColor: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val base = remember(stripeColor) { Color(stripeColor) }
    val cardBrush =
        remember(base, colorScheme.primaryContainer, colorScheme.surfaceContainerHighest) {
            Brush.linearGradient(
                colors =
                    listOf(
                        lerp(base, colorScheme.primaryContainer, 0.18f),
                        lerp(base, colorScheme.surfaceContainerHighest, 0.34f),
                    ),
                start = Offset.Zero,
                end = Offset(900f, 650f),
            )
        }
    val coverBrush =
        remember(base, colorScheme.surface, colorScheme.scrim) {
            Brush.linearGradient(
                colors =
                    listOf(
                        lerp(base, colorScheme.surface, 0.28f),
                        lerp(base, colorScheme.scrim, 0.2f),
                    ),
                start = Offset.Zero,
                end = Offset(360f, 360f),
            )
        }
    val textScrimBrush =
        remember(colorScheme.scrim) {
            Brush.horizontalGradient(
                colors =
                    listOf(
                        colorScheme.scrim.copy(alpha = 0.38f),
                        colorScheme.scrim.copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
            )
        }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.height(MoodCardHeight),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(cardBrush),
        ) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 12.dp)
                        .size(MoodCoverSize)
                        .clip(RoundedCornerShape(14.dp))
                        .background(coverBrush),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(textScrimBrush),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, end = 76.dp, bottom = 14.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SuggestedSongsSection(
    songs: List<SongItem>,
    modifier: Modifier = Modifier,
) {
    if (songs.isEmpty()) return

    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val visibleSongs = remember(songs) { songs.take(MaxSuggestedSongRows) }

    Column(modifier = modifier.fillMaxWidth()) {
        NavigationTitle(title = stringResource(R.string.search_discovery_songs))

        Column(
            verticalArrangement = Arrangement.spacedBy(SongGroupItemSpacing),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = SongGroupHorizontalPadding,
                        vertical = SongGroupVerticalPadding,
                    ),
        ) {
            visibleSongs.forEachIndexed { index, song ->
                val isActive = song.id == mediaMetadata?.id
                Card(
                    shape = segmentedSongShape(index = index, count = visibleSongs.size),
                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                if (isActive) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (isActive) {
                                        playerConnection.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            YouTubeQueue(
                                                endpoint = song.endpoint ?: WatchEndpoint(videoId = song.id),
                                                preloadItem = song.toMediaMetadata(),
                                            ),
                                        )
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        YouTubeSongMenu(
                                            song = song,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ),
                ) {
                    YouTubeListItem(
                        item = song,
                        albumIndex = index + 1,
                        isActive = isActive,
                        isPlaying = isPlaying,
                        isSwipeable = false,
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    menuState.show {
                                        YouTubeSongMenu(
                                            song = song,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SuggestedArtistsSection(
    artists: List<ArtistItem>,
    modifier: Modifier = Modifier,
) {
    if (artists.isEmpty()) return

    val navController = LocalNavController.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        NavigationTitle(title = stringResource(R.string.search_discovery_artists))
        LazyRow(
            contentPadding =
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Horizontal)
                    .asPaddingValues(),
        ) {
            items(
                items = artists,
                key = { artist -> artist.id },
                contentType = { "discovery_artist" },
            ) { artist ->
                YouTubeGridItem(
                    item = artist,
                    modifier =
                        Modifier
                            .combinedClickable(
                                onClick = { navController.navigate("artist/${artist.id}") },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        YouTubeArtistMenu(
                                            artist = artist,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrendingAlbumsSection(
    albums: List<AlbumItem>,
    modifier: Modifier = Modifier,
) {
    if (albums.isEmpty()) return

    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxWidth()) {
        NavigationTitle(title = stringResource(R.string.search_discovery_albums))
        LazyRow(
            contentPadding =
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Horizontal)
                    .asPaddingValues(),
        ) {
            items(
                items = albums,
                key = { album -> album.id },
                contentType = { "discovery_album" },
            ) { album ->
                YouTubeGridItem(
                    item = album,
                    isActive = mediaMetadata?.album?.id == album.id,
                    isPlaying = isPlaying,
                    coroutineScope = coroutineScope,
                    modifier =
                        Modifier
                            .combinedClickable(
                                onClick = { navController.navigate("album/${album.id}") },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        YouTubeAlbumMenu(
                                            albumItem = album,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ),
                )
            }
        }
    }
}

@Composable
private fun SearchDiscoveryLoading(modifier: Modifier = Modifier) {
    ShimmerHost(modifier = modifier.fillMaxWidth()) {
        TextPlaceholder(
            height = 28.dp,
            modifier =
                Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .width(180.dp),
        )
        repeat(6) {
            TextPlaceholder(
                height = 84.dp,
                shape = RoundedCornerShape(20.dp),
                modifier =
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SearchStateMessage(
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable RowScope.() -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.search_off),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(content = action)
        }
    }
}

/**
 * Rounds only the outer edges of a run of cards so the group reads as one
 * segmented block instead of a stack of separate cards.
 */
private fun segmentedSongShape(
    index: Int,
    count: Int,
): Shape {
    val large = SongGroupLargeCorner
    val small = SongGroupSmallCorner
    return when {
        count <= 1 -> RoundedCornerShape(large)

        index == 0 ->
            RoundedCornerShape(
                topStart = large,
                topEnd = large,
                bottomEnd = small,
                bottomStart = small,
            )

        index == count - 1 ->
            RoundedCornerShape(
                topStart = small,
                topEnd = small,
                bottomEnd = large,
                bottomStart = large,
            )

        else -> RoundedCornerShape(small)
    }
}

private val GlowHeight = 430.dp
private val MoodCellMinWidth = 180.dp
private val MoodCardHeight = 104.dp
private val MoodCoverSize = 52.dp
private val SongGroupHorizontalPadding = 12.dp
private val SongGroupVerticalPadding = 2.dp
private val SongGroupItemSpacing = 2.dp
private val SongGroupLargeCorner = 28.dp
private val SongGroupSmallCorner = 6.dp
private const val MaxSuggestedSongRows = 6
