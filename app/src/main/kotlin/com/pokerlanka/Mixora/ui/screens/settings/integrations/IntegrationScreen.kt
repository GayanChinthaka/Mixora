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
import com.pokerlanka.mixora.constants.AiApiKeyKey
import com.pokerlanka.mixora.constants.AiProvider
import com.pokerlanka.mixora.constants.AiProviderKey
import com.pokerlanka.mixora.utils.rememberEnumPreference
import com.pokerlanka.mixora.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationScreen(
    navController: NavController
) {
    val aiProvider by rememberEnumPreference(AiProviderKey, AiProvider.NONE)
    val (aiApiKey, _) = rememberPreference(AiApiKeyKey, defaultValue = "")
    val isAiConnected = aiProvider != AiProvider.NONE && aiApiKey.isNotBlank()

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
                    title = { Text("AI Lyrics & Romanization") },
                    description = { Text("Gemini, ChatGPT, or OpenRouter fallback for missing lyrics") },
                    trailingContent = {
                        Badge(
                            containerColor = if (isAiConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            contentColor = if (isAiConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error
                        ) {
                            Text(
                                if (isAiConnected) stringResource(R.string.connected) + " ✓" else stringResource(R.string.not_configured) + " ⚠️",
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    },
                    onClick = { navController.navigate("settings/ai_integration") }
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
