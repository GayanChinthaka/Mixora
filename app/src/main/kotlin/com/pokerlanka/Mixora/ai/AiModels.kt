/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.ai

import androidx.compose.runtime.Immutable
import com.pokerlanka.mixora.constants.AiProvider

/**
 * Coarse grouping of what a model produces, derived from the provider's model listing.
 * Ordinal order doubles as the picker's sort order, so text models surface first.
 */
enum class AiModelCategory {
    TEXT,
    COMPUTER_USE,
    AGENT,
    IMAGE,
    VIDEO,
    MUSIC,
    SPEECH,
    LIVE,
    GROUNDED_QA,
    EMBEDDING,
    OTHER,
    ;

    /** Only plain text generation can be driven by [AiTextService]'s single request shape. */
    val isCallableForText: Boolean
        get() = this == TEXT
}

@Immutable
data class AiModelOption(
    val id: String,
    val displayName: String,
    val category: AiModelCategory = AiModelCategory.TEXT,
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
