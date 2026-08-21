/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pokerlanka.mixora.R

/**
 * Transient "Romanizing…" pill shown while an AI romanization request is in flight.
 *
 * Romanization is a network round trip that lands seconds after the lyrics are already on screen,
 * so without this the sheet looks finished-but-wrong: the original script sits there with no hint
 * that anything more is coming. Deliberately shown only for a live request — a cache hit resolves
 * before the next frame and flashing a spinner for it would read as a glitch.
 */
@Composable
fun RomanizingIndicator(
    visible: Boolean,
    modifier: Modifier = Modifier,
    accent: Color? = null,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        val tint = accent ?: MaterialTheme.colorScheme.primary
        Surface(
            shape = RoundedCornerShape(percent = 50),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
            contentColor = tint,
            modifier = Modifier.padding(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = tint,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(R.string.romanizing_in_progress),
                    style = MaterialTheme.typography.labelMedium,
                    color = tint,
                )
            }
        }
    }
}
