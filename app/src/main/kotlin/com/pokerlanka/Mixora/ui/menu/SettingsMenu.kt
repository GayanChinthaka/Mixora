/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.ui.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pokerlanka.innertube.utils.parseCookieString
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.constants.DataSyncIdKey
import com.pokerlanka.mixora.constants.InnerTubeCookieKey
import com.pokerlanka.mixora.constants.PoTokenGvsKey
import com.pokerlanka.mixora.constants.SavedAccountsKey
import com.pokerlanka.mixora.constants.VisitorDataKey
import com.pokerlanka.mixora.ui.component.AccountSummaryCard
import com.pokerlanka.mixora.ui.component.AccountSwitcherSheet
import com.pokerlanka.mixora.ui.component.Material3SettingsGroup
import com.pokerlanka.mixora.ui.component.Material3SettingsItem
import com.pokerlanka.mixora.ui.component.TokenEditorDialog
import com.pokerlanka.mixora.utils.SavedAccount
import com.pokerlanka.mixora.utils.decodeSavedAccounts
import com.pokerlanka.mixora.utils.encodeSavedAccounts
import com.pokerlanka.mixora.utils.rememberPreference
import com.pokerlanka.mixora.viewmodels.HomeViewModel
import java.util.UUID

@Composable
fun SettingsMenu(
    navController: NavController,
    onDismiss: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val accountName by viewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrl by viewModel.accountImageUrl.collectAsStateWithLifecycle()
    
    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")
    val (_, onPoTokenChange) = rememberPreference(PoTokenGvsKey, "")
    val (savedAccountsJson, onSavedAccountsJsonChange) = rememberPreference(SavedAccountsKey, "")
    
    val savedAccounts = remember(savedAccountsJson) {
        decodeSavedAccounts(savedAccountsJson)
    }
    
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    
    var showTokenEditor by remember { mutableStateOf(false) }
    var showAccountSwitcher by remember { mutableStateOf(false) }

    if (showTokenEditor) {
        TokenEditorDialog(
            innerTubeCookie = innerTubeCookie,
            visitorData = visitorData,
            dataSyncId = dataSyncId,
            onInnerTubeCookieChange = onInnerTubeCookieChange,
            onPoTokenChange = onPoTokenChange,
            onVisitorDataChange = onVisitorDataChange,
            onDataSyncIdChange = onDataSyncIdChange,
            onDismiss = { showTokenEditor = false }
        )
    }
    
    if (showAccountSwitcher) {
        AccountSwitcherSheet(
            isLoggedIn = isLoggedIn,
            savedAccounts = savedAccounts,
            activeInnerTubeCookie = innerTubeCookie,
            onSaveAccount = {
                val existing = decodeSavedAccounts(savedAccountsJson)
                if (isLoggedIn && existing.none { it.innerTubeCookie == innerTubeCookie }) {
                    val newAccount = SavedAccount(
                        id = UUID.randomUUID().toString(),
                        name = accountName,
                        email = "",
                        channelHandle = "",
                        innerTubeCookie = innerTubeCookie,
                        visitorData = visitorData,
                        dataSyncId = dataSyncId,
                        ytmSync = true,
                        selectedYtmPlaylists = ""
                    )
                    onSavedAccountsJsonChange(encodeSavedAccounts(existing + newAccount))
                }
            },
            onSwitchAccount = { account ->
                onInnerTubeCookieChange(account.innerTubeCookie)
                onVisitorDataChange(account.visitorData)
                onDataSyncIdChange(account.dataSyncId)
            },
            onRemoveAccount = { account ->
                val existing = decodeSavedAccounts(savedAccountsJson)
                onSavedAccountsJsonChange(encodeSavedAccounts(existing.filter { it.id != account.id }))
            },
            onAddAnotherAccount = {
                onDismiss()
                navController.navigate("login")
            },
            onDismiss = { showAccountSwitcher = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                AccountSummaryCard(
                    isLoggedIn = isLoggedIn,
                    accountName = accountName,
                    accountEmail = "", 
                    accountHandle = "",
                    accountImageUrl = accountImageUrl,
                    onPrimaryAction = {
                        onDismiss()
                        if (isLoggedIn) navController.navigate("settings/account") else navController.navigate("login")
                    },
                    onSecondaryAction = {
                        if (isLoggedIn) {
                            onInnerTubeCookieChange("")
                        } else {
                            showTokenEditor = true
                        }
                    },
                    onOpenAccountSwitcher = {
                        showAccountSwitcher = true
                    }
                )
                Spacer(Modifier.height(16.dp))
            }

            item {
                Material3SettingsGroup(
                    title = stringResource(R.string.account),
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.integration),
                            title = { Text(stringResource(R.string.integrations)) },
                            onClick = {
                                onDismiss()
                                navController.navigate("settings/integrations")
                            }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.security),
                            title = { Text(stringResource(R.string.privacy)) },
                            onClick = {
                                onDismiss()
                                navController.navigate("settings/privacy")
                            }
                        )
                    )
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                Material3SettingsGroup(
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.group),
                            title = { Text("Mix Together") },
                            onClick = {
                                onDismiss()
                                navController.navigate("settings/music_together")
                            }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.stats),
                            title = { Text(stringResource(R.string.stats)) },
                            onClick = {
                                onDismiss()
                                navController.navigate("stats")
                            }
                        )
                    )
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                Material3SettingsGroup(
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.palette),
                            title = { Text(stringResource(R.string.appearance)) },
                            onClick = {
                                onDismiss()
                                navController.navigate("settings/appearance")
                            }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.play),
                            title = { Text(stringResource(R.string.player_and_audio)) },
                            onClick = {
                                onDismiss()
                                navController.navigate("settings/player")
                            }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.lyrics),
                            title = { Text(stringResource(R.string.lyrics)) },
                            onClick = {
                                onDismiss()
                                navController.navigate("settings/lyrics")
                            }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.language),
                            title = { Text(stringResource(R.string.content)) },
                            onClick = {
                                onDismiss()
                                navController.navigate("settings/content")
                            }
                        )
                    )
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                Material3SettingsGroup(
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.ic_android_auto),
                            title = { Text(stringResource(R.string.android_auto)) },
                            onClick = {
                                onDismiss()
                                navController.navigate("settings/android_auto")
                            }
                        )
                    )
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                Material3SettingsGroup(
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.storage),
                            title = { Text(stringResource(R.string.storage)) },
                            onClick = {
                                onDismiss()
                                navController.navigate("settings/storage")
                            }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.restore),
                            title = { Text(stringResource(R.string.backup_restore)) },
                            onClick = {
                                onDismiss()
                                navController.navigate("settings/backup_restore")
                            }
                        )
                    )
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            item {
                Material3SettingsGroup(
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.wifi_proxy),
                            title = { Text("Proxy") },
                            onClick = {
                                onDismiss()
                                navController.navigate("settings/internet")
                            }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.key),
                            title = { Text("PO Token Generation") },
                            onClick = {
                                onDismiss()
                                navController.navigate("settings/po_token_generation")
                            }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.update),
                            title = { Text("Updates") },
                            onClick = {
                                onDismiss()
                                navController.navigate("settings/updates")
                            }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.info),
                            title = { Text(stringResource(R.string.about)) },
                            onClick = {
                                onDismiss()
                                navController.navigate("settings/about")
                            }
                        )
                    )
                )
            }
        }
    }
}
