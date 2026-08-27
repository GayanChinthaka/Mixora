/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pokerlanka.mixora.BuildConfig
import com.pokerlanka.mixora.LocalPlayerAwareWindowInsets
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.ui.component.IconButton
import com.pokerlanka.mixora.ui.component.Material3SettingsGroup
import com.pokerlanka.mixora.ui.component.Material3SettingsItem
import com.pokerlanka.mixora.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    latestVersionName: String,
) {
    val context = LocalContext.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val hasAndroidAuto = remember {
        try {
            context.packageManager.getPackageInfo(
                "com.google.android.projection.gearhead", 0
            )
            true
        } catch (e: Exception) {
            false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.account),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.account),
                            title = { Text(stringResource(R.string.account)) },
                            description = { Text(stringResource(R.string.settings_account_desc)) },
                            onClick = { navController.navigate("settings/account") }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.group),
                            title = { Text(stringResource(R.string.music_together)) },
                            description = { Text(stringResource(R.string.settings_mix_together_desc)) },
                            onClick = { navController.navigate("settings/music_together") }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.stats),
                            title = { Text(stringResource(R.string.stats)) },
                            description = { Text(stringResource(R.string.settings_stats_desc)) },
                            onClick = { navController.navigate("stats") }
                        )
                    )
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.settings_section_ui),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.palette),
                            title = { Text(stringResource(R.string.appearance)) },
                            description = { Text(stringResource(R.string.settings_appearance_desc)) },
                            onClick = { navController.navigate("settings/appearance") }
                        )
                    )
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.settings_section_player_content),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.play),
                            title = { Text(stringResource(R.string.player_and_audio)) },
                            description = { Text(stringResource(R.string.settings_player_desc)) },
                            onClick = { navController.navigate("settings/player") }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.swipe),
                            title = { Text(stringResource(R.string.gestures)) },
                            description = { Text(stringResource(R.string.gestures_desc)) },
                            onClick = { navController.navigate("settings/gestures") }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics)) },
                            description = { Text(stringResource(R.string.settings_lyrics_desc)) },
                            onClick = { navController.navigate("settings/lyrics") }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.language),
                            title = { Text(stringResource(R.string.content)) },
                            description = { Text(stringResource(R.string.settings_content_desc)) },
                            onClick = { navController.navigate("settings/content") }
                        )
                    )
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            if (hasAndroidAuto) {
                item {
                    Material3SettingsGroup(
                        title = stringResource(R.string.android_auto),
                        items = listOf(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.ic_android_auto),
                                title = { Text(stringResource(R.string.android_auto)) },
                                description = { Text(stringResource(R.string.settings_android_auto_desc)) },
                                onClick = { navController.navigate("settings/android_auto") }
                            )
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.settings_section_storage),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.storage),
                            title = { Text(stringResource(R.string.storage)) },
                            description = { Text(stringResource(R.string.settings_storage_desc)) },
                            onClick = { navController.navigate("settings/storage") }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.restore),
                            title = { Text(stringResource(R.string.backup_restore)) },
                            description = { Text(stringResource(R.string.settings_backup_desc)) },
                            onClick = { navController.navigate("settings/backup_restore") }
                        )
                    )
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.settings_section_system),
                    items = buildList {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.wifi_proxy),
                                title = { Text(stringResource(R.string.proxy)) },
                                description = { Text(stringResource(R.string.settings_proxy_desc)) },
                                onClick = { navController.navigate("settings/internet") }
                            )
                        )
                        if (isAndroid12OrLater) {
                            add(
                                Material3SettingsItem(
                                    icon = painterResource(R.drawable.link),
                                    title = { Text(stringResource(R.string.default_links)) },
                                    description = { Text(stringResource(R.string.settings_links_desc)) },
                                    onClick = {
                                        try {
                                            val intent = Intent(
                                                Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                                                Uri.parse("package:${context.packageName}")
                                            ).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                R.string.open_app_settings_error,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                )
                            )
                        }
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.update),
                                title = { Text(stringResource(R.string.updater)) },
                                description = { Text(stringResource(R.string.settings_updates_desc)) },
                                onClick = { navController.navigate("settings/updates") }
                            )
                        )
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.info),
                                title = { Text(stringResource(R.string.about)) },
                                description = { Text(stringResource(R.string.settings_about_desc)) },
                                onClick = { navController.navigate("settings/about") }
                            )
                        )
                    }
                )
            }
        }
    }
}
