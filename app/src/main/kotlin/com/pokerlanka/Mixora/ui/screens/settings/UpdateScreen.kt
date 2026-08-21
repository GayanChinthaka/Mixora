/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.pokerlanka.mixora.BuildConfig
import com.pokerlanka.mixora.LocalPlayerAwareWindowInsets
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.constants.CheckForUpdatesKey
import com.pokerlanka.mixora.constants.LastUpdateCheckTimeKey
import com.pokerlanka.mixora.constants.UpdateNotificationsEnabledKey
import com.pokerlanka.mixora.ui.component.IconButton
import com.pokerlanka.mixora.ui.component.Material3SettingsGroup
import com.pokerlanka.mixora.ui.component.Material3SettingsItem
import com.pokerlanka.mixora.ui.utils.backToMain
import com.pokerlanka.mixora.utils.GitCommit
import com.pokerlanka.mixora.utils.ReleaseInfo
import com.pokerlanka.mixora.utils.Updater
import com.pokerlanka.mixora.utils.rememberPreference
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val (autoCheckUpdates, onAutoCheckUpdatesChange) = rememberPreference(
        CheckForUpdatesKey,
        defaultValue = true
    )
    val (updateNotifications, onUpdateNotificationsChange) = rememberPreference(
        UpdateNotificationsEnabledKey,
        defaultValue = true
    )
    val (_, onLastUpdateCheckTimeChange) = rememberPreference(
        LastUpdateCheckTimeKey,
        defaultValue = 0L
    )

    var isChecking by remember { mutableStateOf(false) }
    var latestRelease by remember { mutableStateOf<ReleaseInfo?>(null) }
    var recentCommits by remember { mutableStateOf<List<GitCommit>>(emptyList()) }
    var checkError by remember { mutableStateOf<String?>(null) }
    var showCommits by rememberSaveable { mutableStateOf(true) }

    val isUpdateAvailable = remember(latestRelease) {
        val release = latestRelease ?: return@remember false
        Updater.isUpdateAvailable(release.tagName, BuildConfig.VERSION_NAME)
    }

    fun checkForUpdates(silent: Boolean = false) {
        if (isChecking) return
        isChecking = true
        checkError = null

        coroutineScope.launch {
            try {
                val releaseResult = Updater.getLatestRelease()
                val commitsResult = Updater.getRecentCommits(15)

                releaseResult.onSuccess { release ->
                    latestRelease = release
                    onLastUpdateCheckTimeChange(System.currentTimeMillis())
                    if (!silent && !Updater.isUpdateAvailable(release.tagName, BuildConfig.VERSION_NAME)) {
                        Toast.makeText(context, "Mixora is up to date!", Toast.LENGTH_SHORT).show()
                    }
                }.onFailure { error ->
                    checkError = error.message ?: "Failed to check for updates"
                    if (!silent) {
                        Toast.makeText(context, "Could not fetch updates", Toast.LENGTH_SHORT).show()
                    }
                }

                commitsResult.onSuccess { commits ->
                    recentCommits = commits
                }
            } catch (e: Exception) {
                checkError = e.message
            } finally {
                isChecking = false
            }
        }
    }

    LaunchedEffect(Unit) {
        checkForUpdates(silent = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.updater)) },
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Hero Status Card
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isUpdateAvailable) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isUpdateAvailable) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        },
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isChecking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = if (isUpdateAvailable) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(
                                        if (isUpdateAvailable) R.drawable.update else R.drawable.check
                                    ),
                                    contentDescription = null,
                                    tint = if (isUpdateAvailable) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = when {
                            isChecking -> stringResource(R.string.checking_for_updates)
                            isUpdateAvailable -> stringResource(R.string.update_available_title)
                            else -> "Mixora is up to date!"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isUpdateAvailable) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = if (isUpdateAvailable && latestRelease != null) {
                            "Version ${latestRelease?.tagName} is available (Current: v${BuildConfig.VERSION_NAME})"
                        } else {
                            "Installed: v${BuildConfig.VERSION_NAME}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUpdateAvailable) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    if (checkError != null && !isChecking) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = checkError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isUpdateAvailable && latestRelease != null) {
                            Button(
                                onClick = {
                                    val downloadUrl = latestRelease?.downloadUrl ?: latestRelease?.htmlUrl ?: Updater.GITHUB_RELEASES_URL
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.download),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Download Update")
                            }
                        }

                        OutlinedButton(
                            onClick = { checkForUpdates(silent = false) },
                            enabled = !isChecking,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.sync),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.check_for_updates_button))
                        }
                    }
                }
            }

            // Release Notes Card (if update available)
            if (isUpdateAvailable && latestRelease?.body?.isNotBlank() == true) {
                Spacer(Modifier.height(16.dp))

                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.info),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "What's New in ${latestRelease?.tagName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = latestRelease?.body ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Settings Section
            Material3SettingsGroup(
                title = stringResource(R.string.update_settings),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.sync),
                        title = { Text(stringResource(R.string.check_for_updates)) },
                        trailingContent = {
                            Switch(
                                checked = autoCheckUpdates,
                                onCheckedChange = onAutoCheckUpdatesChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (autoCheckUpdates) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        }
                    ),
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.notification),
                        title = { Text(stringResource(R.string.update_notifications)) },
                        trailingContent = {
                            Switch(
                                checked = updateNotifications,
                                onCheckedChange = onUpdateNotificationsChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (updateNotifications) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        }
                    )
                )
            )

            // Recent Commits (Changelog) Section
            if (recentCommits.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.changelog),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = if (showCommits) stringResource(R.string.hide_changelog) else stringResource(R.string.view_changelog),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showCommits = !showCommits }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                AnimatedVisibility(visible = showCommits) {
                    ElevatedCard(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            recentCommits.forEachIndexed { index, commit ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (commit.url.isNotBlank()) {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(commit.url))
                                                context.startActivity(intent)
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    // Author Avatar
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        if (commit.authorAvatarUrl != null) {
                                            AsyncImage(
                                                model = commit.authorAvatarUrl,
                                                contentDescription = commit.author,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    painter = painterResource(R.drawable.person),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = commit.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(Modifier.height(2.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = commit.author,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = commit.date,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    // Commit SHA Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    ) {
                                        Text(
                                            text = commit.sha,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
