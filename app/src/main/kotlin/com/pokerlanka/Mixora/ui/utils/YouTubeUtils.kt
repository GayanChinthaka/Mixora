/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

@file:Suppress("LocalVariableName")

package com.pokerlanka.mixora.ui.utils

/**
 * Edge length requested for now-playing artwork — the player pager, the queue, and whatever the
 * media session hands to Coil.
 *
 * These URLs used to ask for 1080x1080, which decodes to ~4.6 MB of heap per track and is a
 * noticeably slower download on a weak connection. Album art carries very little fine detail, so
 * the cost was buying almost nothing on screen.
 */
const val PLAYER_ARTWORK_SIZE = 512

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this
    // Match BOTH lh3 and yt3 googleusercontent: YouTube migrated music/album art from
    // lh3.googleusercontent.com to yt3.googleusercontent.com. Both serve the same =wW-hH resize
    // params; matching only lh3 silently no-ops on the new host, so the player upscales the raw
    // ~60px thumbnail (blurry). Verified live: a yt3 URL + =w544-h544 returns a sharp full-size image.
    "https://(?:lh3|yt3)\\.googleusercontent\\.com/.*=w(\\d+)-h(\\d+).*".toRegex()
        .matchEntire(this)?.groupValues?.let { group ->
        val (W, H) = group.drop(1).map { it.toInt() }
        var w = width
        var h = height
        if (w != null && h == null) h = (w / W) * H
        if (w == null && h != null) w = (h / H) * W
        return "${split("=w")[0]}=w$w-h$h-p-l90-rj"
    }
    if (this matches "https://yt3\\.ggpht\\.com/.*=s(\\d+)".toRegex()) {
        return "$this-s${width ?: height}"
    }
    return this
}
