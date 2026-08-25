package com.pokerlanka.mixora.ui.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pokerlanka.mixora.LocalDatabase
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.db.entities.SongEntity
import kotlinx.coroutines.FlowPreview
import java.util.Locale
import kotlin.math.roundToInt

private const val MIN_OFFSET_MS = -20000
private const val MAX_OFFSET_MS = 20000
private const val STEP_MS = 500

@OptIn(FlowPreview::class)
@Composable
fun ShowOffsetDialog(songProvider: () -> SongEntity?) {
    val database = LocalDatabase.current
    val song = songProvider()
    var lyricsOffset by rememberSaveable { mutableIntStateOf(song?.lyricsOffset ?: 0) }

    LaunchedEffect(song?.id) {
        song?.let {
            lyricsOffset = it.lyricsOffset
        }
    }

    LaunchedEffect(lyricsOffset) {
        songProvider()?.let { currentSong ->
            database.query {
                upsert(
                    currentSong.copy(
                        lyricsOffset = lyricsOffset
                    )
                )
            }
        }
    }

    val offsetSeconds = lyricsOffset / 1000f
    val formattedSeconds = if (lyricsOffset == 0) {
        "0.0s"
    } else {
        "${if (lyricsOffset > 0) "+" else ""}${String.format(Locale.US, "%.1f", offsetSeconds)}s"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // Header: Title and Reset button in one compact row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.lyrics_offset),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (lyricsOffset != 0) {
                TextButton(
                    onClick = { lyricsOffset = 0 },
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.replay),
                        contentDescription = "Reset",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Reset",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Spacer(Modifier.size(32.dp))
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Main Adjustment Row: -0.5s | +1.5s (1500ms) | +0.5s
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Decrease button (-500ms / -0.5s)
            FilledTonalIconButton(
                onClick = {
                    lyricsOffset = (lyricsOffset - STEP_MS).coerceIn(MIN_OFFSET_MS, MAX_OFFSET_MS)
                },
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.remove),
                    contentDescription = "Decrease 0.5s",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Center Value Display (Seconds + ms)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formattedSeconds,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (lyricsOffset == 0) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Text(
                    text = "${if (lyricsOffset > 0) "+" else ""}${lyricsOffset} ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Increase button (+500ms / +0.5s)
            FilledTonalIconButton(
                onClick = {
                    lyricsOffset = (lyricsOffset + STEP_MS).coerceIn(MIN_OFFSET_MS, MAX_OFFSET_MS)
                },
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = "Increase 0.5s",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Slider (-20s to +20s with 0.5s steps)
        Slider(
            value = lyricsOffset.toFloat(),
            onValueChange = { newValue ->
                val rounded = ((newValue / STEP_MS.toFloat()).roundToInt()) * STEP_MS
                lyricsOffset = rounded.coerceIn(MIN_OFFSET_MS, MAX_OFFSET_MS)
            },
            valueRange = MIN_OFFSET_MS.toFloat()..MAX_OFFSET_MS.toFloat(),
            steps = ((MAX_OFFSET_MS - MIN_OFFSET_MS) / STEP_MS) - 1, // 79 steps
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )

        // Range Labels: -20s | 0s | +20s
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = "-20s",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = "0s",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = "+20s",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
