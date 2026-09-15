/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.lyrics

object LyricsProviderRegistry {
    private val providerMap = mapOf(
        "LrcLib" to LrcLibLyricsProvider,
        "Paxsenix" to PaxsenixLyricsProvider,
        "YouTubeSubtitle" to YouTubeSubtitleLyricsProvider,
        "YouTube" to YouTubeLyricsProvider,
        "SinhalaLyrics" to SinhalaLyricsProvider,
        "AiLyrics" to AiLyricsProvider,
    )

    val providerNames = providerMap.keys.toList()

    fun getProviderByName(name: String): LyricsProvider? {
        providerMap[name]?.let { return it }
        // Case-insensitive lookup
        providerMap.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.let { return it }
        // Alternate name matching
        return when (name.lowercase().replace(" ", "")) {
            "sinhalalyrics", "sinhala" -> SinhalaLyricsProvider
            "ailyrics", "ai", "aifallback" -> AiLyricsProvider
            "youtube", "youtubemusic" -> YouTubeLyricsProvider
            "youtubesubtitle", "youtubesubtitles" -> YouTubeSubtitleLyricsProvider
            "lrclib" -> LrcLibLyricsProvider
            "paxsenix" -> PaxsenixLyricsProvider
            else -> null
        }
    }

    fun getProviderName(provider: LyricsProvider): String? =
        providerMap.entries.find { it.value == provider }?.key

    fun deserializeProviderOrder(orderString: String): List<String> {
        val defaults = getDefaultProviderOrder()
        if (orderString.isBlank()) {
            return defaults
        }
        val saved = orderString
            .split(",")
            .map { it.trim() }
            .filter { it in providerNames }
        val missing = defaults.filter { it !in saved }
        return (saved + missing).ifEmpty { defaults }
    }

    fun serializeProviderOrder(providers: List<String>): String {
        return providers.filter { it in providerNames }.joinToString(",")
    }

    fun getDefaultProviderOrder(): List<String> = listOf(
        "LrcLib",
        "Paxsenix",
        "YouTube",
        "YouTubeSubtitle",
        "SinhalaLyrics",
    )

    fun getOrderedProviders(orderString: String): List<LyricsProvider> {
        val order = deserializeProviderOrder(orderString)
        return order.mapNotNull { getProviderByName(it) }
    }
}
