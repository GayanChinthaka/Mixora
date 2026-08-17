/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.ui.component.IconButton
import com.pokerlanka.mixora.ui.component.Material3SettingsGroup
import com.pokerlanka.mixora.ui.component.Material3SettingsItem
import com.pokerlanka.mixora.utils.potoken.BotGuardTokenGenerator
import com.pokerlanka.mixora.utils.potoken.PoTokenResult
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoTokenGenerationScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isGenerating by remember { mutableStateOf(false) }
    var generatedTokens by remember { mutableStateOf<PoTokenResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun generateTokens() {
        scope.launch {
            isGenerating = true
            error = null
            val sessionId = UUID.randomUUID().toString()
            val videoId = "jNQXAC9IVRw"
            
            try {
                BotGuardTokenGenerator.initialize(context)
                val result = BotGuardTokenGenerator.mintToken(videoId, sessionId)
                if (result != null) {
                    generatedTokens = result
                } else {
                    error = "Failed to generate tokens"
                }
            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
            } finally {
                isGenerating = false
            }
        }
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PO Token Generation") },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        onLongClick = {}
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Manual PO Token Generation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "Generate cryptographically valid Proof-of-Origin tokens for playback.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = ::generateTokens,
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Generating...")
                } else {
                    Text("Generate Token")
                }
            }

            error?.let {
                Spacer(Modifier.height(16.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            generatedTokens?.let { tokens ->
                Spacer(Modifier.height(32.dp))
                
                Material3SettingsGroup(
                    title = "Generated Tokens",
                    items = listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.key),
                            title = { Text("PO Token (GVS)") },
                            description = { Text(tokens.playerRequestPoToken.take(30) + "...") },
                            onClick = { copyToClipboard("GVS Token", tokens.playerRequestPoToken) }
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.play),
                            title = { Text("PO Token (Player)") },
                            description = { Text(tokens.streamingDataPoToken.take(30) + "...") },
                            onClick = { copyToClipboard("Player Token", tokens.streamingDataPoToken) }
                        )
                    )
                )
            }
        }
    }
}
