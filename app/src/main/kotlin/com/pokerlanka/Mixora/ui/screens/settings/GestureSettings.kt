/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pokerlanka.mixora.LocalPlayerAwareWindowInsets
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.constants.SwipeMiniPlayerKey
import com.pokerlanka.mixora.constants.SwipeSensitivityKey
import com.pokerlanka.mixora.constants.SwipeThumbnailKey
import com.pokerlanka.mixora.constants.SwipeToRemoveSongKey
import com.pokerlanka.mixora.ui.component.DefaultDialog
import com.pokerlanka.mixora.ui.component.IconButton
import com.pokerlanka.mixora.ui.component.Material3SettingsGroup
import com.pokerlanka.mixora.ui.component.Material3SettingsItem
import com.pokerlanka.mixora.ui.utils.backToMain
import com.pokerlanka.mixora.utils.rememberPreference
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureSettings(
    navController: NavController,
) {
    val (swipeThumbnail, onSwipeThumbnailChange) =
        rememberPreference(
            SwipeThumbnailKey,
            defaultValue = true,
        )
    val (swipeMiniPlayer, onSwipeMiniPlayerChange) =
        rememberPreference(
            SwipeMiniPlayerKey,
            defaultValue = true,
        )
    val (swipeSensitivity, onSwipeSensitivityChange) =
        rememberPreference(
            SwipeSensitivityKey,
            defaultValue = 0.73f,
        )
    val (swipeToRemoveSong, onSwipeToRemoveSongChange) =
        rememberPreference(
            SwipeToRemoveSongKey,
            defaultValue = true,
        )

    var showSensitivityDialog by rememberSaveable { mutableStateOf(false) }

    if (showSensitivityDialog) {
        var tempSensitivity by remember { mutableFloatStateOf(swipeSensitivity) }

        DefaultDialog(
            onDismiss = {
                tempSensitivity = swipeSensitivity
                showSensitivityDialog = false
            },
            buttons = {
                TextButton(
                    onClick = {
                        tempSensitivity = 0.73f
                    },
                ) {
                    Text(stringResource(R.string.reset))
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = {
                        tempSensitivity = swipeSensitivity
                        showSensitivityDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        onSwipeSensitivityChange(tempSensitivity)
                        showSensitivityDialog = false
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        ) {
            Text(
                text = stringResource(R.string.swipe_sensitivity),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                text =
                    stringResource(
                        R.string.sensitivity_percentage,
                        (tempSensitivity * 100).roundToInt(),
                    ),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Slider(
                value = tempSensitivity,
                onValueChange = { tempSensitivity = it },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gestures)) },
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Material3SettingsGroup(
                title = stringResource(R.string.player_gestures),
                items =
                    listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.swipe),
                            title = { Text(stringResource(R.string.enable_swipe_thumbnail)) },
                            trailingContent = {
                                Switch(
                                    checked = swipeThumbnail,
                                    onCheckedChange = onSwipeThumbnailChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (swipeThumbnail) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onSwipeThumbnailChange(!swipeThumbnail) },
                        ),
                    ) +
                        if (swipeThumbnail || swipeMiniPlayer) {
                            listOf(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.tune),
                                    title = { Text(stringResource(R.string.swipe_sensitivity)) },
                                    description = {
                                        Text(
                                            stringResource(
                                                R.string.sensitivity_percentage,
                                                (swipeSensitivity * 100).roundToInt(),
                                            ),
                                        )
                                    },
                                    onClick = { showSensitivityDialog = true },
                                ),
                            )
                        } else {
                            emptyList()
                        },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.mini_player_gestures),
                items =
                    listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.swipe),
                            title = { Text(stringResource(R.string.enable_mini_player_swipe)) },
                            trailingContent = {
                                Switch(
                                    checked = swipeMiniPlayer,
                                    onCheckedChange = onSwipeMiniPlayerChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (swipeMiniPlayer) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onSwipeMiniPlayerChange(!swipeMiniPlayer) },
                        ),
                    ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Material3SettingsGroup(
                title = stringResource(R.string.list_gestures),
                items =
                    listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.swipe),
                            title = { Text(stringResource(R.string.swipe_song_to_remove)) },
                            trailingContent = {
                                Switch(
                                    checked = swipeToRemoveSong,
                                    onCheckedChange = onSwipeToRemoveSongChange,
                                    thumbContent = {
                                        Icon(
                                            painter =
                                                painterResource(
                                                    id = if (swipeToRemoveSong) R.drawable.check else R.drawable.close,
                                                ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onSwipeToRemoveSongChange(!swipeToRemoveSong) },
                        ),
                    ),
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
