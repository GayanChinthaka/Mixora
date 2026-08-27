/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.ui.screens.settings.integrations

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pokerlanka.mixora.LocalPlayerAwareWindowInsets
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.ui.component.IconButton
import com.pokerlanka.mixora.ui.component.IntegrationCard
import com.pokerlanka.mixora.ui.component.IntegrationCardItem
import com.pokerlanka.mixora.ui.utils.backToMain

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pokerlanka.mixora.constants.DeezerUserTokenKey
import com.pokerlanka.mixora.constants.MusixmatchUserTokenKey
import com.pokerlanka.mixora.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationScreen(
    navController: NavController
) {
    val (musixmatchToken, onMusixmatchTokenChange) = rememberPreference(MusixmatchUserTokenKey, defaultValue = "")
    val (deezerToken, onDeezerTokenChange) = rememberPreference(DeezerUserTokenKey, defaultValue = "")

    var showMusixmatchDialog by remember { mutableStateOf(false) }
    var showDeezerDialog by remember { mutableStateOf(false) }

    if (showMusixmatchDialog) {
        LyricsServiceConfigDialog(
            serviceName = "Musixmatch",
            currentToken = musixmatchToken,
            onSaveToken = onMusixmatchTokenChange,
            onDismiss = { showMusixmatchDialog = false },
            isMusixmatch = true
        )
    }

    if (showDeezerDialog) {
        LyricsServiceConfigDialog(
            serviceName = "Deezer",
            currentToken = deezerToken,
            onSaveToken = onDeezerTokenChange,
            onDismiss = { showDeezerDialog = false },
            isMusixmatch = false
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        IntegrationCard(
            title = stringResource(R.string.scrobbling),
            items = listOf(
                IntegrationCardItem(
                    icon = painterResource(R.drawable.music_note),
                    title = { Text(stringResource(R.string.lastfm_integration)) },
                    onClick = {
                        navController.navigate("settings/integrations/lastfm")
                    }
                )
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        IntegrationCard(
            title = stringResource(R.string.lyrics_services),
            items = listOf(
                IntegrationCardItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.musixmatch_integration)) },
                    description = { Text(stringResource(R.string.musixmatch_integration_desc)) },
                    trailingContent = {
                        val isConnected = musixmatchToken.isNotBlank()
                        Badge(
                            containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            contentColor = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                if (isConnected) stringResource(R.string.connected) + " ✓" else stringResource(R.string.not_configured) + " ⚠️",
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    },
                    onClick = { showMusixmatchDialog = true }
                ),
                IntegrationCardItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.deezer_integration)) },
                    description = { Text(stringResource(R.string.deezer_integration_desc)) },
                    trailingContent = {
                        val isConnected = deezerToken.isNotBlank()
                        Badge(
                            containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            contentColor = if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                if (isConnected) stringResource(R.string.connected) + " ✓" else stringResource(R.string.not_configured) + " ⚠️",
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    },
                    onClick = { showDeezerDialog = true }
                )
            )
        )
    }

    TopAppBar(
        title = { Text(stringResource(R.string.integrations)) },
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
        }
    )
}
