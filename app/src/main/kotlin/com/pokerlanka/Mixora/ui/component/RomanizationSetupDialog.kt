/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pokerlanka.mixora.R

/**
 * Explains that romanization needs an AI provider and offers a shortcut to set one up.
 *
 * Shared by the Lyrics settings screen and the player's lyrics menu so both entry points give the
 * same explanation — the menu previously only raised a toast, which was easy to miss and offered
 * no way to act on it.
 */
@Composable
fun RomanizationSetupDialog(
    onDismiss: () -> Unit,
    onSetUp: () -> Unit,
) {
    AlertDialog(
        // Inset from the screen edges: without this the dialog stretches nearly edge to edge on
        // phones and reads as a page rather than a dialog.
        modifier = Modifier.padding(horizontal = 24.dp),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.romanization_setup_title)) },
        text = { Text(stringResource(R.string.romanization_setup_message)) },
        confirmButton = {
            TextButton(onClick = onSetUp) {
                Text(stringResource(R.string.romanization_setup_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
