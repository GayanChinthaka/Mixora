/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pokerlanka.mixora.LocalPlayerAwareWindowInsets
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.constants.EnableBetterLyricsKey
import com.pokerlanka.mixora.constants.AiRomanizationEnabledKey
import com.pokerlanka.mixora.constants.EnableKugouKey
import com.pokerlanka.mixora.constants.EnableLrcLibKey
import com.pokerlanka.mixora.constants.EnablePaxsenixKey
import com.pokerlanka.mixora.constants.EnableLyricsPlus
import com.pokerlanka.mixora.constants.HideStatusBarOnFullscreenKey
import com.pokerlanka.mixora.constants.LyricsBackgroundStyle
import com.pokerlanka.mixora.constants.LyricsBackgroundStyleKey
import com.pokerlanka.mixora.constants.LyricsClickKey
import com.pokerlanka.mixora.constants.LyricsLineSpacingKey
import com.pokerlanka.mixora.constants.LyricsProviderOrderKey
import com.pokerlanka.mixora.constants.LyricsScrollKey
import com.pokerlanka.mixora.constants.LyricsTextPositionKey
import com.pokerlanka.mixora.constants.LyricsTextSizeKey
import com.pokerlanka.mixora.constants.RespectAgentPositioningKey
import com.pokerlanka.mixora.lyrics.LyricsProviderRegistry
import com.pokerlanka.mixora.ui.component.DefaultDialog
import com.pokerlanka.mixora.ui.component.DraggableLyricsProviderItem
import com.pokerlanka.mixora.ui.component.DraggableLyricsProviderList
import com.pokerlanka.mixora.ui.component.EnumDialog
import com.pokerlanka.mixora.ui.component.IconButton
import com.pokerlanka.mixora.ui.component.Material3SettingsGroup
import com.pokerlanka.mixora.ui.component.Material3SettingsItem
import com.pokerlanka.mixora.ui.utils.backToMain
import com.pokerlanka.mixora.utils.rememberEnumPreference
import com.pokerlanka.mixora.utils.rememberPreference
import java.util.Locale
import kotlin.math.roundToInt

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSettings(
    navController: NavController,
) {
    val (enableKugou, onEnableKugouChange) = rememberPreference(key = EnableKugouKey, defaultValue = true)
    val (enableLrclib, onEnableLrclibChange) = rememberPreference(key = EnableLrcLibKey, defaultValue = true)
    val (enableBetterLyrics, onEnableBetterLyricsChange) = rememberPreference(key = EnableBetterLyricsKey, defaultValue = true)
    val (enablePaxsenix, onEnablePaxsenixChange) = rememberPreference(key = EnablePaxsenixKey, defaultValue = true)
    val (enableLyricsPlus, onEnableLyricsPlusChange) = rememberPreference(key = EnableLyricsPlus, defaultValue = true)
    val (lyricsProviderOrder, onLyricsProviderOrderChange) = rememberPreference(
        key = LyricsProviderOrderKey,
        defaultValue = LyricsProviderRegistry.serializeProviderOrder(LyricsProviderRegistry.getDefaultProviderOrder())
    )

    val (lyricsPosition, onLyricsPositionChange) =
        rememberEnumPreference(
            LyricsTextPositionKey,
            defaultValue = LyricsPosition.CENTER,
        )
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (lyricsScroll, onLyricsScrollChange) =
        rememberPreference(
            LyricsScrollKey,
            defaultValue = true,
        )
    val (hideStatusBarOnFullscreen, onHideStatusBarOnFullscreenChange) =
        rememberPreference(
            HideStatusBarOnFullscreenKey,
            defaultValue = false,
        )
    val (respectAgentPositioning, onRespectAgentPositioningChange) = rememberPreference(RespectAgentPositioningKey, defaultValue = true)

    val (lyricsBackgroundStyle, onLyricsBackgroundStyleChange) =
        rememberEnumPreference(
            LyricsBackgroundStyleKey,
            defaultValue = LyricsBackgroundStyle.THEME,
        )
    // Single switch for romanization now that the dedicated Romanization screen is gone. The
    // provider/model live under Settings > Account > AI Integration, so nothing is duplicated here.
    val (aiRomanizationEnabled, onAiRomanizationEnabledChange) =
        rememberPreference(AiRomanizationEnabledKey, defaultValue = false)
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, defaultValue = 36f)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.3f)

    var showLyricsBackgroundStyleDialog by remember { mutableStateOf(false) }
    var showLyricsTextSizeDialog by remember { mutableStateOf(false) }
    var showLyricsLineSpacingDialog by remember { mutableStateOf(false) }
    var showLyricsPositionDialog by remember { mutableStateOf(false) }
    var showProviderDialog by rememberSaveable { mutableStateOf(false) }

    val providerDisplayNames =
        mapOf(
            "BetterLyrics" to "Better Lyrics",
            "Paxsenix" to "Paxsenix",
            "LrcLib" to "LrcLib",
            "KuGou" to "KuGou",
            "LyricsPlus" to "LyricsPlus",
            "YouTubeSubtitle" to "YouTube Subtitles",
            "YouTube" to "YouTube",
        )

    if (showProviderDialog) {
        val defaultOrder = LyricsProviderRegistry.getDefaultProviderOrder()
        val currentOrder = LyricsProviderRegistry.deserializeProviderOrder(lyricsProviderOrder)
        val normalizedOrder = currentOrder.filter { it in defaultOrder } +
            defaultOrder.filter { it !in currentOrder }

        // Only these five have an enable preference. The YouTube providers are always-available
        // fallbacks with no switch, so they stay out of the list and keep their trailing position
        // in the saved order (the note under the list explains that).
        val toggleableProviders = listOf("LrcLib", "KuGou", "BetterLyrics", "Paxsenix", "LyricsPlus")

        val isProviderEnabled: (String) -> Boolean = { id ->
            when (id) {
                "LrcLib" -> enableLrclib
                "KuGou" -> enableKugou
                "BetterLyrics" -> enableBetterLyrics
                "Paxsenix" -> enablePaxsenix
                "LyricsPlus" -> enableLyricsPlus
                else -> true
            }
        }
        val setProviderEnabled: (String, Boolean) -> Unit = { id, value ->
            when (id) {
                "LrcLib" -> onEnableLrclibChange(value)
                "KuGou" -> onEnableKugouChange(value)
                "BetterLyrics" -> onEnableBetterLyricsChange(value)
                "Paxsenix" -> onEnablePaxsenixChange(value)
                "LyricsPlus" -> onEnableLyricsPlusChange(value)
                else -> Unit
            }
        }

        val providerDescriptions =
            mapOf(
                "LrcLib" to stringResource(R.string.enable_lrclib_desc),
                "KuGou" to stringResource(R.string.enable_kugou_desc),
                "BetterLyrics" to stringResource(R.string.enable_better_lyrics_desc),
                "Paxsenix" to stringResource(R.string.enable_paxsenix_desc),
                "LyricsPlus" to stringResource(R.string.enable_lyricsplus_desc),
            )
        val lyricsIcon = painterResource(R.drawable.lyrics)
        val draggableItems = remember { mutableStateListOf<DraggableLyricsProviderItem>() }

        // Every toggleable provider is listed regardless of state, so the order can be arranged
        // before one is switched on. The previous priority dialog listed only enabled providers,
        // which made reordering a disabled provider impossible.
        LaunchedEffect(
            normalizedOrder,
            enableLrclib,
            enableKugou,
            enableBetterLyrics,
            enablePaxsenix,
            enableLyricsPlus,
        ) {
            val ordered = normalizedOrder.filter { it in toggleableProviders } +
                toggleableProviders.filter { it !in normalizedOrder }
            draggableItems.clear()
            draggableItems.addAll(
                ordered.mapNotNull { providerName ->
                    LyricsProviderRegistry.getProviderByName(providerName) ?: return@mapNotNull null
                    DraggableLyricsProviderItem(
                        id = providerName,
                        name = providerDisplayNames[providerName] ?: providerName,
                        icon = lyricsIcon,
                        enabled = isProviderEnabled(providerName),
                        description = providerDescriptions[providerName],
                    )
                },
            )
        }

        AlertDialog(
            onDismissRequest = { showProviderDialog = false },
            title = { Text(stringResource(R.string.lyrics_provider_selection)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                ) {
                    Text(
                        stringResource(R.string.lyrics_provider_priority_desc),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    DraggableLyricsProviderList(
                        items = draggableItems,
                        onItemsReordered = { reorderedItems ->
                            // Providers without a switch keep their existing relative order and
                            // stay after the toggleable ones, matching the previous behaviour.
                            val reordered = reorderedItems.map { it.id }
                            val rest = normalizedOrder.filter { it !in toggleableProviders }
                            onLyricsProviderOrderChange(
                                LyricsProviderRegistry.serializeProviderOrder(reordered + rest),
                            )
                        },
                        onToggle = setProviderEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.youtube_music_lyrics_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showProviderDialog = false },
                ) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }

    if (showLyricsPositionDialog) {
        EnumDialog(
            onDismiss = { showLyricsPositionDialog = false },
            onSelect = {
                onLyricsPositionChange(it)
                showLyricsPositionDialog = false
            },
            title = stringResource(R.string.lyrics_text_position),
            current = lyricsPosition,
            values = LyricsPosition.values().toList(),
            valueText = {
                when (it) {
                    LyricsPosition.LEFT -> stringResource(R.string.left)
                    LyricsPosition.CENTER -> stringResource(R.string.center)
                    LyricsPosition.RIGHT -> stringResource(R.string.right)
                }
            },
        )
    }

    if (showLyricsBackgroundStyleDialog) {
        EnumDialog(
            onDismiss = { showLyricsBackgroundStyleDialog = false },
            onSelect = {
                onLyricsBackgroundStyleChange(it)
                showLyricsBackgroundStyleDialog = false
            },
            title = stringResource(R.string.lyrics_background),
            current = lyricsBackgroundStyle,
            values = LyricsBackgroundStyle.values().toList(),
            valueText = {
                when (it) {
                    LyricsBackgroundStyle.THEME -> stringResource(R.string.lyrics_background_theme)
                    LyricsBackgroundStyle.THUMBNAIL -> stringResource(R.string.lyrics_background_thumbnail)
                }
            },
        )
    }

    if (showLyricsTextSizeDialog) {
        var tempTextSize by remember { mutableFloatStateOf(lyricsTextSize) }

        DefaultDialog(
            onDismiss = {
                tempTextSize = lyricsTextSize
                showLyricsTextSizeDialog = false
            },
            buttons = {
                TextButton(
                    onClick = {
                        tempTextSize = 36f
                    },
                ) {
                    Text(stringResource(R.string.reset))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        tempTextSize = lyricsTextSize
                        showLyricsTextSizeDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onLyricsTextSizeChange(tempTextSize)
                        showLyricsTextSizeDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.lyrics_text_size),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                Text(
                    text = "${tempTextSize.roundToInt()} sp",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                Slider(
                    value = tempTextSize,
                    onValueChange = { tempTextSize = it },
                    valueRange = 12f..48f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showLyricsLineSpacingDialog) {
        var tempLineSpacing by remember { mutableFloatStateOf(lyricsLineSpacing) }

        DefaultDialog(
            onDismiss = {
                tempLineSpacing = lyricsLineSpacing
                showLyricsLineSpacingDialog = false
            },
            buttons = {
                TextButton(
                    onClick = {
                        tempLineSpacing = 1.3f
                    },
                ) {
                    Text(stringResource(R.string.reset))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        tempLineSpacing = lyricsLineSpacing
                        showLyricsLineSpacingDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onLyricsLineSpacingChange(tempLineSpacing)
                        showLyricsLineSpacingDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.lyrics_line_spacing),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                Text(
                    text = String.format(Locale.US, "%.1f", tempLineSpacing),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                Slider(
                    value = tempLineSpacing,
                    onValueChange = { tempLineSpacing = it },
                    valueRange = 1.0f..3.0f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lyrics)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Material3SettingsGroup(
                title = stringResource(R.string.lyrics),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.lyrics),
                        title = { Text(stringResource(R.string.lyrics_provider_selection)) },
                        description = { Text(stringResource(R.string.lyrics_providers_desc)) },
                        onClick = { showProviderDialog = true }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.language_korean_latin),
                        title = { Text(stringResource(R.string.lyrics_romanization)) },
                        description = { Text(stringResource(R.string.romanization_ai_enable_desc)) },
                        trailingContent = {
                            Switch(
                                checked = aiRomanizationEnabled,
                                onCheckedChange = onAiRomanizationEnabledChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (aiRomanizationEnabled) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        onClick = { onAiRomanizationEnabledChange(!aiRomanizationEnabled) },
                    )
                )
            )

            Spacer(modifier = Modifier.height(27.dp))

            Material3SettingsGroup(
                items =
                    buildList {
                        add(
                            Material3SettingsItem(
                                icon =
                                    painterResource(
                                        if (lyricsBackgroundStyle == LyricsBackgroundStyle.THUMBNAIL) {
                                            R.drawable.insert_photo
                                        } else {
                                            R.drawable.palette
                                        },
                                    ),
                                title = { Text(stringResource(R.string.lyrics_background)) },
                                description = {
                                    Text(
                                        when (lyricsBackgroundStyle) {
                                            LyricsBackgroundStyle.THEME -> stringResource(R.string.lyrics_background_theme_desc)
                                            LyricsBackgroundStyle.THUMBNAIL -> stringResource(R.string.lyrics_background_thumbnail_desc)
                                        },
                                    )
                                },
                                onClick = { showLyricsBackgroundStyleDialog = true },
                            ),
                        )
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.lyrics),
                                title = { Text(stringResource(R.string.lyrics_text_size)) },
                                description = { Text("${lyricsTextSize.roundToInt()} sp") },
                                onClick = { showLyricsTextSizeDialog = true },
                            ),
                        )
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.lyrics),
                                title = { Text(stringResource(R.string.lyrics_line_spacing)) },
                                description = { Text(String.format(Locale.US, "%.1f", lyricsLineSpacing)) },
                                onClick = { showLyricsLineSpacingDialog = true },
                            ),
                        )
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.lyrics),
                                title = { Text(stringResource(R.string.lyrics_text_position)) },
                                description = {
                                    Text(
                                        when (lyricsPosition) {
                                            LyricsPosition.LEFT -> stringResource(R.string.left)
                                            LyricsPosition.CENTER -> stringResource(R.string.center)
                                            LyricsPosition.RIGHT -> stringResource(R.string.right)
                                        },
                                    )
                                },
                                onClick = { showLyricsPositionDialog = true },
                            ),
                        )
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.lyrics),
                                title = { Text(stringResource(R.string.respect_agent_positioning)) },
                                description = { Text(stringResource(R.string.respect_agent_positioning_desc)) },
                                trailingContent = {
                                    Switch(
                                        checked = respectAgentPositioning,
                                        onCheckedChange = onRespectAgentPositioningChange,
                                        thumbContent = {
                                            Icon(
                                                painter =
                                                    painterResource(
                                                        id = if (respectAgentPositioning) R.drawable.check else R.drawable.close,
                                                    ),
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        },
                                    )
                                },
                                onClick = { onRespectAgentPositioningChange(!respectAgentPositioning) },
                            ),
                        )
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.lyrics),
                                title = { Text(stringResource(R.string.lyrics_click_change)) },
                                trailingContent = {
                                    Switch(
                                        checked = lyricsClick,
                                        onCheckedChange = onLyricsClickChange,
                                        thumbContent = {
                                            Icon(
                                                painter =
                                                    painterResource(
                                                        id = if (lyricsClick) R.drawable.check else R.drawable.close,
                                                    ),
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        },
                                    )
                                },
                                onClick = { onLyricsClickChange(!lyricsClick) },
                            ),
                        )
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.lyrics),
                                title = { Text(stringResource(R.string.lyrics_auto_scroll)) },
                                trailingContent = {
                                    Switch(
                                        checked = lyricsScroll,
                                        onCheckedChange = onLyricsScrollChange,
                                        thumbContent = {
                                            Icon(
                                                painter =
                                                    painterResource(
                                                        id = if (lyricsScroll) R.drawable.check else R.drawable.close,
                                                    ),
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        },
                                    )
                                },
                                onClick = { onLyricsScrollChange(!lyricsScroll) },
                            ),
                        )
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.lyrics),
                                title = { Text(stringResource(R.string.hide_status_bar_fullscreen)) },
                                description = { Text(stringResource(R.string.hide_status_bar_fullscreen_desc)) },
                                trailingContent = {
                                    Switch(
                                        checked = hideStatusBarOnFullscreen,
                                        onCheckedChange = onHideStatusBarOnFullscreenChange,
                                        thumbContent = {
                                            Icon(
                                                painter =
                                                    painterResource(
                                                        id = if (hideStatusBarOnFullscreen) R.drawable.check else R.drawable.close,
                                                    ),
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        },
                                    )
                                },
                                onClick = { onHideStatusBarOnFullscreenChange(!hideStatusBarOnFullscreen) },
                            ),
                        )
                    },
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
