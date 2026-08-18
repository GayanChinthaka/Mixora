/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.pokerlanka.mixora.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pokerlanka.mixora.LocalPlayerAwareWindowInsets
import com.pokerlanka.mixora.LocalPlayerConnection
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.together.TogetherLink
import com.pokerlanka.mixora.ui.component.IconButton as MixoraIconButton
import com.pokerlanka.mixora.viewmodels.MusicTogetherActivityLogItemUiModel
import com.pokerlanka.mixora.viewmodels.MusicTogetherActivityLogUiModels
import com.pokerlanka.mixora.viewmodels.MusicTogetherDialogUiState
import com.pokerlanka.mixora.viewmodels.MusicTogetherEffect
import com.pokerlanka.mixora.viewmodels.MusicTogetherHostUiModel
import com.pokerlanka.mixora.viewmodels.MusicTogetherJoinUiModel
import com.pokerlanka.mixora.viewmodels.MusicTogetherParticipantUiModel
import com.pokerlanka.mixora.viewmodels.MusicTogetherParticipantUiModels
import com.pokerlanka.mixora.viewmodels.MusicTogetherPlaybackUiModel
import com.pokerlanka.mixora.viewmodels.MusicTogetherScreenState
import com.pokerlanka.mixora.viewmodels.MusicTogetherSessionShareUiModel
import com.pokerlanka.mixora.viewmodels.MusicTogetherStatusUiModel
import com.pokerlanka.mixora.viewmodels.MusicTogetherUiModel
import com.pokerlanka.mixora.viewmodels.MusicTogetherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicTogetherScreen(
    navController: NavController,
    viewModel: MusicTogetherViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val screenState by viewModel.state.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val useSupportingPane = configuration.screenWidthDp >= 600

    LaunchedEffect(playerConnection?.service) {
        viewModel.attachService(playerConnection?.service)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is MusicTogetherEffect.CopyText -> {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(
                        ClipData.newPlainText(context.getString(effect.labelResId), effect.value),
                    )
                }

                is MusicTogetherEffect.ShareText -> {
                    val share =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, effect.value)
                        }
                    context.startActivity(Intent.createChooser(share, null))
                }

                is MusicTogetherEffect.ToastMessage -> {
                    Toast.makeText(context, effect.messageResId, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val model = (screenState as? MusicTogetherScreenState.Success)?.model
    if (model != null) {
        MusicTogetherDialogs(model = model, viewModel = viewModel)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.music_together)) },
                navigationIcon = {
                    MixoraIconButton(
                        onClick = { navController.navigateUp() },
                        onLongClick = {}
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                ),
        ) {
            when (val state = screenState) {
                MusicTogetherScreenState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                MusicTogetherScreenState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = stringResource(R.string.together_idle))
                    }
                }

                is MusicTogetherScreenState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(state.messageResId),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                is MusicTogetherScreenState.Success -> {
                    MusicTogetherContent(
                        model = state.model,
                        useSupportingPane = useSupportingPane,
                        viewModel = viewModel,
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicTogetherContent(
    model: MusicTogetherUiModel,
    useSupportingPane: Boolean,
    viewModel: MusicTogetherViewModel,
) {
    if (useSupportingPane) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(bottom = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                item(contentType = "status") {
                    StatusCard(
                        status = model.status,
                        sessionShare = model.sessionShare,
                        onCopy = viewModel::copySessionValue,
                        onShare = viewModel::shareSessionValue,
                        onLeave = viewModel::leaveSession,
                    )
                }
                item(contentType = "playback") {
                    PlaybackCard(playback = model.playback)
                }
                if (model.host.visible) {
                    item(contentType = "host") {
                        HostControlsCard(host = model.host, viewModel = viewModel)
                    }
                }
                if (!model.status.active) {
                    item(contentType = "join") {
                        JoinControlsCard(join = model.join, viewModel = viewModel)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .widthIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                ParticipantsCard(
                    participants = model.participants,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxWidth(),
                )
                ActivityLogCard(
                    log = model.activityLog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = Spacing.sm,
                vertical = Spacing.sm,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item(contentType = "status") {
                StatusCard(
                    status = model.status,
                    sessionShare = model.sessionShare,
                    onCopy = viewModel::copySessionValue,
                    onShare = viewModel::shareSessionValue,
                    onLeave = viewModel::leaveSession,
                )
            }
            item(contentType = "playback") {
                PlaybackCard(playback = model.playback)
            }
            if (model.host.visible) {
                item(contentType = "host") {
                    HostControlsCard(host = model.host, viewModel = viewModel)
                }
            }
            if (!model.status.active) {
                item(contentType = "join") {
                    JoinControlsCard(join = model.join, viewModel = viewModel)
                }
            }
            item(contentType = "participants") {
                ParticipantsCard(
                    participants = model.participants,
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item(contentType = "activity_log") {
                ActivityLogCard(
                    log = model.activityLog,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun StatusCard(
    status: MusicTogetherStatusUiModel,
    sessionShare: MusicTogetherSessionShareUiModel?,
    onCopy: (Int, String) -> Unit,
    onShare: (String) -> Unit,
    onLeave: () -> Unit,
) {
    val accent = when {
        status.error -> MaterialTheme.colorScheme.error
        status.active -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                AccentIcon(iconResId = status.iconResId, accent = accent)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(status.titleResId),
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                    )
                    Text(
                        text = stringResource(status.stateLabelResId),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (status.active) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accent),
                    )
                }
                if (status.canLeave) {
                    FilledTonalButton(
                        onClick = onLeave,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.leave),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text(text = stringResource(R.string.together_leave_session))
                    }
                }
            }

            if (status.errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        text = status.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(Spacing.sm),
                    )
                }
            }

            if (sessionShare != null) {
                SessionShareCard(sessionShare = sessionShare, onCopy = onCopy, onShare = onShare)
            }
        }
    }
}

@Composable
private fun SessionShareCard(
    sessionShare: MusicTogetherSessionShareUiModel,
    onCopy: (Int, String) -> Unit,
    onShare: (String) -> Unit,
) {
    val isShortCode = sessionShare.value.length == 6 && sessionShare.value.all { it.isDigit() }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = stringResource(sessionShare.labelResId),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (isShortCode) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(vertical = 4.dp),
                ) {
                    Text(
                        text = sessionShare.value,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                Text(
                    text = sessionShare.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = sessionShare.maxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FilledTonalButton(
                    onClick = { onCopy(sessionShare.labelResId, sessionShare.value) },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.content_copy),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(text = if (isShortCode) "Copy Code" else stringResource(R.string.together_copy_link))
                }
                TextButton(
                    onClick = { onShare(sessionShare.value) },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(text = if (isShortCode) "Share Code" else stringResource(R.string.together_share_link))
                }
            }
        }
    }
}


@Composable
private fun PlaybackCard(playback: MusicTogetherPlaybackUiModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            AccentIcon(
                iconResId = R.drawable.music_note,
                accent = MaterialTheme.colorScheme.secondary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playback.title ?: stringResource(R.string.together_playback_empty),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!playback.artists.isNullOrBlank()) {
                    Text(
                        text = playback.artists,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = stringResource(playback.playbackStateResId),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun HostControlsCard(
    host: MusicTogetherHostUiModel,
    viewModel: MusicTogetherViewModel,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.together_host_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !host.onlineMode,
                    onClick = { viewModel.setHostOnlineMode(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) {
                    Text(text = stringResource(R.string.together_lan))
                }
                SegmentedButton(
                    selected = host.onlineMode,
                    onClick = { viewModel.setHostOnlineMode(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) {
                    Text(text = stringResource(R.string.together_online))
                }
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.together_display_name)) },
                supportingContent = { Text(host.displayName) },
                modifier = Modifier.clickable { viewModel.openDisplayNameDialog() },
            )
            
            if (!host.onlineMode) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.together_port)) },
                    supportingContent = { Text(host.port.toString()) },
                    modifier = Modifier.clickable { viewModel.openPortDialog() },
                )
            }

            ToggleRow(
                titleResId = R.string.together_allow_guests_add,
                checked = host.allowGuestsToAddTracks,
                onCheckedChange = viewModel::setAllowGuestsToAddTracks,
            )
            ToggleRow(
                titleResId = R.string.together_allow_guests_control,
                checked = host.allowGuestsToControlPlayback,
                onCheckedChange = viewModel::setAllowGuestsToControlPlayback,
            )
            ToggleRow(
                titleResId = R.string.together_require_approval,
                checked = host.requireHostApprovalToJoin,
                onCheckedChange = viewModel::setRequireHostApprovalToJoin,
            )

            Button(
                onClick = viewModel::startSession,
                enabled = host.startEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (host.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(text = stringResource(R.string.together_start_session))
                }
            }
        }
    }
}

@Composable
private fun JoinControlsCard(
    join: MusicTogetherJoinUiModel,
    viewModel: MusicTogetherViewModel,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.together_join_section),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !join.onlineMode,
                    onClick = { viewModel.setJoinOnlineMode(false) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) {
                    Text(text = stringResource(R.string.together_lan))
                }
                SegmentedButton(
                    selected = join.onlineMode,
                    onClick = { viewModel.setJoinOnlineMode(true) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) {
                    Text(text = stringResource(R.string.together_online))
                }
            }

            ListItem(
                headlineContent = { Text(text = if (join.input.isBlank()) stringResource(join.hintResId) else join.input) },
                modifier = Modifier.clickable { viewModel.openJoinDialog() },
            )

            Button(
                onClick = viewModel::joinSession,
                enabled = join.canJoin,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (join.joining) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(text = stringResource(R.string.together_join_session))
                }
            }
        }
    }
}

@Composable
private fun ParticipantsCard(
    participants: MusicTogetherParticipantUiModels,
    viewModel: MusicTogetherViewModel,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.together_participants),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (participants.isEmpty) {
                Text(
                    text = stringResource(R.string.together_participants_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                for (i in 0 until participants.size) {
                    val p = participants[i]
                    ParticipantItem(participant = p, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun ParticipantItem(
    participant: MusicTogetherParticipantUiModel,
    viewModel: MusicTogetherViewModel,
) {
    ListItem(
        headlineContent = { Text(participant.name) },
        supportingContent = {
            val roleText = when {
                participant.host -> stringResource(R.string.together_role_host)
                participant.pending -> stringResource(R.string.together_pending_approval)
                else -> stringResource(R.string.together_role_guest)
            }
            Text(roleText)
        },
        leadingContent = {
            AccentIcon(
                iconResId = R.drawable.person,
                accent = if (participant.host) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Row {
                if (participant.showApproveActions) {
                    IconButton(onClick = { viewModel.approveParticipant(participant.id) }) {
                        Icon(painterResource(R.drawable.check), contentDescription = null)
                    }
                    IconButton(onClick = { viewModel.rejectParticipant(participant.id) }) {
                        Icon(painterResource(R.drawable.close), contentDescription = null)
                    }
                }
                if (participant.showModerationActions) {
                    IconButton(onClick = { viewModel.requestKickParticipant(participant.id) }) {
                        Icon(painterResource(R.drawable.kick), contentDescription = null)
                    }
                }
            }
        },
    )
}

@Composable
private fun ActivityLogCard(
    log: MusicTogetherActivityLogUiModels,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = stringResource(R.string.together_activity_log),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (log.isEmpty) {
                Text(
                    text = stringResource(R.string.together_activity_log_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                for (i in 0 until log.size) {
                    val item = log[i]
                    ActivityLogItem(item = item)
                }
            }
        }
    }
}

@Composable
private fun ActivityLogItem(item: MusicTogetherActivityLogItemUiModel) {
    val context = LocalContext.current
    val messageText = remember(item) {
        try {
            if (item.args.isNotEmpty()) {
                context.getString(item.messageResId, *item.args.toTypedArray())
            } else {
                context.getString(item.messageResId)
            }
        } catch (_: Exception) {
            item.args.joinToString(" ")
        }
    }

    ListItem(
        headlineContent = { Text(text = messageText, style = MaterialTheme.typography.bodyMedium) },
        supportingContent = { Text(text = item.timestamp, style = MaterialTheme.typography.bodySmall) },
        leadingContent = {
            Icon(
                painter = painterResource(item.iconResId),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun ToggleRow(
    @StringRes titleResId: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(titleResId),
            style = MaterialTheme.typography.bodyLarge,
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}

@Composable
private fun AccentIcon(
    @DrawableRes iconResId: Int,
    accent: Color,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun MusicTogetherDialogs(
    model: MusicTogetherUiModel,
    viewModel: MusicTogetherViewModel,
) {
    when (val dialog = model.dialog) {
        MusicTogetherDialogUiState.None -> Unit

        is MusicTogetherDialogUiState.DisplayName -> {
            var text by remember { mutableStateOf(dialog.initialValue) }
            AlertDialog(
                onDismissRequest = viewModel::dismissDialog,
                title = { Text(stringResource(R.string.together_display_name)) },
                text = {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.submitDisplayName(text)
                        viewModel.dismissDialog()
                    }) {
                        Text(stringResource(R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDialog) {
                        Text(stringResource(R.string.dismiss))
                    }
                },
            )
        }

        is MusicTogetherDialogUiState.Port -> {
            var text by remember { mutableStateOf(dialog.initialValue) }
            AlertDialog(
                onDismissRequest = viewModel::dismissDialog,
                title = { Text(stringResource(R.string.together_port)) },
                text = {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.submitPort(text)
                        viewModel.dismissDialog()
                    }) {
                        Text(stringResource(R.string.save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDialog) {
                        Text(stringResource(R.string.dismiss))
                    }
                },
            )
        }

        is MusicTogetherDialogUiState.Join -> {
            var text by remember { mutableStateOf(dialog.initialValue) }
            AlertDialog(
                onDismissRequest = viewModel::dismissDialog,
                title = { Text(stringResource(R.string.together_join_session)) },
                text = {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text(stringResource(dialog.placeholderResId)) },
                        singleLine = false,
                        maxLines = 4,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.submitJoinInput(text)
                        viewModel.dismissDialog()
                    }) {
                        Text(stringResource(R.string.together_join_session))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDialog) {
                        Text(stringResource(R.string.dismiss))
                    }
                },
            )
        }

        is MusicTogetherDialogUiState.KickParticipant -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissDialog,
                title = { Text(stringResource(R.string.together_kick)) },
                text = { Text("Are you sure you want to kick ${dialog.participantName}?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmKickParticipant(dialog.participantId) }) {
                        Text(stringResource(R.string.together_kick))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDialog) {
                        Text(stringResource(R.string.dismiss))
                    }
                },
            )
        }

        is MusicTogetherDialogUiState.BanParticipant -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissDialog,
                title = { Text(stringResource(R.string.together_ban)) },
                text = { Text("Are you sure you want to ban ${dialog.participantName}?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmBanParticipant(dialog.participantId) }) {
                        Text(stringResource(R.string.together_ban))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDialog) {
                        Text(stringResource(R.string.dismiss))
                    }
                },
            )
        }

        is MusicTogetherDialogUiState.TransferHost -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissDialog,
                title = { Text(stringResource(R.string.together_make_host)) },
                text = { Text("Transfer host role to ${dialog.participantName}?") },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmTransferHost(dialog.participantId) }) {
                        Text(stringResource(R.string.together_make_host))
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissDialog) {
                        Text(stringResource(R.string.dismiss))
                    }
                },
            )
        }
    }
}


private object Spacing {
    val xs = 8.dp
    val sm = 16.dp
    val md = 24.dp
    val lg = 32.dp
}
