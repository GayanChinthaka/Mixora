/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.viewmodels

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokerlanka.innertube.YouTube
import com.pokerlanka.innertube.models.Artist
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.constants.HideVideoSongsKey
import com.pokerlanka.mixora.constants.LastMonthlyMostPlaylistSyncKey
import com.pokerlanka.mixora.constants.LastWeeklyMostPlaylistSyncKey
import com.pokerlanka.mixora.constants.ShowMostStatsPlaylistsKey
import com.pokerlanka.mixora.constants.StatPeriod
import com.pokerlanka.mixora.constants.statToPeriod
import com.pokerlanka.mixora.db.MusicDatabase
import com.pokerlanka.mixora.db.entities.Album
import com.pokerlanka.mixora.db.entities.Artist as DbArtist
import com.pokerlanka.mixora.db.entities.EventWithSong
import com.pokerlanka.mixora.db.entities.ListeningBySlot
import com.pokerlanka.mixora.db.entities.ListeningSummary
import com.pokerlanka.mixora.db.entities.ListeningTotals
import com.pokerlanka.mixora.db.entities.PlaylistEntity
import com.pokerlanka.mixora.db.entities.Song
import com.pokerlanka.mixora.db.entities.SongWithStats
import com.pokerlanka.mixora.ui.screens.OptionStats
import com.pokerlanka.mixora.utils.dataStore
import com.pokerlanka.mixora.utils.reportException
import com.pokerlanka.mixora.utils.safeDataStoreEdit
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject

sealed interface StatsScreenState {
    data object Loading : StatsScreenState

    data class Success(
        val data: StatsUiData,
    ) : StatsScreenState

    data object Empty : StatsScreenState

    data class Error(
        @StringRes val messageResId: Int,
    ) : StatsScreenState
}

@Immutable
data class StatsUiData(
    val selectedOption: OptionStats,
    val selectedPeriodIndex: Int,
    val mostPlayedSongs: List<Song>,
    val visibleRankedSongs: List<SongWithStats>,
    val rankedSongCount: Int,
    val mostPlayedArtists: List<DbArtist>,
    val mostPlayedAlbums: List<Album>,
    val listeningByHour: List<ListeningBySlot>,
    val listeningByDayOfWeek: List<ListeningBySlot>,
    val listeningSummary: ListeningSummary,
    val firstEvent: EventWithSong?,
    val isSongListExpanded: Boolean,
    val canExpandSongList: Boolean,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    val database: MusicDatabase,
) : ViewModel() {
    private val periodicMostPlaylistSyncMutex = Mutex()
    val selectedOption = MutableStateFlow(OptionStats.CONTINUOUS)
    val indexChips = MutableStateFlow(0)
    private val isSongListExpanded = MutableStateFlow(false)
    private val isYearPickerOpen = MutableStateFlow(false)
    private val refreshRequest = MutableStateFlow(0L)

    val yearPickerOpen: StateFlow<Boolean> = isYearPickerOpen

    private val showMostStatsPlaylists =
        context.dataStore.data
            .map { it[ShowMostStatsPlaylistsKey] ?: true }
            .distinctUntilChanged()

    fun onOptionSelected(option: OptionStats) {
        if (selectedOption.value == option) return
        selectedOption.value = option
        indexChips.value = 0
        isSongListExpanded.value = false
    }

    fun onChipIndexChanged(index: Int) {
        if (indexChips.value == index) return
        indexChips.value = index
        isSongListExpanded.value = false
    }

    fun toggleSongListExpanded() {
        isSongListExpanded.value = !isSongListExpanded.value
    }

    fun showYearPicker() {
        isYearPickerOpen.value = true
    }

    fun dismissYearPicker() {
        isYearPickerOpen.value = false
    }

    fun retry() {
        refreshRequest.value += 1L
    }

    private fun periodPair() = combine(selectedOption, indexChips) { opt, idx -> Pair(opt, idx) }

    private fun toTimestamp(
        selection: OptionStats,
        t: Int,
    ): LocalDateTime =
        if (selection == OptionStats.CONTINUOUS || t == 0) {
            LocalDateTime.now()
        } else {
            statToPeriod(selection, t - 1)
        }

    val mostPlayedSongsStats: StateFlow<List<SongWithStats>> =
        combine(
            selectedOption,
            indexChips,
            context.dataStore.data.map { it[HideVideoSongsKey] ?: false }.distinctUntilChanged()
        ) { first, second, third -> Triple(first, second, third) }
            .flatMapLatest { (selection, t, hideVideoSongs) ->
                database
                    .mostPlayedSongsStats(
                        fromTimeStamp = statToPeriod(selection, t),
                        limit = -1,
                        toTimeStamp = toTimestamp(selection, t),
                    ).map { songs ->
                        if (hideVideoSongs) songs.filter { !it.isVideo } else songs
                    }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mostPlayedSongs: StateFlow<List<Song>> =
        combine(
            selectedOption,
            indexChips,
            context.dataStore.data.map { it[HideVideoSongsKey] ?: false }.distinctUntilChanged()
        ) { first, second, third -> Triple(first, second, third) }
            .flatMapLatest { (selection, t, hideVideoSongs) ->
                database
                    .mostPlayedSongs(
                        fromTimeStamp = statToPeriod(selection, t),
                        limit = -1,
                        toTimeStamp = toTimestamp(selection, t),
                    ).map { songs ->
                        if (hideVideoSongs) songs.filter { !it.song.isVideo } else songs
                    }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mostPlayedArtists: StateFlow<List<DbArtist>> =
        periodPair()
            .flatMapLatest { (selection, t) ->
                database
                    .mostPlayedArtists(
                        statToPeriod(selection, t),
                        limit = -1,
                        toTimeStamp = toTimestamp(selection, t),
                    ).map { artists ->
                        artists.filter { it.artist.isYouTubeArtist }
                    }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val mostPlayedAlbums: StateFlow<List<Album>> =
        periodPair()
            .flatMapLatest { (selection, t) ->
                database.mostPlayedAlbums(
                    statToPeriod(selection, t),
                    limit = -1,
                    toTimeStamp = toTimestamp(selection, t),
                )
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val listeningByHour: StateFlow<List<ListeningBySlot>> =
        periodPair()
            .flatMapLatest { (selection, t) ->
                database.listeningByHour(
                    fromTimeStamp = statToPeriod(selection, t),
                    toTimeStamp = toTimestamp(selection, t),
                )
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val listeningByDayOfWeek: StateFlow<List<ListeningBySlot>> =
        periodPair()
            .flatMapLatest { (selection, t) ->
                database.listeningByDayOfWeek(
                    fromTimeStamp = statToPeriod(selection, t),
                    toTimeStamp = toTimestamp(selection, t),
                )
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val listeningTotals: StateFlow<ListeningTotals> =
        periodPair()
            .flatMapLatest { (selection, t) ->
                database.listeningTotals(
                    fromTimeStamp = statToPeriod(selection, t),
                    toTimeStamp = toTimestamp(selection, t),
                )
            }.stateIn(viewModelScope, SharingStarted.Lazily, ListeningTotals(0, 0L))

    val firstEvent: StateFlow<EventWithSong?> =
        database
            .firstEvent()
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val selectedArtists = mutableStateListOf<Artist>() // Current artist selection

    val filteredSongs = combine(
        mostPlayedSongsStats,
        snapshotFlow { selectedArtists.toList() }
    ) { songs, selected ->
        if (selected.isEmpty()) {
            songs
        } else {
            songs.filter { song ->
                song.artists.any { artist -> selected.any { it.id == artist.id } }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredArtists = combine(
        mostPlayedArtists,
        snapshotFlow { selectedArtists.toList() }
    ) { artists, selected ->
        if (selected.isEmpty()) {
            artists
        } else {
            artists.filter { artist ->
                selected.any { it.id == artist.artist.id }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredAlbums = combine(
        mostPlayedAlbums,
        snapshotFlow { selectedArtists.toList() }
    ) { albums, selected ->
        if (selected.isEmpty()) {
            albums
        } else {
            albums.filter { album ->
                album.artists.any { artist ->
                    selected.any { it.id == artist.id }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val primaryStats =
        combine(
            mostPlayedSongsStats,
            mostPlayedSongs,
            mostPlayedArtists,
            mostPlayedAlbums,
        ) { rankedSongs, songs, artists, albums ->
            PrimaryStats(
                rankedSongs = rankedSongs,
                songs = songs,
                artists = artists,
                albums = albums,
            )
        }

    private val listeningStats =
        combine(
            listeningByHour,
            listeningByDayOfWeek,
            listeningTotals,
            firstEvent,
        ) { byHour, byDay, totals, first ->
            ListeningStats(
                byHour = byHour,
                byDay = byDay,
                totals = totals,
                firstEvent = first,
            )
        }

    val screenState: StateFlow<StatsScreenState> =
        refreshRequest
            .flatMapLatest {
                combine(
                    primaryStats,
                    listeningStats,
                    selectedOption,
                    indexChips,
                    isSongListExpanded,
                ) { primary, listening, option, periodIndex, expanded ->
                    val summary =
                        ListeningSummary(
                            totalPlayCount = listening.totals.totalPlayCount,
                            totalTimeListened = listening.totals.totalTimeListened,
                            uniqueSongsCount = primary.rankedSongs.size,
                            uniqueArtistsCount = primary.artists.size,
                            uniqueAlbumsCount = primary.albums.size,
                        )
                    if (summary.totalPlayCount == 0 && primary.rankedSongs.isEmpty()) {
                        StatsScreenState.Empty
                    } else {
                        StatsScreenState.Success(
                            StatsUiData(
                                selectedOption = option,
                                selectedPeriodIndex = periodIndex,
                                mostPlayedSongs = primary.songs,
                                visibleRankedSongs =
                                    if (expanded) {
                                        primary.rankedSongs
                                    } else {
                                        primary.rankedSongs.take(COLLAPSED_SONG_COUNT)
                                    },
                                rankedSongCount = primary.rankedSongs.size,
                                mostPlayedArtists = primary.artists,
                                mostPlayedAlbums = primary.albums,
                                listeningByHour = listening.byHour,
                                listeningByDayOfWeek = listening.byDay,
                                listeningSummary = summary,
                                firstEvent = listening.firstEvent,
                                isSongListExpanded = expanded,
                                canExpandSongList = primary.rankedSongs.size > COLLAPSED_SONG_COUNT,
                            ),
                        )
                    }
                }.catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    reportException(throwable)
                    emit(StatsScreenState.Error(R.string.error_unknown))
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = StatsScreenState.Loading,
            )

    fun transferSongStats(fromSongId: String, toSongId: String, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                database.transferSongStats(fromSongId, toSongId)
                syncMostPlaylistsIfNeeded(force = true)
                onDone?.invoke()
            } catch (t: Throwable) {
                reportException(t)
            }
        }
    }

    val weeklyMostPlaylist =
        showMostStatsPlaylists.flatMapLatest { isEnabled ->
            if (isEnabled) {
                database.playlist(PlaylistEntity.WEEKLY_MOST_PLAYLIST_ID)
            } else {
                flowOf(null)
            }
        }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val monthlyMostPlaylist =
        showMostStatsPlaylists.flatMapLatest { isEnabled ->
            if (isEnabled) {
                database.playlist(PlaylistEntity.MONTHLY_MOST_PLAYLIST_ID)
            } else {
                flowOf(null)
            }
        }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val recapPlaylists =
        database
            .playlistsByNameAsc()
            .map { playlists ->
                playlists.filter { playlist ->
                    playlist.playlist.browseId != null &&
                        playlist.playlist.name.contains("recap", ignoreCase = true)
                }
            }.distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun syncMostPlaylistsIfNeeded(force: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            periodicMostPlaylistSyncMutex.withLock {
                val now = LocalDateTime.now()
                val nowEpochMillis = System.currentTimeMillis()
                val preferences = context.dataStore.data.first()
                val hideVideoSongs = preferences[HideVideoSongsKey] ?: false
                val shouldShowMostStatsPlaylists = preferences[ShowMostStatsPlaylistsKey] ?: true

                if (!shouldShowMostStatsPlaylists) {
                    clearMostPlaylists()
                    return@withLock
                }

                val weeklyPlaylistExists =
                    database.playlist(PlaylistEntity.WEEKLY_MOST_PLAYLIST_ID).first() != null
                val monthlyPlaylistExists =
                    database.playlist(PlaylistEntity.MONTHLY_MOST_PLAYLIST_ID).first() != null

                val shouldSyncWeekly =
                    force || !weeklyPlaylistExists || isWeeklySyncDue(
                        lastSyncMillis = preferences[LastWeeklyMostPlaylistSyncKey],
                        now = now,
                    )
                val shouldSyncMonthly =
                    force || !monthlyPlaylistExists || isMonthlySyncDue(
                        lastSyncMillis = preferences[LastMonthlyMostPlaylistSyncKey],
                        now = now,
                    )

                if (!shouldSyncWeekly && !shouldSyncMonthly) {
                    return@withLock
                }

                if (shouldSyncWeekly) {
                    syncMostPlaylist(
                        playlistId = PlaylistEntity.WEEKLY_MOST_PLAYLIST_ID,
                        playlistName = context.getString(R.string.weekly_most_playlist_name),
                        fromTimeStamp = StatPeriod.WEEK_1.toLocalDateTime(),
                        hideVideoSongs = hideVideoSongs,
                        now = now,
                    )
                }

                if (shouldSyncMonthly) {
                    syncMostPlaylist(
                        playlistId = PlaylistEntity.MONTHLY_MOST_PLAYLIST_ID,
                        playlistName = context.getString(R.string.monthly_most_playlist_name),
                        fromTimeStamp = StatPeriod.MONTH_1.toLocalDateTime(),
                        hideVideoSongs = hideVideoSongs,
                        now = now,
                    )
                }

                if (!force) {
                    context.safeDataStoreEdit { settings ->
                        if (shouldSyncWeekly) settings[LastWeeklyMostPlaylistSyncKey] = nowEpochMillis
                        if (shouldSyncMonthly) settings[LastMonthlyMostPlaylistSyncKey] = nowEpochMillis
                    }
                }
            }
        }
    }

    private suspend fun clearMostPlaylists() {
        database.withTransaction {
            clearPlaylist(PlaylistEntity.WEEKLY_MOST_PLAYLIST_ID)
            clearPlaylist(PlaylistEntity.MONTHLY_MOST_PLAYLIST_ID)
            delete(
                PlaylistEntity(
                    id = PlaylistEntity.WEEKLY_MOST_PLAYLIST_ID,
                    name = "",
                ),
            )
            delete(
                PlaylistEntity(
                    id = PlaylistEntity.MONTHLY_MOST_PLAYLIST_ID,
                    name = "",
                ),
            )
        }
    }

    private fun isWeeklySyncDue(
        lastSyncMillis: Long?,
        now: LocalDateTime,
    ): Boolean {
        if (lastSyncMillis == null || lastSyncMillis <= 0L) return true
        val lastSyncAt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(lastSyncMillis), java.time.ZoneId.systemDefault())
        return !lastSyncAt.plusWeeks(1).isAfter(now)
    }

    private fun isMonthlySyncDue(
        lastSyncMillis: Long?,
        now: LocalDateTime,
    ): Boolean {
        if (lastSyncMillis == null || lastSyncMillis <= 0L) return true
        val lastSyncAt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(lastSyncMillis), java.time.ZoneId.systemDefault())
        return !lastSyncAt.plusMonths(1).isAfter(now)
    }

    private suspend fun syncMostPlaylist(
        playlistId: String,
        playlistName: String,
        fromTimeStamp: LocalDateTime,
        hideVideoSongs: Boolean,
        now: LocalDateTime,
    ) {
        val songs =
            database
                .mostPlayedSongs(
                    fromTimeStamp = fromTimeStamp,
                    limit = -1,
                    toTimeStamp = now,
                ).first()
                .let { mostPlayedSongs ->
                    if (hideVideoSongs) {
                        mostPlayedSongs.filter { !it.song.isVideo }
                    } else {
                        mostPlayedSongs
                    }
                }.distinctBy { it.song.id }

        val existingPlaylist = database.playlist(playlistId).first()?.playlist
        val playlistEntity =
            existingPlaylist?.copy(
                name = playlistName,
                isEditable = true,
                bookmarkedAt = existingPlaylist.bookmarkedAt ?: now,
                lastUpdateTime = now,
            ) ?: PlaylistEntity(
                id = playlistId,
                name = playlistName,
                isEditable = true,
                bookmarkedAt = now,
                lastUpdateTime = now,
            )

        if (existingPlaylist == null) {
            database.insert(playlistEntity)
        } else {
            database.update(playlistEntity)
        }

        database.clearPlaylist(playlistId)

        val fullPlaylist = database.playlist(playlistId).first()
        if (fullPlaylist != null) {
            database.addSongsToPlaylist(fullPlaylist, songs.map { it.id to null })
        }
    }

    init {
        viewModelScope.launch {
            mostPlayedArtists.collect { artists ->
                artists
                    .map { it.artist }
                    .filter {
                        it.thumbnailUrl == null || Duration.between(
                            it.lastUpdateTime,
                            LocalDateTime.now()
                        ) > Duration.ofDays(10)
                    }.forEach { artist ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                    }
            }
        }
        viewModelScope.launch {
            mostPlayedAlbums.collect { albums ->
                albums
                    .filter {
                        it.album.songCount == 0
                    }.forEach { album ->
                        YouTube
                            .album(album.id)
                            .onSuccess { albumPage ->
                                database.query {
                                    update(album.album, albumPage, album.artists)
                                }
                            }.onFailure {
                                reportException(it)
                                if (it.message?.contains("NOT_FOUND") == true) {
                                    database.query {
                                        delete(album.album)
                                    }
                                }
                            }
                    }
            }
        }
    }

    private data class PrimaryStats(
        val rankedSongs: List<SongWithStats>,
        val songs: List<Song>,
        val artists: List<DbArtist>,
        val albums: List<Album>,
    )

    private data class ListeningStats(
        val byHour: List<ListeningBySlot>,
        val byDay: List<ListeningBySlot>,
        val totals: ListeningTotals,
        val firstEvent: EventWithSong?,
    )

    private companion object {
        const val COLLAPSED_SONG_COUNT = 5
    }
}
