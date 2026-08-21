/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.pokerlanka.mixora.viewmodels.LyricsViewModel

/**
 * Entry point for the player's lyrics sheet.
 *
 * Previously chose between two full implementations behind an `experimentalLyrics` preference;
 * the older one has been removed, so this is now a thin pass-through. Kept as the public name
 * so call sites do not have to care which implementation backs it.
 */
@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    showLyrics: Boolean,
    lyricsViewModel: LyricsViewModel = hiltViewModel(),
    textColorOverride: Color? = null,
) {
    ExperimentalLyrics(
        sliderPositionProvider = sliderPositionProvider,
        modifier = modifier,
        showLyrics = showLyrics,
        lyricsViewModel = lyricsViewModel,
        textColorOverride = textColorOverride,
    )
}
