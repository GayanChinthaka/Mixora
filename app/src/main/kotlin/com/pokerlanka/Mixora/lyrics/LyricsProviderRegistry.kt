/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.lyrics

object LyricsProviderRegistry {
    private val providerMap = mapOf(
        "LrcLib" to LrcLibLyricsProvider,
        "Musixmatch" to MusixmatchLyricsProvider,
        "Paxsenix" to PaxsenixLyricsProvider,
        "KuGou" to KuGouLyricsProvider,
        "YouTube" to YouTubeLyricsProvider,
        "YouTubeSubtitle" to YouTubeSubtitleLyricsProvider,
    )

    val providerNames = providerMap.keys.toList()

    fun getProviderByName(name: String): LyricsProvider? {
        providerMap[name]?.let { return it }
        // Case-insensitive lookup
        providerMap.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.let { return it }
        // Alternate name matching
        return when (name.lowercase().replace(" ", "")) {
            "kugou" -> KuGouLyricsProvider
            "youtube", "youtubemusic" -> YouTubeLyricsProvider
            "youtubesubtitle", "youtubesubtitles" -> YouTubeSubtitleLyricsProvider
            "lrclib" -> LrcLibLyricsProvider
            "musixmatch" -> MusixmatchLyricsProvider
            "paxsenix" -> PaxsenixLyricsProvider
            else -> null
        }
    }

    fun getProviderName(provider: LyricsProvider): String? =
        providerMap.entries.find { it.value == provider }?.key

    fun deserializeProviderOrder(orderString: String): List<String> {
        if (orderString.isBlank()) {
            return getDefaultProviderOrder()
        }
        return orderString.split(",").map { it.trim() }.filter { it in providerNames }
    }

    fun serializeProviderOrder(providers: List<String>): String {
        return providers.filter { it in providerNames }.joinToString(",")
    }

    fun getDefaultProviderOrder(): List<String> = listOf(
        "LrcLib",
        "Musixmatch",
        "Paxsenix",
        "KuGou",
        "YouTube",
        "YouTubeSubtitle",
    )

    fun getOrderedProviders(orderString: String): List<LyricsProvider> {
        val order = deserializeProviderOrder(orderString)
        return order.mapNotNull { getProviderByName(it) }
    }
}
