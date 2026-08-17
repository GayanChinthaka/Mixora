/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.ui.screens

import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.pokerlanka.mixora.constants.DarkModeKey
import com.pokerlanka.mixora.constants.PureBlackKey
import com.pokerlanka.mixora.ui.screens.artist.ArtistAlbumsScreen
import com.pokerlanka.mixora.ui.screens.artist.ArtistItemsScreen
import com.pokerlanka.mixora.ui.screens.artist.ArtistScreen
import com.pokerlanka.mixora.ui.screens.artist.ArtistSongsScreen
import com.pokerlanka.mixora.ui.screens.equalizer.EqScreen
import com.pokerlanka.mixora.ui.screens.equalizer.wizard.WizardScreen
import com.pokerlanka.mixora.ui.screens.library.LibraryScreen
import com.pokerlanka.mixora.ui.screens.playlist.AutoPlaylistScreen
import com.pokerlanka.mixora.ui.screens.playlist.CachePlaylistScreen
import com.pokerlanka.mixora.ui.screens.playlist.LocalPlaylistScreen
import com.pokerlanka.mixora.ui.screens.playlist.OnlinePlaylistScreen
import com.pokerlanka.mixora.ui.screens.playlist.TopPlaylistScreen
import com.pokerlanka.mixora.ui.screens.podcast.OnlinePodcastScreen
import com.pokerlanka.mixora.ui.screens.recognition.RecognitionHistoryScreen
import com.pokerlanka.mixora.ui.screens.recognition.RecognitionScreen
import com.pokerlanka.mixora.ui.screens.search.OnlineSearchResult
import com.pokerlanka.mixora.ui.screens.search.SearchScreen
import com.pokerlanka.mixora.ui.screens.settings.AboutScreen
import com.pokerlanka.mixora.ui.screens.settings.AccountSettings
import com.pokerlanka.mixora.ui.screens.settings.AiIntegrationSettings
import com.pokerlanka.mixora.ui.screens.settings.AndroidAutoSettings
import com.pokerlanka.mixora.ui.screens.settings.AppearanceSettings
import com.pokerlanka.mixora.ui.screens.settings.BackupAndRestore
import com.pokerlanka.mixora.ui.screens.settings.ContentSettings
import com.pokerlanka.mixora.ui.screens.settings.DarkMode
import com.pokerlanka.mixora.ui.screens.settings.InternetSettings
import com.pokerlanka.mixora.ui.screens.settings.MiscSettings
import com.pokerlanka.mixora.ui.screens.settings.MusicTogetherScreen
import com.pokerlanka.mixora.ui.screens.settings.PlayerSettings
import com.pokerlanka.mixora.ui.screens.settings.PoTokenGenerationScreen
import com.pokerlanka.mixora.ui.screens.settings.PrivacySettings
import com.pokerlanka.mixora.ui.screens.settings.RomanizationSettings
import com.pokerlanka.mixora.ui.screens.settings.SettingsScreen
import com.pokerlanka.mixora.ui.screens.settings.StorageSettings
import com.pokerlanka.mixora.ui.screens.settings.StreamSourcesSettings
import com.pokerlanka.mixora.ui.screens.settings.ThemeScreen
import com.pokerlanka.mixora.ui.screens.settings.UpdateScreen
import com.pokerlanka.mixora.ui.screens.settings.integrations.IntegrationScreen
import com.pokerlanka.mixora.ui.screens.settings.integrations.LastFMSettings

import com.pokerlanka.mixora.ui.screens.wrapped.WrappedScreen
import com.pokerlanka.mixora.utils.rememberEnumPreference
import com.pokerlanka.mixora.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
    activity: Activity,
    snackbarHostState: SnackbarHostState,
) {
    composable(Screens.Home.route) {
        HomeScreen(snackbarHostState = snackbarHostState)
    }

    composable(Screens.Search.route) { backStackEntry ->
        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
        val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
        val isSystemInDarkTheme = isSystemInDarkTheme()
        val useDarkTheme =
            remember(darkTheme, isSystemInDarkTheme) {
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            }
        val pureBlack =
            remember(pureBlackEnabled, useDarkTheme) {
                pureBlackEnabled && useDarkTheme
            }
        SearchScreen(
            pureBlack = pureBlack,
            savedStateHandle = backStackEntry.savedStateHandle
        )
    }

    composable(Screens.Library.route) {
        LibraryScreen()
    }

    composable("history") {
        HistoryScreen(navController)
    }

    composable("stats") {
        StatsScreen(navController)
    }

    composable("mood_and_genres") {
        MoodAndGenresScreen(navController)
    }

    composable("account") {
        AccountScreen(navController)
    }

    composable("new_release") {
        NewReleaseScreen(navController)
    }

    composable("charts_screen") {
        ChartsScreen(navController)
    }

    composable(
        route = "browse/{browseId}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                },
            ),
    ) {
        BrowseScreen(
            navController,
            it.arguments?.getString("browseId"),
        )
    }

    composable(
        route = "search/{query}",
        arguments =
            listOf(
                navArgument("query") {
                    type = NavType.StringType
                },
            ),
        enterTransition = {
            fadeIn(tween(250))
        },
        exitTransition = {
            if (targetState.destination.route?.startsWith("search/") == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (initialState.destination.route?.startsWith("search/") == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            fadeOut(tween(200))
        },
    ) { backStackEntry ->
        OnlineSearchResult(
            savedStateHandle = backStackEntry.savedStateHandle
        )

    }

    composable(
        route = "album/{albumId}",
        arguments =
            listOf(
                navArgument("albumId") {
                    type = NavType.StringType
                },
            ),
    ) {
        AlbumScreen(navController)
    }

    composable(
        route = "artist/{artistId}?isPodcastChannel={isPodcastChannel}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
                navArgument("isPodcastChannel") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
    ) {
        ArtistScreen(navController)
    }

    composable(
        route = "artist/{artistId}/songs",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistSongsScreen(navController)
    }

    composable(
        route = "artist/{artistId}/albums",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistAlbumsScreen(navController, scrollBehavior)
    }

    composable(
        route = "artist/{artistId}/items?browseId={browseId}?params={params}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        ArtistItemsScreen(navController)
    }

    composable(
        route = "online_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        OnlinePlaylistScreen(navController)
    }

    composable(
        route = "online_podcast/{podcastId}",
        arguments =
            listOf(
                navArgument("podcastId") {
                    type = NavType.StringType
                },
            ),
    ) {
        OnlinePodcastScreen(navController, scrollBehavior)
    }

    composable(
        route = "local_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        LocalPlaylistScreen(navController)
    }

    composable(
        route = "auto_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        AutoPlaylistScreen(navController)
    }

    composable(
        route = "cache_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        CachePlaylistScreen(navController)
    }

    composable(
        route = "top_playlist/{top}",
        arguments =
            listOf(
                navArgument("top") {
                    type = NavType.StringType
                },
            ),
    ) {
        TopPlaylistScreen(navController)
    }

    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        YouTubeBrowseScreen(navController)
    }

    composable("settings") {
        SettingsScreen(navController, latestVersionName)
    }

    composable("settings/appearance") {
        AppearanceSettings(navController, activity, snackbarHostState)
    }

    composable("settings/appearance/theme") {
        ThemeScreen(navController)
    }

    composable("settings/content") {
        ContentSettings(navController)
    }

    composable("settings/content/romanization") {
        RomanizationSettings(navController)
    }

    composable("settings/player") {
        PlayerSettings(navController)
    }

    composable("settings/stream_sources") {
        StreamSourcesSettings(navController)
    }

    composable("settings/storage") {
        StorageSettings(navController)
    }

    composable("settings/privacy") {
        PrivacySettings(navController)
    }

    composable("settings/backup_restore") {
        BackupAndRestore(navController)
    }

    composable("settings/account") {
        AccountSettings(
            navController = navController,
            latestVersionName = latestVersionName
        )
    }

    composable("settings/integrations") {
        IntegrationScreen(navController)
    }

    composable("settings/integrations/lastfm") {
        LastFMSettings(navController)
    }

    composable("settings/about") {
        AboutScreen(navController)
    }

    composable("settings/ai_integration") {
        AiIntegrationSettings(navController)
    }

    composable("settings/music_together") {
        MusicTogetherScreen(navController)
    }

    composable("settings/internet") {
        InternetSettings(navController)
    }

    composable("settings/po_token_generation") {
        PoTokenGenerationScreen(navController)
    }

    composable("settings/misc") {
        MiscSettings(navController)
    }

    composable("settings/updates") {
        UpdateScreen(navController)
    }

    composable("login") {
        LoginScreen(navController)
    }

    composable("wrapped") {
        WrappedScreen()
    }

    composable("equalizer") {
        EqScreen()
    }

    composable("eq_wizard") {
        WizardScreen(onNavigateBack = {
            navController.popBackStack()
        })
    }

    composable(
        route = "recognition?autoStart={autoStart}",
        arguments =
            listOf(
                navArgument("autoStart") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
    ) {
        RecognitionScreen(navController, it.arguments?.getBoolean("autoStart") ?: false)
    }

    composable("recognition_history") {
        RecognitionHistoryScreen(navController)
    }
    composable("settings/android_auto") {
        AndroidAutoSettings(navController)
    }
}
