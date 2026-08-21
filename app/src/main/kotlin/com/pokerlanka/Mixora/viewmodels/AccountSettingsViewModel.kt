/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pokerlanka.mixora.App
import com.pokerlanka.mixora.utils.SyncUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val syncUtils: SyncUtils,
) : ViewModel() {

    /**
     * Logout user and clear all synced content to prevent data mixing between accounts
     */
    fun logoutAndClearSyncedContent(context: Context, onCookieChange: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            // Clear all YouTube Music synced content first
            syncUtils.clearAllSyncedContent()

            // Then clear account preferences
            App.forgetAccount(context)

            // Clear cookie in UI
            onCookieChange("")
        }
    }

    /**
     * Clear all library data including songs, albums, artists, playlists, podcasts.
     */
    suspend fun clearAllLibraryData() {
        Timber.d("[LOGOUT_CLEAR] ViewModel: clearAllLibraryData called")
        syncUtils.clearAllLibraryData()
        Timber.d("[LOGOUT_CLEAR] ViewModel: clearAllLibraryData completed")
    }

    /**
     * Forget the account FIRST (clearing auth so all background syncs skip),
     * THEN clear all library data. This prevents sync operations that are
     * triggered by the database becoming empty from re-adding songs.
     */
    suspend fun logoutAndClearLibraryData(
        context: Context,
        signOutOfGoogle: Boolean = false,
    ) {
        Timber.d("[LOGOUT_CLEAR] ViewModel: logoutAndClearLibraryData called")
        withContext(Dispatchers.IO) {
            // Forget account first — clears cookie/auth from DataStore.
            // Once isLoggedIn() returns false, ALL sync operations will skip.
            App.forgetAccount(context, signOutOfGoogle)

            // Now clear the local database. Any sync coroutines that observe
            // the empty state will check isLoggedIn() and skip silently.
            syncUtils.clearAllLibraryData()
        }
        Timber.d("[LOGOUT_CLEAR] ViewModel: logoutAndClearLibraryData completed")
    }

    /**
     * Just logout without clearing library data
     */
    suspend fun logoutKeepData(
        context: Context,
        onCookieChange: (String) -> Unit,
        signOutOfGoogle: Boolean = false,
    ) {
        Timber.d("[LOGOUT_KEEP] ViewModel: logoutKeepData called")
        withContext(Dispatchers.IO) {
            App.forgetAccount(context, signOutOfGoogle)
        }
        Timber.d("[LOGOUT_KEEP] ViewModel: Account forgotten, clearing cookie in UI")
        onCookieChange("")
    }

}
