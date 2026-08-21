/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.together

import kotlinx.serialization.json.Json

val TogetherJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
    allowStructuredMapKeys = true
}
