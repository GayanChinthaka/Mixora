/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.lyrics

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object LyricsResyncHelper {
    private val _resyncTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val resyncTrigger: SharedFlow<Unit> = _resyncTrigger.asSharedFlow()

    fun triggerResync() {
        _resyncTrigger.tryEmit(Unit)
    }
}
