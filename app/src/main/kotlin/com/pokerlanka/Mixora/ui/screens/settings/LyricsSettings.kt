/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pokerlanka.mixora.LocalPlayerAwareWindowInsets
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.constants.ExperimentalLyricsKey
import com.pokerlanka.mixora.constants.HideStatusBarOnFullscreenKey
import com.pokerlanka.mixora.constants.LyricsAnimationStyle
import com.pokerlanka.mixora.constants.LyricsAnimationStyleKey
import com.pokerlanka.mixora.constants.LyricsClickKey
import com.pokerlanka.mixora.constants.LyricsGlowEffectKey
import com.pokerlanka.mixora.constants.LyricsLineSpacingKey
import com.pokerlanka.mixora.constants.LyricsScrollKey
import com.pokerlanka.mixora.constants.LyricsTextPositionKey
import com.pokerlanka.mixora.constants.LyricsTextSizeKey
import com.pokerlanka.mixora.constants.RespectAgentPositioningKey
import com.pokerlanka.mixora.ui.component.DefaultDialog
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
    val (experimentalLyrics, onExperimentalLyricsChange) = rememberPreference(ExperimentalLyricsKey, defaultValue = true)

    val (lyricsGlowEffect, onLyricsGlowEffectChange) = rememberPreference(LyricsGlowEffectKey, defaultValue = false)
    val (lyricsAnimationStyle, onLyricsAnimationStyleChange) =
        rememberEnumPreference(
            LyricsAnimationStyleKey,
            defaultValue = LyricsAnimationStyle.FADE,
        )
    val (lyricsTextSize, onLyricsTextSizeChange) = rememberPreference(LyricsTextSizeKey, defaultValue = 24f)
    val (lyricsLineSpacing, onLyricsLineSpacingChange) = rememberPreference(LyricsLineSpacingKey, defaultValue = 1.2f)

    var showLyricsAnimationStyleDialog by remember { mutableStateOf(false) }
    var showLyricsTextSizeDialog by remember { mutableStateOf(false) }
    var showLyricsLineSpacingDialog by remember { mutableStateOf(false) }
    var showLyricsPositionDialog by remember { mutableStateOf(false) }

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

    if (showLyricsAnimationStyleDialog) {
        EnumDialog(
            onDismiss = { showLyricsAnimationStyleDialog = false },
            onSelect = {
                onLyricsAnimationStyleChange(it)
                showLyricsAnimationStyleDialog = false
            },
            title = stringResource(R.string.lyrics_animation_style_title),
            current = lyricsAnimationStyle,
            values = LyricsAnimationStyle.values().toList(),
            valueText = {
                when (it) {
                    LyricsAnimationStyle.NONE -> stringResource(R.string.lyrics_animation_none)
                    LyricsAnimationStyle.FADE -> stringResource(R.string.lyrics_animation_fade)
                    LyricsAnimationStyle.GLOW -> stringResource(R.string.lyrics_animation_glow)
                    LyricsAnimationStyle.SLIDE -> stringResource(R.string.lyrics_animation_slide)
                    LyricsAnimationStyle.KARAOKE -> stringResource(R.string.lyrics_animation_karaoke)
                    LyricsAnimationStyle.APPLE -> stringResource(R.string.lyrics_animation_apple)
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
                        tempTextSize = 24f
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
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Material3SettingsGroup(
                title = stringResource(R.string.lyrics),
                items =
                    buildList {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.lyrics),
                                title = { Text(stringResource(R.string.experimental_lyrics)) },
                                description = { Text(stringResource(R.string.experimental_lyrics_desc)) },
                                showBadge = true,
                                trailingContent = {
                                    Switch(
                                        checked = experimentalLyrics,
                                        onCheckedChange = {
                                            onExperimentalLyricsChange(it)
                                        },
                                        thumbContent = {
                                            Icon(
                                                painter =
                                                    painterResource(
                                                        id = if (experimentalLyrics) R.drawable.check else R.drawable.close,
                                                    ),
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        },
                                    )
                                },
                                onClick = {
                                    onExperimentalLyricsChange(!experimentalLyrics)
                                },
                            ),
                        )

                        if (!experimentalLyrics) {
                            add(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.lyrics),
                                    title = { Text(stringResource(R.string.lyrics_glow_effect)) },
                                    description = { Text(stringResource(R.string.lyrics_glow_effect_desc)) },
                                    trailingContent = {
                                        Switch(
                                            checked = lyricsGlowEffect,
                                            onCheckedChange = onLyricsGlowEffectChange,
                                            thumbContent = {
                                                Icon(
                                                    painter =
                                                        painterResource(
                                                            id = if (lyricsGlowEffect) R.drawable.check else R.drawable.close,
                                                        ),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                                )
                                            },
                                        )
                                    },
                                    onClick = { onLyricsGlowEffectChange(!lyricsGlowEffect) },
                                ),
                            )
                            add(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.lyrics),
                                    title = { Text(stringResource(R.string.lyrics_animation_style_title)) },
                                    description = {
                                        Text(
                                            when (lyricsAnimationStyle) {
                                                LyricsAnimationStyle.NONE -> stringResource(R.string.lyrics_animation_none)
                                                LyricsAnimationStyle.FADE -> stringResource(R.string.lyrics_animation_fade)
                                                LyricsAnimationStyle.GLOW -> stringResource(R.string.lyrics_animation_glow)
                                                LyricsAnimationStyle.SLIDE -> stringResource(R.string.lyrics_animation_slide)
                                                LyricsAnimationStyle.KARAOKE -> stringResource(R.string.lyrics_animation_karaoke)
                                                LyricsAnimationStyle.APPLE -> stringResource(R.string.lyrics_animation_apple)
                                            },
                                        )
                                    },
                                    onClick = { showLyricsAnimationStyleDialog = true },
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
                        }

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
        }
    }
}
