/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pokerlanka.mixora.LocalDatabase
import com.pokerlanka.mixora.LocalPlayerAwareWindowInsets
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.constants.AiProvider
import com.pokerlanka.mixora.constants.AiProviderKey
import com.pokerlanka.mixora.constants.AiRomanizationEnabledKey
import com.pokerlanka.mixora.constants.LyricsRomanizeAsMainKey
import com.pokerlanka.mixora.constants.RomanizationPinyinToneMarksKey
import com.pokerlanka.mixora.ui.component.IconButton
import com.pokerlanka.mixora.ui.component.Material3SettingsGroup
import com.pokerlanka.mixora.ui.component.Material3SettingsItem
import com.pokerlanka.mixora.ui.utils.backToMain
import com.pokerlanka.mixora.utils.rememberEnumPreference
import com.pokerlanka.mixora.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RomanizationSettings(
    navController: NavController,
) {
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()

    val (aiRomanizationEnabled, onAiRomanizationEnabledChange) =
        rememberPreference(AiRomanizationEnabledKey, defaultValue = false)
    val (lyricsRomanizeAsMain, onLyricsRomanizeAsMainChange) =
        rememberPreference(LyricsRomanizeAsMainKey, defaultValue = false)
    val (pinyinToneMarks, onPinyinToneMarksChange) =
        rememberPreference(RomanizationPinyinToneMarksKey, defaultValue = true)

    // The romanizer cannot run without a configured provider, so the screen says so up front
    // rather than letting the toggle look active and silently do nothing.
    val aiProvider by rememberEnumPreference(AiProviderKey, AiProvider.NONE)
    val providerConfigured = aiProvider != AiProvider.NONE

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Material3SettingsGroup(
            title = stringResource(R.string.lyrics_romanize_title),
            items =
                listOf(
                    Material3SettingsItem(
                        title = { Text(stringResource(R.string.romanization_ai_enable)) },
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
                        icon = painterResource(R.drawable.language),
                    ),
                    Material3SettingsItem(
                        title = { Text(stringResource(R.string.ai_integration)) },
                        description = {
                            Text(
                                stringResource(
                                    if (providerConfigured) {
                                        R.string.romanization_provider_configured
                                    } else {
                                        R.string.romanization_provider_missing
                                    },
                                ),
                            )
                        },
                        icon = painterResource(R.drawable.discover_tune),
                        isHighlighted = aiRomanizationEnabled && !providerConfigured,
                        onClick = { navController.navigate("settings/ai_integration") },
                    ),
                ),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.options),
            items =
                listOf(
                    Material3SettingsItem(
                        title = { Text(stringResource(R.string.lyrics_romanize_as_main)) },
                        trailingContent = {
                            Switch(
                                checked = lyricsRomanizeAsMain,
                                onCheckedChange = onLyricsRomanizeAsMainChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (lyricsRomanizeAsMain) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        icon = painterResource(R.drawable.queue_music),
                    ),
                    Material3SettingsItem(
                        title = { Text(stringResource(R.string.romanization_pinyin_tones)) },
                        description = { Text(stringResource(R.string.romanization_pinyin_tones_desc)) },
                        trailingContent = {
                            Switch(
                                checked = pinyinToneMarks,
                                onCheckedChange = onPinyinToneMarksChange,
                                thumbContent = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id = if (pinyinToneMarks) R.drawable.check else R.drawable.close,
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize),
                                    )
                                },
                            )
                        },
                        icon = painterResource(R.drawable.info),
                    ),
                ),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.storage),
            items =
                listOf(
                    Material3SettingsItem(
                        title = { Text(stringResource(R.string.romanization_clear_cache)) },
                        description = { Text(stringResource(R.string.romanization_clear_cache_desc)) },
                        icon = painterResource(R.drawable.delete),
                        onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    runCatching { database.clearRomanizedLyricsCache() }
                                }
                            }
                        },
                    ),
                ),
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.lyrics_romanize_title)) },
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
