/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.viewmodels

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.playback.MusicService
import com.pokerlanka.mixora.together.AttachMusicTogetherServiceUseCase
import com.pokerlanka.mixora.together.MusicTogetherPreferences
import com.pokerlanka.mixora.together.MusicTogetherSessionActionsUseCase
import com.pokerlanka.mixora.together.MusicTogetherSnapshot
import com.pokerlanka.mixora.together.ObserveMusicTogetherStateUseCase
import com.pokerlanka.mixora.together.TogetherRole
import com.pokerlanka.mixora.together.TogetherRoomSettings
import com.pokerlanka.mixora.together.TogetherRoomState
import com.pokerlanka.mixora.together.TogetherSessionState
import com.pokerlanka.mixora.together.UpdateMusicTogetherPreferencesUseCase
import com.pokerlanka.mixora.together.isConnectedToSession
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

sealed interface MusicTogetherScreenState {
    data object Loading : MusicTogetherScreenState

    data class Success(
        val model: MusicTogetherUiModel,
    ) : MusicTogetherScreenState

    data object Empty : MusicTogetherScreenState

    data class Error(
        @StringRes val messageResId: Int,
    ) : MusicTogetherScreenState
}

@Immutable
data class MusicTogetherUiModel(
    val showWelcomeDialog: Boolean,
    val welcomeDontShowAgain: Boolean,
    val dialog: MusicTogetherDialogUiState,
    val status: MusicTogetherStatusUiModel,
    val sessionShare: MusicTogetherSessionShareUiModel?,
    val playback: MusicTogetherPlaybackUiModel,
    val host: MusicTogetherHostUiModel,
    val join: MusicTogetherJoinUiModel,
    val participants: MusicTogetherParticipantUiModels,
    val activityLog: MusicTogetherActivityLogUiModels,
    val isInSession: Boolean,
    val isHostRole: Boolean,
)

@Immutable
sealed interface MusicTogetherDialogUiState {
    data object None : MusicTogetherDialogUiState

    data class DisplayName(
        val initialValue: String,
    ) : MusicTogetherDialogUiState

    data class Join(
        val initialValue: String,
        @StringRes val placeholderResId: Int,
    ) : MusicTogetherDialogUiState

    data class KickParticipant(
        val participantId: String,
        val participantName: String,
    ) : MusicTogetherDialogUiState

    data class BanParticipant(
        val participantId: String,
        val participantName: String,
    ) : MusicTogetherDialogUiState

    data class TransferHost(
        val participantId: String,
        val participantName: String,
    ) : MusicTogetherDialogUiState
}

@Immutable
data class MusicTogetherStatusUiModel(
    @StringRes val titleResId: Int,
    @StringRes val stateLabelResId: Int,
    val errorMessage: String?,
    @DrawableRes val iconResId: Int,
    val active: Boolean,
    val error: Boolean,
    val waitingApproval: Boolean,
    val canLeave: Boolean,
)

@Immutable
data class MusicTogetherSessionShareUiModel(
    val code: String,
)

@Immutable
data class MusicTogetherPlaybackUiModel(
    val title: String?,
    val artists: String?,
    @StringRes val playbackStateResId: Int,
    val queueSize: Int,
    val currentIndexLabel: String?,
    val shuffleEnabled: Boolean,
    val repeatMode: Int,
)

@Immutable
data class MusicTogetherHostUiModel(
    val displayName: String,
    val allowGuestsToAddTracks: Boolean,
    val allowGuestsToControlPlayback: Boolean,
    val requireHostApprovalToJoin: Boolean,
    val visible: Boolean,
    val startEnabled: Boolean,
    val loading: Boolean,
)

@Immutable
data class MusicTogetherJoinUiModel(
    val input: String,
    @StringRes val hintResId: Int,
    val canJoin: Boolean,
    val disabled: Boolean,
    val joined: Boolean,
    val waitingApproval: Boolean,
    val joining: Boolean,
)

@Immutable
data class MusicTogetherParticipantUiModels(
    private val values: List<MusicTogetherParticipantUiModel>,
) {
    val size: Int get() = values.size
    val isEmpty: Boolean get() = values.isEmpty()

    operator fun get(index: Int): MusicTogetherParticipantUiModel = values[index]
}

@Immutable
data class MusicTogetherParticipantUiModel(
    val id: String,
    val name: String,
    val host: Boolean,
    val pending: Boolean,
    val connected: Boolean,
    val showApproveActions: Boolean,
    val showModerationActions: Boolean,
    val showTransferHostAction: Boolean,
)

@Immutable
data class MusicTogetherActivityLogUiModels(
    private val values: List<MusicTogetherActivityLogItemUiModel>,
) {
    val size: Int get() = values.size
    val isEmpty: Boolean get() = values.isEmpty()

    operator fun get(index: Int): MusicTogetherActivityLogItemUiModel = values[index]
}

@Immutable
data class MusicTogetherActivityLogItemUiModel(
    val id: Long,
    @DrawableRes val iconResId: Int,
    @StringRes val messageResId: Int,
    val args: List<String>,
    val timestamp: String,
)

sealed interface MusicTogetherEffect {
    data class CopyText(
        @StringRes val labelResId: Int,
        val value: String,
    ) : MusicTogetherEffect

    data class ShareText(
        val value: String,
    ) : MusicTogetherEffect

    data class ToastMessage(
        @StringRes val messageResId: Int,
    ) : MusicTogetherEffect
}

@HiltViewModel
class MusicTogetherViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        observeMusicTogetherState: ObserveMusicTogetherStateUseCase,
        private val attachMusicTogetherService: AttachMusicTogetherServiceUseCase,
        private val updatePreferences: UpdateMusicTogetherPreferencesUseCase,
        private val sessionActions: MusicTogetherSessionActionsUseCase,
    ) : ViewModel() {
        private val dialog = MutableStateFlow<MusicTogetherDialogUiState>(MusicTogetherDialogUiState.None)
        private val welcomeDismissedThisSession = MutableStateFlow(false)
        private val welcomeDontShowAgain = MutableStateFlow(true)
        private val activityLog = MutableStateFlow(MusicTogetherActivityLogUiModels(emptyList()))
        private val effectsFlow = MutableSharedFlow<MusicTogetherEffect>(extraBufferCapacity = 8)
        val effects = effectsFlow.asSharedFlow()

        private val logItems = ArrayDeque<MusicTogetherActivityLogItemUiModel>()
        private var nextLogId = 0L
        private var lastSessionId: String? = null
        private var lastRoomState: TogetherRoomState? = null
        private var lastStateWasError = false

        private data class StateInputs(
            val snapshot: MusicTogetherSnapshot,
            val dialogState: MusicTogetherDialogUiState,
            val welcomeDismissed: Boolean,
        )

        private val snapshots =
            observeMusicTogetherState()
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    emit(
                        MusicTogetherSnapshot(
                            preferences =
                                MusicTogetherPreferences(
                                    displayName = "",
                                    allowGuestsToAddTracks = true,
                                    allowGuestsToControlPlayback = false,
                                    requireHostApprovalToJoin = false,
                                    lastJoinCode = "",
                                    welcomeShown = false,
                                ),
                            sessionState = TogetherSessionState.Error(message = throwable.message.orEmpty()),
                        ),
                    )
                }

        val state: StateFlow<MusicTogetherScreenState> =
            combine(
                snapshots,
                dialog,
                welcomeDismissedThisSession,
            ) { snapshot, dialogState, welcomeDismissed ->
                StateInputs(
                    snapshot = snapshot,
                    dialogState = dialogState,
                    welcomeDismissed = welcomeDismissed,
                )
            }.let { inputs ->
                combine(inputs, welcomeDontShowAgain, activityLog) { stateInputs, dontShowAgain, log ->
                    val screenState: MusicTogetherScreenState =
                        MusicTogetherScreenState.Success(
                            stateInputs.snapshot.toUiModel(
                                dialogState = stateInputs.dialogState,
                                welcomeDismissed = stateInputs.welcomeDismissed,
                                dontShowAgain = dontShowAgain,
                                log = log,
                            ),
                        )
                    screenState
                }
            }.catch { throwable ->
                if (throwable is CancellationException) throw throwable
                emit(MusicTogetherScreenState.Error(R.string.error_unknown))
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MusicTogetherScreenState.Loading,
            )

        init {
            viewModelScope.launch {
                snapshots
                    .map { it.sessionState }
                    .distinctUntilChanged()
                    .collect { sessionState ->
                        updateActivityLog(sessionState)
                    }
            }
        }

        fun attachService(service: MusicService?) {
            attachMusicTogetherService(service)
        }

        fun setWelcomeDontShowAgain(value: Boolean) {
            welcomeDontShowAgain.value = value
        }

        fun dismissWelcomeDialog() {
            welcomeDismissedThisSession.value = true
        }

        fun confirmWelcomeDialog() {
            val dontShowAgain = welcomeDontShowAgain.value
            welcomeDismissedThisSession.value = true
            if (dontShowAgain) {
                viewModelScope.launch(Dispatchers.IO) {
                    updatePreferences.setWelcomeShown(true)
                }
            }
        }

        fun openDisplayNameDialog() {
            val model = successModel() ?: return
            dialog.value = MusicTogetherDialogUiState.DisplayName(model.host.displayName)
        }

        fun submitDisplayName(value: String) {
            val trimmed = value.trim()
            if (trimmed.isBlank()) return
            viewModelScope.launch(Dispatchers.IO) {
                updatePreferences.setDisplayName(trimmed)
            }
        }

        fun openJoinDialog() {
            val model = successModel() ?: return
            if (model.join.disabled || model.join.joining || model.join.joined || model.join.waitingApproval) return
            dialog.value =
                MusicTogetherDialogUiState.Join(
                    initialValue = model.join.input,
                    placeholderResId = model.join.hintResId,
                )
        }

        fun submitJoinInput(value: String) {
            val model = successModel() ?: return
            val trimmed = value.replace("\\s+".toRegex(), "").trim()
            if (trimmed.isBlank()) return
            viewModelScope.launch(Dispatchers.IO) {
                updatePreferences.setLastJoinCode(trimmed)
            }
            viewModelScope.launch {
                sessionActions.joinSession(
                    code = trimmed,
                    displayName = model.host.displayName,
                )
            }
        }

        fun dismissDialog() {
            dialog.value = MusicTogetherDialogUiState.None
        }

        fun setAllowGuestsToAddTracks(value: Boolean) {
            viewModelScope.launch(Dispatchers.IO) {
                updatePreferences.setAllowGuestsToAddTracks(value)
            }
            pushSettingsToActiveSession(addTracks = value)
        }

        fun setAllowGuestsToControlPlayback(value: Boolean) {
            viewModelScope.launch(Dispatchers.IO) {
                updatePreferences.setAllowGuestsToControlPlayback(value)
            }
            pushSettingsToActiveSession(controlPlayback = value)
        }

        fun setRequireHostApprovalToJoin(value: Boolean) {
            viewModelScope.launch(Dispatchers.IO) {
                updatePreferences.setRequireHostApprovalToJoin(value)
            }
            pushSettingsToActiveSession(approval = value)
        }

        fun startSession() {
            val model = successModel() ?: return
            if (!model.host.startEnabled) return
            sessionActions.startSession(
                displayName = model.host.displayName,
                settings =
                    TogetherRoomSettings(
                        allowGuestsToAddTracks = model.host.allowGuestsToAddTracks,
                        allowGuestsToControlPlayback = model.host.allowGuestsToControlPlayback,
                        requireHostApprovalToJoin = model.host.requireHostApprovalToJoin,
                    ),
            )
        }

        fun joinSession() {
            val model = successModel() ?: return
            if (!model.join.canJoin) return
            submitJoinInput(model.join.input)
        }

        fun leaveSession() {
            sessionActions.leaveSession()
        }

        fun copySessionValue(
            @StringRes labelResId: Int,
            value: String,
        ) {
            effectsFlow.tryEmit(MusicTogetherEffect.CopyText(labelResId = labelResId, value = value))
            effectsFlow.tryEmit(MusicTogetherEffect.ToastMessage(R.string.together_copied_to_clipboard))
        }

        fun shareSessionValue(value: String) {
            effectsFlow.tryEmit(MusicTogetherEffect.ShareText(value))
        }

        fun approveParticipant(participantId: String) {
            sessionActions.approveParticipant(participantId, approved = true)
        }

        fun rejectParticipant(participantId: String) {
            sessionActions.approveParticipant(participantId, approved = false)
        }

        fun requestTransferHost(participantId: String) {
            val participant = successModel()?.participants?.find(participantId) ?: return
            dialog.value = MusicTogetherDialogUiState.TransferHost(participant.id, participant.name)
        }

        fun requestKickParticipant(participantId: String) {
            val participant = successModel()?.participants?.find(participantId) ?: return
            dialog.value = MusicTogetherDialogUiState.KickParticipant(participant.id, participant.name)
        }

        fun requestBanParticipant(participantId: String) {
            val participant = successModel()?.participants?.find(participantId) ?: return
            dialog.value = MusicTogetherDialogUiState.BanParticipant(participant.id, participant.name)
        }

        fun confirmKickParticipant(participantId: String) {
            sessionActions.kickParticipant(participantId)
            dismissDialog()
        }

        fun confirmBanParticipant(participantId: String) {
            sessionActions.banParticipant(participantId)
            dismissDialog()
        }

        fun confirmTransferHost(participantId: String) {
            sessionActions.transferHostOwnership(participantId)
            dismissDialog()
        }

        private fun successModel(): MusicTogetherUiModel? =
            when (val current = state.value) {
                is MusicTogetherScreenState.Success -> current.model
                else -> null
            }

        private fun pushSettingsToActiveSession(
            addTracks: Boolean? = null,
            controlPlayback: Boolean? = null,
            approval: Boolean? = null,
        ) {
            val model = successModel() ?: return
            if (!model.status.active || !model.isHostRole) return
            sessionActions.updateSettings(
                TogetherRoomSettings(
                    allowGuestsToAddTracks = addTracks ?: model.host.allowGuestsToAddTracks,
                    allowGuestsToControlPlayback = controlPlayback ?: model.host.allowGuestsToControlPlayback,
                    requireHostApprovalToJoin = approval ?: model.host.requireHostApprovalToJoin,
                ),
            )
        }

        private fun MusicTogetherSnapshot.toUiModel(
            dialogState: MusicTogetherDialogUiState,
            welcomeDismissed: Boolean,
            dontShowAgain: Boolean,
            log: MusicTogetherActivityLogUiModels,
        ): MusicTogetherUiModel {
            val state = sessionState
            val roomState = state.roomStateOrNull()
            val isHosting = state is TogetherSessionState.Hosting
            val isJoining = state is TogetherSessionState.Joining
            val isHostRole =
                when (state) {
                    is TogetherSessionState.Hosting -> true
                    is TogetherSessionState.Joined -> state.role is TogetherRole.Host
                    else -> false
                }
            val isCreatingSessionLoading = false
            val isJoinedAsGuest = state is TogetherSessionState.Joined && state.role is TogetherRole.Guest
            val isWaitingApproval =
                state is TogetherSessionState.Joined &&
                    state.role is TogetherRole.Guest &&
                    state.roomState.participants
                        .firstOrNull { it.id == state.selfParticipantId }
                        ?.isPending == true
            val isJoinedAsAcceptedGuest = isJoinedAsGuest && !isWaitingApproval
            val disableJoinUi = isHostRole || isCreatingSessionLoading || isJoinedAsGuest
            val active = isHosting || state is TogetherSessionState.Joined

            val status =
                MusicTogetherStatusUiModel(
                    titleResId = R.string.together_status,
                    stateLabelResId =
                        when (state) {
                            TogetherSessionState.Idle -> R.string.together_idle
                            is TogetherSessionState.Hosting -> R.string.together_hosting
                            is TogetherSessionState.Joining -> R.string.together_joining
                            is TogetherSessionState.Joined -> if (isWaitingApproval) R.string.together_pending_approval else R.string.together_connected
                            is TogetherSessionState.Error -> R.string.together_error_state
                        },
                    errorMessage = (state as? TogetherSessionState.Error)?.message,
                    iconResId = if (state is TogetherSessionState.Error) R.drawable.error else R.drawable.fire,
                    active = active,
                    error = state is TogetherSessionState.Error,
                    waitingApproval = isWaitingApproval,
                    canLeave = isHosting || active || isJoining || state is TogetherSessionState.Error,
                )

            val sessionShare =
                when (state) {
                    is TogetherSessionState.Hosting -> {
                        MusicTogetherSessionShareUiModel(
                            code = state.code,
                        )
                    }

                    else -> null
                }

            val currentTrack = roomState?.queue?.getOrNull(roomState.currentIndex)
            val playback =
                MusicTogetherPlaybackUiModel(
                    title = currentTrack?.title,
                    artists = currentTrack?.artists?.joinToString(", "),
                    playbackStateResId =
                        if (roomState?.isPlaying == true) {
                            R.string.together_playback_playing
                        } else {
                            R.string.together_playback_paused
                        },
                    queueSize = roomState?.queue?.size ?: 0,
                    currentIndexLabel =
                        roomState?.let {
                            if (it.queue.isNotEmpty()) "${it.currentIndex + 1}/${it.queue.size}" else null
                        },
                    shuffleEnabled = roomState?.shuffleEnabled ?: false,
                    repeatMode = roomState?.repeatMode ?: 0,
                )

            val hostUi =
                MusicTogetherHostUiModel(
                    displayName = preferences.displayName,
                    allowGuestsToAddTracks = preferences.allowGuestsToAddTracks,
                    allowGuestsToControlPlayback = preferences.allowGuestsToControlPlayback,
                    requireHostApprovalToJoin = preferences.requireHostApprovalToJoin,
                    visible = !isJoinedAsGuest,
                    startEnabled = !active && !isJoining && !isCreatingSessionLoading,
                    loading = isCreatingSessionLoading,
                )

            val joinUi =
                MusicTogetherJoinUiModel(
                    input = preferences.lastJoinCode,
                    hintResId = R.string.together_join_code_hint,
                    canJoin = !disableJoinUi && !isJoining && preferences.lastJoinCode.isNotBlank(),
                    disabled = disableJoinUi,
                    joined = isJoinedAsAcceptedGuest,
                    waitingApproval = isWaitingApproval,
                    joining = isJoining,
                )

            val participantsList =
                roomState?.participants?.map { participant ->
                    MusicTogetherParticipantUiModel(
                        id = participant.id,
                        name = participant.name,
                        host = participant.isHost,
                        pending = participant.isPending,
                        connected = participant.isConnected,
                        showApproveActions = isHostRole && participant.isPending,
                        showModerationActions = isHostRole && !participant.isHost,
                        showTransferHostAction = isHostRole && !participant.isHost && !participant.isPending,
                    )
                } ?: emptyList()

            val isInSession = isHosting || isJoining || state is TogetherSessionState.Joined

            return MusicTogetherUiModel(
                showWelcomeDialog = !preferences.welcomeShown && !welcomeDismissed,
                welcomeDontShowAgain = dontShowAgain,
                dialog = dialogState,
                status = status,
                sessionShare = sessionShare,
                playback = playback,
                host = hostUi,
                join = joinUi,
                participants = MusicTogetherParticipantUiModels(participantsList),
                activityLog = log,
                isInSession = isInSession,
                isHostRole = isHostRole,
            )
        }

        private suspend fun updateActivityLog(sessionState: TogetherSessionState) {
            withContext(Dispatchers.Main) {
                val sessionId = sessionState.sessionIdOrNull()
                val currentRoomState = sessionState.roomStateOrNull()

                if (sessionId == null && lastSessionId != null) {
                    lastSessionId = null
                    lastRoomState = null
                    lastStateWasError = false
                    clearLog()
                    return@withContext
                }

                if (sessionId == null) return@withContext

                if (lastSessionId != sessionId) {
                    lastSessionId = sessionId
                    lastRoomState = null
                    lastStateWasError = false
                    clearLog()
                    addLog(
                        iconResId = R.drawable.fire,
                        messageResId =
                            when (sessionState) {
                                is TogetherSessionState.Joining -> R.string.together_activity_joining_session
                                else -> R.string.together_activity_session_started
                            },
                        args = emptyList(),
                    )
                }

                if (sessionState is TogetherSessionState.Error && !lastStateWasError) {
                    addLog(
                        iconResId = R.drawable.error,
                        messageResId = R.string.together_activity_error,
                        args = listOf(sessionState.message.ifBlank { "" }),
                    )
                    lastStateWasError = true
                } else if (sessionState !is TogetherSessionState.Error) {
                    lastStateWasError = false
                }

                if (currentRoomState != null) {
                    val previousRoomState = lastRoomState
                    if (previousRoomState != null) {
                        appendRoomStateDiff(previousRoomState, currentRoomState)
                    }
                    lastRoomState = currentRoomState
                }

                activityLog.value = MusicTogetherActivityLogUiModels(logItems.toList())
            }
        }

        private fun appendRoomStateDiff(
            previous: TogetherRoomState,
            current: TogetherRoomState,
        ) {
            val previousParticipants = previous.participants.associateBy { it.id }
            val currentParticipants = current.participants.associateBy { it.id }
            current.participants.forEach { participant ->
                if (previousParticipants[participant.id] == null && !participant.isHost) {
                    addLog(
                        iconResId = R.drawable.person,
                        messageResId =
                            if (participant.isPending) {
                                R.string.together_activity_join_requested
                            } else {
                                R.string.together_participant_joined_notification
                            },
                        args = listOf(participant.name),
                    )
                }
            }
            previous.participants.forEach { participant ->
                if (currentParticipants[participant.id] == null && !participant.isHost) {
                    addLog(
                        iconResId = R.drawable.leave,
                        messageResId = R.string.together_participant_left_notification,
                        args = listOf(participant.name),
                    )
                }
            }

            if (previous.hostId != current.hostId) {
                addLog(
                    iconResId = R.drawable.sync,
                    messageResId = R.string.together_activity_host_transferred,
                    args = listOf(current.actorName(current.hostId)),
                )
            }

            val actor = current.actorName(current.hostId)
            if (previous.isPlaying != current.isPlaying) {
                addLog(
                    iconResId = if (current.isPlaying) R.drawable.play else R.drawable.pause,
                    messageResId = if (current.isPlaying) R.string.together_activity_playback_resumed else R.string.together_activity_playback_paused,
                    args = listOf(actor),
                )
            }

            val previousTrack = previous.queue.getOrNull(previous.currentIndex)
            val currentTrack = current.queue.getOrNull(current.currentIndex)
            if (previousTrack?.id != currentTrack?.id && currentTrack != null) {
                addLog(
                    iconResId = R.drawable.music_note,
                    messageResId = R.string.together_activity_song_changed,
                    args = listOf(actor, currentTrack.title),
                )
            } else if (previous.currentIndex != current.currentIndex && currentTrack != null) {
                addLog(
                    iconResId = R.drawable.skip_next,
                    messageResId = R.string.together_activity_song_skipped,
                    args = listOf(actor, currentTrack.title),
                )
            }

            if (previous.queueHash != current.queueHash || previous.queue.size != current.queue.size) {
                addLog(
                    iconResId = R.drawable.queue_music,
                    messageResId = R.string.together_activity_queue_updated,
                    args = listOf(actor),
                )
            }

            if (previous.shuffleEnabled != current.shuffleEnabled || previous.repeatMode != current.repeatMode) {
                addLog(
                    iconResId = R.drawable.settings,
                    messageResId = R.string.together_activity_playback_state_changed,
                    args = listOf(actor),
                )
            }
        }

        private fun TogetherRoomState.actorName(participantId: String): String =
            participants.firstOrNull { it.id == participantId }?.name
                ?: participants.firstOrNull { it.isHost }?.name
                ?: context.getString(R.string.together_role_host)

        private fun addLog(
            @DrawableRes iconResId: Int,
            @StringRes messageResId: Int,
            args: List<String>,
        ) {
            logItems.addFirst(
                MusicTogetherActivityLogItemUiModel(
                    id = nextLogId++,
                    iconResId = iconResId,
                    messageResId = messageResId,
                    args = args,
                    timestamp = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date()),
                ),
            )
            while (logItems.size > MaxActivityLogSize) {
                logItems.removeLast()
            }
        }

        private fun clearLog() {
            logItems.clear()
            activityLog.value = MusicTogetherActivityLogUiModels(emptyList())
        }

        private fun TogetherSessionState.sessionIdOrNull(): String? =
            when (this) {
                is TogetherSessionState.Hosting -> sessionId
                is TogetherSessionState.Joined -> sessionId
                is TogetherSessionState.Joining -> null
                is TogetherSessionState.Error,
                TogetherSessionState.Idle,
                -> null
            }

        private fun TogetherSessionState.roomStateOrNull(): TogetherRoomState? =
            when (this) {
                is TogetherSessionState.Hosting -> roomState
                is TogetherSessionState.Joined -> roomState
                else -> null
            }

        private fun MusicTogetherParticipantUiModels.find(participantId: String): MusicTogetherParticipantUiModel? {
            for (index in 0 until size) {
                val participant = get(index)
                if (participant.id == participantId) return participant
            }
            return null
        }

        private companion object {
            const val MaxActivityLogSize = 80
        }
    }
