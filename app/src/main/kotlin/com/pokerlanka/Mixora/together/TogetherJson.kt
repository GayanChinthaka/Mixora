/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.together

import kotlinx.serialization.json.Json

val TogetherJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
    allowStructuredMapKeys = true
}
