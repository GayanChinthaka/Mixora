/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavController
import com.pokerlanka.mixora.LocalPlayerAwareWindowInsets
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.constants.CropAlbumArtKey
import com.pokerlanka.mixora.constants.CustomThemeColorKey
import com.pokerlanka.mixora.constants.DarkModeKey
import com.pokerlanka.mixora.constants.DensityScale
import com.pokerlanka.mixora.constants.DensityScaleKey
import com.pokerlanka.mixora.constants.DynamicThemeKey
import com.pokerlanka.mixora.constants.EnableLandscapeScalingKey
import com.pokerlanka.mixora.ui.theme.ThemePalettes
import com.pokerlanka.mixora.ui.theme.ThemeSeedPaletteCodec
import com.pokerlanka.mixora.ui.theme.toThemePalette
import com.pokerlanka.mixora.constants.HidePlayerThumbnailKey
import com.pokerlanka.mixora.constants.LyricsClickKey
import com.pokerlanka.mixora.constants.LyricsLineSpacingKey
import com.pokerlanka.mixora.constants.LyricsScrollKey
import com.pokerlanka.mixora.constants.LyricsTextPositionKey
import com.pokerlanka.mixora.constants.LyricsTextSizeKey
import com.pokerlanka.mixora.constants.MiniPlayerBackgroundStyle
import com.pokerlanka.mixora.constants.MiniPlayerBackgroundStyleKey
import com.pokerlanka.mixora.constants.PlayerBackgroundStyle
import com.pokerlanka.mixora.constants.PlayerBackgroundStyleKey
import com.pokerlanka.mixora.constants.PlayerButtonsStyle
import com.pokerlanka.mixora.constants.PlayerButtonsStyleKey
import com.pokerlanka.mixora.constants.PureBlackMiniPlayerKey
import com.pokerlanka.mixora.constants.RespectAgentPositioningKey
import com.pokerlanka.mixora.constants.SelectedThemeColorKey
import com.pokerlanka.mixora.constants.ShowCachedPlaylistKey
import com.pokerlanka.mixora.constants.ShowDownloadedPlaylistKey
import com.pokerlanka.mixora.constants.ShowLikedPlaylistKey
import com.pokerlanka.mixora.constants.ShowTopPlaylistKey
import com.pokerlanka.mixora.constants.ShowUploadedPlaylistKey
import com.pokerlanka.mixora.constants.SliderStyle
import com.pokerlanka.mixora.constants.SliderStyleKey
import com.pokerlanka.mixora.constants.UseNewMiniPlayerDesignKey
import com.pokerlanka.mixora.constants.UseNewPlayerDesignKey
import com.pokerlanka.mixora.ui.component.DefaultDialog
import com.pokerlanka.mixora.ui.component.EnumDialog
import com.pokerlanka.mixora.ui.component.IconButton
import com.pokerlanka.mixora.ui.component.Material3SettingsGroup
import com.pokerlanka.mixora.ui.component.Material3SettingsItem
import com.pokerlanka.mixora.ui.component.PlayerSliderTrack
import com.pokerlanka.mixora.ui.theme.DefaultThemeColor
import com.pokerlanka.mixora.ui.theme.PlayerSliderColors
import com.pokerlanka.mixora.ui.utils.backToMain
import com.pokerlanka.mixora.utils.rememberEnumPreference
import com.pokerlanka.mixora.utils.rememberPreference
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings(
    navController: NavController,
    activity: Activity,
    snackbarHostState: SnackbarHostState,
) {
    val (darkMode) =
        rememberEnumPreference(
            DarkModeKey,
            defaultValue = DarkMode.AUTO,
        )
    val (customThemeColor) =
        rememberPreference(
            CustomThemeColorKey,
            defaultValue = ThemePalettes.Default.id,
        )
    val (sliderStyle, onSliderStyleChange) =
        rememberEnumPreference(
            SliderStyleKey,
            defaultValue = SliderStyle.SLIM,
        )

    // Density scale preferences
    val context = activity as Context
    val sharedPreferences = remember { context.getSharedPreferences("Mixora_settings", Context.MODE_PRIVATE) }
    val prefDensityScale =
        remember(sharedPreferences) {
            sharedPreferences.getFloat("density_scale_factor", 1.0f)
        }
    val (densityScale, setDensityScale) = rememberPreference(DensityScaleKey, defaultValue = prefDensityScale)
    var showRestartDialog by rememberSaveable { mutableStateOf(false) }
    var showDensityScaleDialog by rememberSaveable { mutableStateOf(false) }

    val onDensityScaleChange: (Float) -> Unit = { newScale ->
        setDensityScale(newScale)
        // Write to SharedPreferences for DensityScaler to read on next startup
        sharedPreferences.edit {
            putFloat("density_scale_factor", newScale)
        }
        showRestartDialog = true
    }

    val (showLikedPlaylist, onShowLikedPlaylistChange) =
        rememberPreference(
            ShowLikedPlaylistKey,
            defaultValue = true,
        )
    val (showDownloadedPlaylist, onShowDownloadedPlaylistChange) =
        rememberPreference(
            ShowDownloadedPlaylistKey,
            defaultValue = true,
        )
    val (showTopPlaylist, onShowTopPlaylistChange) =
        rememberPreference(
            ShowTopPlaylistKey,
            defaultValue = true,
        )
    val (showCachedPlaylist, onShowCachedPlaylistChange) =
        rememberPreference(
            ShowCachedPlaylistKey,
            defaultValue = true,
        )
    val (showUploadedPlaylist, onShowUploadedPlaylistChange) =
        rememberPreference(
            ShowUploadedPlaylistKey,
            defaultValue = true,
        )

    var showSliderOptionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showRestartDialog) {
        DefaultDialog(
            onDismiss = { showRestartDialog = false },
            buttons = {
                TextButton(
                    onClick = { showRestartDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showRestartDialog = false
                        val intent =
                            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                        context.startActivity(intent)
                        Runtime.getRuntime().exit(0)
                    },
                ) {
                    Text(text = stringResource(R.string.restart))
                }
            },
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.restart_required),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.density_restart_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    if (showDensityScaleDialog) {
        DefaultDialog(
            onDismiss = { showDensityScaleDialog = false },
            buttons = {
                TextButton(
                    onClick = { showDensityScaleDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
        ) {
            Column {
                DensityScale.entries.forEach { scale ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDensityScaleChange(scale.value)
                                    showDensityScaleDialog = false
                                }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = scale.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color =
                                if (densityScale == scale.value) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        )
                    }
                }
            }
        }
    }

    if (showSliderOptionDialog) {
        DefaultDialog(
            buttons = {
                TextButton(
                    onClick = { showSliderOptionDialog = false },
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showSliderOptionDialog = false
            },
        ) {
            val sliderPreviewColors =
                PlayerSliderColors.getSliderColors(
                    MaterialTheme.colorScheme.primary,
                    PlayerBackgroundStyle.DEFAULT,
                    isSystemInDarkTheme(),
                )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Slim
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier =
                        Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (sliderStyle == SliderStyle.SLIM) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                RoundedCornerShape(16.dp),
                            ).clickable {
                                onSliderStyleChange(SliderStyle.SLIM)
                                showSliderOptionDialog = false
                            }.padding(12.dp),
                ) {
                    val sliderValue = 0.65f
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = { /* preview only */ },
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                        track = { sliderState ->
                            PlayerSliderTrack(
                                sliderState = sliderState,
                                colors = sliderPreviewColors,
                                style = SliderStyle.SLIM,
                                trackHeight = 3.dp,
                            )
                        },
                        colors = sliderPreviewColors,
                        enabled = false,
                        modifier = Modifier.weight(1f),
                    )

                    Text(
                        text = stringResource(R.string.slim),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Ball
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier =
                        Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (sliderStyle == SliderStyle.BALL) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                RoundedCornerShape(16.dp),
                            ).clickable {
                                onSliderStyleChange(SliderStyle.BALL)
                                showSliderOptionDialog = false
                            }.padding(12.dp),
                ) {
                    val sliderValue = 0.5f
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = { /* preview only */ },
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                        track = { sliderState ->
                            PlayerSliderTrack(
                                sliderState = sliderState,
                                colors = sliderPreviewColors,
                                style = SliderStyle.BALL,
                                trackHeight = 3.dp,
                            )
                        },
                        colors = sliderPreviewColors,
                        enabled = false,
                        modifier = Modifier.weight(1f),
                    )

                    Text(
                        text = stringResource(R.string.ball),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Line
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier =
                        Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                1.dp,
                                if (sliderStyle == SliderStyle.LINE) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                RoundedCornerShape(16.dp),
                            ).clickable {
                                onSliderStyleChange(SliderStyle.LINE)
                                showSliderOptionDialog = false
                            }.padding(12.dp),
                ) {
                    val sliderValue = 0.35f
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = { /* preview only */ },
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                        track = { sliderState ->
                            PlayerSliderTrack(
                                sliderState = sliderState,
                                colors = sliderPreviewColors,
                                style = SliderStyle.LINE,
                                trackHeight = 3.dp,
                            )
                        },
                        colors = sliderPreviewColors,
                        enabled = false,
                        modifier = Modifier.weight(1f),
                    )

                    Text(
                        text = stringResource(R.string.line),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        val themeModeDesc =
            when (darkMode) {
                DarkMode.AUTO -> stringResource(R.string.theme_system_default)
                DarkMode.ON -> stringResource(R.string.theme_dark)
                DarkMode.OFF -> stringResource(R.string.theme_light)
            }

        val paletteName =
            remember(customThemeColor) {
                val custom = ThemeSeedPaletteCodec.decodeFromPreference(customThemeColor)?.toThemePalette()
                val palette =
                    custom
                        ?: ThemePalettes.findById(customThemeColor)
                        ?: ThemePalettes.findByPrimaryColor(customThemeColor)
                        ?: ThemePalettes.Default
                if (palette.id.startsWith("custom") || palette.id.startsWith("random_") || customThemeColor.startsWith("seedPalette:")) {
                    ThemeSeedPaletteCodec.extractNameFromPreference(customThemeColor) ?: context.getString(R.string.palette_custom)
                } else {
                    context.getString(palette.nameResId)
                }
            }

        Material3SettingsGroup(
            title = stringResource(R.string.theme),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.dark_mode),
                        title = { Text(stringResource(R.string.theme)) },
                        description = { Text(themeModeDesc) },
                        onClick = { navController.navigate("settings/appearance/theme") },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.format_paint),
                        title = { Text(stringResource(R.string.color_palette)) },
                        description = { Text(paletteName) },
                        onClick = { navController.navigate("settings/appearance/palette_picker") },
                    ),
                ),
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.misc),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.sliders),
                        title = { Text(stringResource(R.string.player_slider_style)) },
                        description = {
                            Text(
                                when (sliderStyle) {
                                    SliderStyle.SLIM -> {
                                        stringResource(R.string.slim)
                                    }

                                    SliderStyle.BALL -> {
                                        stringResource(R.string.ball)
                                    }

                                    SliderStyle.LINE -> {
                                        stringResource(R.string.line)
                                    }
                                },
                            )
                        },
                        onClick = { showSliderOptionDialog = true },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.grid_view),
                        title = { Text(stringResource(R.string.display_density)) },
                        description = {
                            Text(DensityScale.fromValue(densityScale).label)
                        },
                        onClick = { showDensityScaleDialog = true },
                    ),
                ),
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.auto_playlists),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.favorite),
                        title = { Text(stringResource(R.string.show_liked_playlist)) },
                        trailingContent = {
                            Switch(
                                checked = showLikedPlaylist,
                                onCheckedChange = onShowLikedPlaylistChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (showLikedPlaylist) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onShowLikedPlaylistChange(!showLikedPlaylist) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.offline),
                        title = { Text(stringResource(R.string.show_downloaded_playlist)) },
                        trailingContent = {
                            Switch(
                                checked = showDownloadedPlaylist,
                                onCheckedChange = onShowDownloadedPlaylistChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (showDownloadedPlaylist) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onShowDownloadedPlaylistChange(!showDownloadedPlaylist) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.trending_up),
                        title = { Text(stringResource(R.string.show_top_playlist)) },
                        trailingContent = {
                            Switch(
                                checked = showTopPlaylist,
                                onCheckedChange = onShowTopPlaylistChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (showTopPlaylist) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onShowTopPlaylistChange(!showTopPlaylist) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.cached),
                        title = { Text(stringResource(R.string.show_cached_playlist)) },
                        trailingContent = {
                            Switch(
                                checked = showCachedPlaylist,
                                onCheckedChange = onShowCachedPlaylistChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (showCachedPlaylist) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onShowCachedPlaylistChange(!showCachedPlaylist) },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.backup),
                        title = { Text(stringResource(R.string.show_uploaded_playlist)) },
                        trailingContent = {
                            Switch(
                                checked = showUploadedPlaylist,
                                onCheckedChange = onShowUploadedPlaylistChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (showUploadedPlaylist) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onShowUploadedPlaylistChange(!showUploadedPlaylist) },
                    ),
                ),
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.appearance)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}

enum class DarkMode {
    ON,
    OFF,
    AUTO,
}

enum class NavigationTab {
    HOME,
    SEARCH,
    LIBRARY,
}

enum class PlayerTextAlignment {
    SIDED,
    CENTER,
}
