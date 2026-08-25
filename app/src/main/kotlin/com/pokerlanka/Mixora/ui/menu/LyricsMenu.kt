/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.ui.menu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.pokerlanka.mixora.LocalDatabase
import com.pokerlanka.mixora.LocalNavController
import com.pokerlanka.mixora.LocalPlayerBottomSheetState
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.db.entities.LyricsEntity
import com.pokerlanka.mixora.db.entities.SongEntity
import com.pokerlanka.mixora.lyrics.LyricsUtils
import com.pokerlanka.mixora.models.MediaMetadata
import com.pokerlanka.mixora.ui.component.Material3MenuGroup
import com.pokerlanka.mixora.ui.component.Material3MenuItemData
import com.pokerlanka.mixora.ui.component.NewAction
import com.pokerlanka.mixora.ui.component.NewActionGrid
import com.pokerlanka.mixora.ui.component.TextFieldDialog
import com.pokerlanka.mixora.ui.component.RomanizationSetupDialog
import com.pokerlanka.mixora.viewmodels.LyricsMenuViewModel
import com.pokerlanka.mixora.constants.LyricsBackgroundStyle
import com.pokerlanka.mixora.constants.LyricsBackgroundStyleKey
import com.pokerlanka.mixora.constants.AiApiKeyKey
import com.pokerlanka.mixora.constants.AiCustomEndpointKey
import com.pokerlanka.mixora.constants.AiCustomModelKey
import com.pokerlanka.mixora.constants.AiProvider
import com.pokerlanka.mixora.constants.AiProviderKey
import com.pokerlanka.mixora.constants.AiRomanizationEnabledKey
import com.pokerlanka.mixora.constants.AiSelectedModelKey
import com.pokerlanka.mixora.constants.RespectAgentPositioningKey
import com.pokerlanka.mixora.constants.ShowIntervalIndicatorKey
import com.pokerlanka.mixora.utils.rememberEnumPreference
import com.pokerlanka.mixora.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsMenu(
    lyricsProvider: () -> LyricsEntity?,
    songProvider: () -> SongEntity?,
    mediaMetadataProvider: () -> MediaMetadata,
    onDismiss: () -> Unit,
    onShowOffsetDialog: () -> Unit = {},
    viewModel: LyricsMenuViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val navController = LocalNavController.current
    val playerBottomSheetState = LocalPlayerBottomSheetState.current

    var respectAgentPositioning by rememberPreference(RespectAgentPositioningKey, true)
    var showIntervalIndicator by rememberPreference(ShowIntervalIndicatorKey, true)
    var lyricsBackgroundStyle by rememberEnumPreference(LyricsBackgroundStyleKey, LyricsBackgroundStyle.THEME)

    var showEditDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showEditDialog) {
        TextFieldDialog(
            onDismiss = { showEditDialog = false },
            icon = { Icon(painter = painterResource(R.drawable.edit), contentDescription = null) },
            title = { Text(text = mediaMetadataProvider().title) },
            initialTextFieldValue = TextFieldValue(lyricsProvider()?.lyrics.orEmpty()),
            singleLine = false,
            onDone = {
                database.query {
                    upsert(
                        LyricsEntity(
                            id = mediaMetadataProvider().id,
                            lyrics = it,
                            provider = lyricsProvider()?.provider ?: "Manual",
                        ),
                    )
                }
            },
        )
    }

    // Same preference the Lyrics settings screen writes. This used to toggle a per-song DB
    // column instead, which is why the two switches appeared out of sync: each was gating the
    // other, so flipping one alone did nothing.
    var isChecked by rememberPreference(AiRomanizationEnabledKey, defaultValue = false)
    var showRomanizationSetupDialog by remember { mutableStateOf(false) }

    // Same configured-check as the Lyrics settings screen: without a provider, key and model a
    // request cannot be made, so the toggle points at AI Integration instead of silently failing.
    val aiProvider by rememberEnumPreference(AiProviderKey, AiProvider.NONE)
    val aiApiKey by rememberPreference(AiApiKeyKey, defaultValue = "")
    val aiCustomEndpoint by rememberPreference(AiCustomEndpointKey, defaultValue = "")
    val aiSelectedModel by rememberPreference(AiSelectedModelKey, defaultValue = "")
    val aiCustomModel by rememberPreference(AiCustomModelKey, defaultValue = "")
    val aiModel = if (aiProvider == AiProvider.CUSTOM) aiCustomModel else aiSelectedModel
    val aiConfigured =
        aiProvider != AiProvider.NONE &&
            aiApiKey.isNotBlank() &&
            aiModel.isNotBlank() &&
            (aiProvider != AiProvider.CUSTOM || aiCustomEndpoint.isNotBlank())

    if (showRomanizationSetupDialog) {
        RomanizationSetupDialog(
            onDismiss = { showRomanizationSetupDialog = false },
            onSetUp = {
                showRomanizationSetupDialog = false
                playerBottomSheetState?.collapseSoft()
                onDismiss()
                navController.navigate("settings/ai_integration")
            },
        )
    }


    var lyricsOffset by rememberSaveable { mutableIntStateOf(songProvider()?.lyricsOffset ?: 0) }

    LaunchedEffect(songProvider()) {
        lyricsOffset = songProvider()?.lyricsOffset ?: 0
    }

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    LazyColumn(
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item {
            val isThumbnailBackground = lyricsBackgroundStyle == LyricsBackgroundStyle.THUMBNAIL
            val backgroundTileContentColor =
                if (isThumbnailBackground) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

            NewActionGrid(
                actions =
                    listOf(
                        NewAction(
                            icon = {
                                Icon(
                                    painter =
                                        painterResource(
                                            if (isThumbnailBackground) R.drawable.insert_photo else R.drawable.palette,
                                        ),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = backgroundTileContentColor,
                                )
                            },
                            text =
                                if (isThumbnailBackground) {
                                    stringResource(R.string.lyrics_background_thumbnail)
                                } else {
                                    stringResource(R.string.lyrics_background_theme)
                                },
                            onClick = {
                                lyricsBackgroundStyle =
                                    if (isThumbnailBackground) {
                                        LyricsBackgroundStyle.THEME
                                    } else {
                                        LyricsBackgroundStyle.THUMBNAIL
                                    }
                            },
                            backgroundColor =
                                if (isThumbnailBackground) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Unspecified
                                },
                            contentColor = backgroundTileContentColor,
                        ),
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.edit),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            text = stringResource(R.string.edit),
                            onClick = {
                                showEditDialog = true
                            },
                        ),
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.cached),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            text = stringResource(R.string.refetch),
                            onClick = {
                                onDismiss()
                                viewModel.refetchLyrics(mediaMetadataProvider(), lyricsProvider())
                            },
                        ),
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.content_copy),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            text = stringResource(R.string.copy),
                            onClick = {
                                lyricsProvider()?.lyrics?.let { lyrics ->
                                    val plainLyrics =
                                        if (lyrics.startsWith("[")) {
                                            LyricsUtils.parseLyrics(lyrics)
                                                .joinToString("\n") { it.text }
                                        } else {
                                            lyrics
                                        }

                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Lyrics", plainLyrics)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                                }
                            },
                        ),
                    ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
                columns = 4,
            )
        }

        item {
            Material3MenuGroup(
                items = buildList {
                    add(
                        Material3MenuItemData(
                            title = { Text(stringResource(R.string.respect_agent_positioning)) },
                            description = { Text(stringResource(R.string.respect_agent_positioning_desc)) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.lyrics),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                respectAgentPositioning = !respectAgentPositioning
                            },
                            trailingContent = {
                                Switch(
                                    checked = respectAgentPositioning,
                                    onCheckedChange = { newCheckedState ->
                                        respectAgentPositioning = newCheckedState
                                    },
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(
                                                id = if (respectAgentPositioning) R.drawable.check else R.drawable.close
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    },
                                    colors = SwitchDefaults.colors(
                                        uncheckedThumbColor = MaterialTheme.colorScheme.primaryContainer,
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        )
                    )
                    
                    add(
                        Material3MenuItemData(
                            title = { Text(stringResource(R.string.show_interval_indicator)) },
                            description = { Text(stringResource(R.string.show_interval_indicator_desc)) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.lyrics),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showIntervalIndicator = !showIntervalIndicator
                            },
                            trailingContent = {
                                Switch(
                                    checked = showIntervalIndicator,
                                    onCheckedChange = { newCheckedState ->
                                        showIntervalIndicator = newCheckedState
                                    },
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(
                                                id = if (showIntervalIndicator) R.drawable.check else R.drawable.close
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    },
                                    colors = SwitchDefaults.colors(
                                        uncheckedThumbColor = MaterialTheme.colorScheme.primaryContainer,
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        )
                    )
                    
                    add(
                        Material3MenuItemData(
                            title = { Text(stringResource(R.string.lyrics_offset)) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.fast_forward),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                onDismiss()
                                onShowOffsetDialog()
                            },
                            trailingContent = {
                                val offsetSec = lyricsOffset / 1000f
                                val formatted = if (lyricsOffset == 0) "0s" else "${if (lyricsOffset > 0) "+" else ""}${String.format(java.util.Locale.US, "%.1fs", offsetSec)}"
                                Text(
                                    text = formatted,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    )

                    add(
                        Material3MenuItemData(
                            title = { Text(text = stringResource(R.string.romanize_current_track)) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.language_korean_latin),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                if (aiConfigured) {
                                    isChecked = !isChecked
                                } else {
                                    showRomanizationSetupDialog = true
                                }
                            },
                            trailingContent = {
                                Switch(
                                    checked = isChecked && aiConfigured,
                                    // Deliberately left interactive while unconfigured: a disabled
                                    // Switch consumes the touch, so onCheckedChange never ran and
                                    // tapping the switch did nothing at all.
                                    onCheckedChange = { newCheckedState ->
                                        if (aiConfigured) {
                                            isChecked = newCheckedState
                                        } else {
                                            showRomanizationSetupDialog = true
                                        }
                                    },
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(
                                                id = if (isChecked && aiConfigured) R.drawable.check else R.drawable.close
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    },
                                    colors = SwitchDefaults.colors(
                                        uncheckedThumbColor = MaterialTheme.colorScheme.primaryContainer,
                                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        )
                    )
                }
            )
        }
    }
}
