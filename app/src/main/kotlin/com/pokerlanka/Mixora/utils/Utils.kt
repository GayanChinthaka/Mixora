/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.utils

import android.content.ClipboardManager
import android.content.Context
import com.pokerlanka.mixora.R

fun getArtistSeparator(context: Context): String = " ${context.getString(R.string.and)} "

fun <T> List<T>.joinToArtistString(
    conjunction: String,
    transform: (T) -> String,
): String = when (size) {
    0 -> ""
    1 -> transform(this[0])
    2 -> "${transform(this[0])}$conjunction${transform(this[1])}"
    else -> dropLast(1).joinToString(", ") { transform(it) } + "$conjunction${transform(last())}"
}

fun reportException(throwable: Throwable) {
    throwable.printStackTrace()
}

/**
 * Current clipboard contents as plain text, or null when the clipboard is empty.
 *
 * Replaces Compose's deprecated `LocalClipboardManager.getText()` and matches how the rest of the
 * app already talks to the clipboard. [android.content.ClipData.Item.coerceToText] is used so a
 * copied URI or styled text still pastes as something sensible.
 */
fun ClipboardManager.readPlainText(context: Context): String? =
    primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.coerceToText(context)
        ?.toString()
