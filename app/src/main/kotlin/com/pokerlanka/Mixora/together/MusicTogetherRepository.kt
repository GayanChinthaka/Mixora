/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.together

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.constants.TogetherAllowGuestsToAddTracksKey
import com.pokerlanka.mixora.constants.TogetherAllowGuestsToControlPlaybackKey
import com.pokerlanka.mixora.constants.TogetherDisplayNameKey
import com.pokerlanka.mixora.constants.TogetherLastJoinCodeKey
import com.pokerlanka.mixora.constants.TogetherRequireHostApprovalToJoinKey
import com.pokerlanka.mixora.constants.TogetherWelcomeShownKey
import com.pokerlanka.mixora.playback.MusicService
import com.pokerlanka.mixora.utils.dataStore
import javax.inject.Inject
import javax.inject.Singleton

data class MusicTogetherPreferences(
    val displayName: String,
    val allowGuestsToAddTracks: Boolean,
    val allowGuestsToControlPlayback: Boolean,
    val requireHostApprovalToJoin: Boolean,
    val lastJoinCode: String,
    val welcomeShown: Boolean,
)

data class MusicTogetherSnapshot(
    val preferences: MusicTogetherPreferences,
    val sessionState: TogetherSessionState,
)

@Singleton
class MusicTogetherRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val serviceFlow = MutableStateFlow<MusicService?>(null)

        val preferences: Flow<MusicTogetherPreferences> =
            context.dataStore.data
                .map { preferences ->
                    MusicTogetherPreferences(
                        displayName =
                            preferences[TogetherDisplayNameKey]
                                ?: Build.MODEL?.takeIf { it.isNotBlank() }
                                ?: context.getString(R.string.app_name),
                        allowGuestsToAddTracks = preferences[TogetherAllowGuestsToAddTracksKey] ?: true,
                        allowGuestsToControlPlayback = preferences[TogetherAllowGuestsToControlPlaybackKey] ?: false,
                        requireHostApprovalToJoin = preferences[TogetherRequireHostApprovalToJoinKey] ?: false,
                        lastJoinCode = preferences[TogetherLastJoinCodeKey] ?: "",
                        welcomeShown = preferences[TogetherWelcomeShownKey] ?: false,
                    )
                }.distinctUntilChanged()

        @OptIn(ExperimentalCoroutinesApi::class)
        val sessionState: Flow<TogetherSessionState> =
            serviceFlow.flatMapLatest { service ->
                service?.togetherSessionState ?: flowOf(TogetherSessionState.Idle)
            }

        fun attachService(service: MusicService?) {
            serviceFlow.value = service
        }

        suspend fun setDisplayName(displayName: String) {
            context.dataStore.edit { preferences ->
                preferences[TogetherDisplayNameKey] = displayName
            }
        }

        suspend fun setAllowGuestsToAddTracks(value: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[TogetherAllowGuestsToAddTracksKey] = value
            }
        }

        suspend fun setAllowGuestsToControlPlayback(value: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[TogetherAllowGuestsToControlPlaybackKey] = value
            }
        }

        suspend fun setRequireHostApprovalToJoin(value: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[TogetherRequireHostApprovalToJoinKey] = value
            }
        }

        suspend fun setLastJoinCode(value: String) {
            context.dataStore.edit { preferences ->
                preferences[TogetherLastJoinCodeKey] = value
            }
        }

        suspend fun setWelcomeShown(value: Boolean) {
            context.dataStore.edit { preferences ->
                preferences[TogetherWelcomeShownKey] = value
            }
        }

        fun startSession(
            displayName: String,
            settings: TogetherRoomSettings,
        ) {
            val service = serviceFlow.value ?: return
            service.startTogetherHost(
                displayName = displayName,
                settings = settings,
            )
        }

        suspend fun joinSession(
            code: String,
            displayName: String,
        ) {
            val service = serviceFlow.value ?: return
            val cleanCode = code.replace("\\s+".toRegex(), "").trim()
            if (cleanCode.length != 6 || !cleanCode.all { it.isDigit() }) {
                service.setTogetherError("Please enter a valid 6-digit room code")
                return
            }

            service.setTogetherJoining(cleanCode)
            val joinInfo = TogetherLanDiscovery.resolveCode(context, cleanCode)
            if (joinInfo != null) {
                service.joinTogether(joinInfo, displayName)
            } else {
                service.setTogetherError("Room '$cleanCode' not found on local Wi-Fi. Ensure both devices are connected to the same Wi-Fi network.")
            }
        }

        fun leaveSession() {
            serviceFlow.value?.leaveTogether()
        }

        fun updateSettings(settings: TogetherRoomSettings) {
            serviceFlow.value?.updateTogetherSettings(settings)
        }

        fun approveParticipant(
            participantId: String,
            approved: Boolean,
        ) {
            serviceFlow.value?.approveTogetherParticipant(participantId, approved)
        }

        fun kickParticipant(participantId: String) {
            serviceFlow.value?.kickTogetherParticipant(participantId)
        }

        fun banParticipant(participantId: String) {
            serviceFlow.value?.banTogetherParticipant(participantId)
        }

        fun transferHostOwnership(participantId: String) {
            serviceFlow.value?.transferTogetherHostOwnership(participantId)
        }
    }
