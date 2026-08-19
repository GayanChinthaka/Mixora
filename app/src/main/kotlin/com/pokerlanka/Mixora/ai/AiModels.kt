/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.ai

import androidx.compose.runtime.Immutable
import com.pokerlanka.mixora.constants.AiProvider

@Immutable
data class AiModelOption(
    val id: String,
    val displayName: String,
)

@Immutable
data class AiServiceConfig(
    val provider: AiProvider,
    val apiKey: String,
    val customEndpoint: String,
    val model: String,
) {
    val canCallApi: Boolean
        get() =
            provider != AiProvider.NONE &&
                apiKey.isNotBlank() &&
                (provider != AiProvider.CUSTOM || customEndpoint.isNotBlank())
}
