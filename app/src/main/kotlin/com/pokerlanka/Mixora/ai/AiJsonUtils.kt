/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.ai

import org.json.JSONArray
import org.json.JSONObject

internal fun extractJsonArray(raw: String): JSONArray {
    val trimmed =
        raw
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    val start = trimmed.indexOf('[')
    val end = trimmed.lastIndexOf(']')
    require(start >= 0 && end > start) { "AI response did not contain a JSON array" }
    return JSONArray(trimmed.substring(start, end + 1))
}

internal fun JSONObject.readErrorMessage(): String? {
    val error = optJSONObject("error")
    return error?.optString("message")?.takeIf { it.isNotBlank() }
        ?: optString("error").takeIf { it.isNotBlank() }
}
