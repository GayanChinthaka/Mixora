package com.pokerlanka.lrclib.models

import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class Track(
    val id: Int,
    val trackName: String,
    val artistName: String,
    val duration: Double,
    val plainLyrics: String?,
    val syncedLyrics: String?,
)

/** Duration window a candidate must fall inside before its name is even considered. */
private const val DURATION_TOLERANCE_SECONDS = 5

/** Below this window the duration alone is strong enough evidence to forgive a differing artist. */
private const val DURATION_STRONG_MATCH_SECONDS = 2

private const val MIN_TITLE_SIMILARITY = 0.6
private const val MIN_ARTIST_SIMILARITY = 0.5

// Relaxed matching with ±5 seconds tolerance. Duration only - callers that know the track name
// should prefer the [bestMatchingFor] overload below, which also checks the name.
internal fun List<Track>.bestMatchingForRelaxed(duration: Int): Track? {
    if (isEmpty()) return null

    if (duration == -1) {
        return firstOrNull { it.syncedLyrics != null } ?: firstOrNull()
    }

    // First try to find synced lyrics within tolerance
    val syncedMatch = filter { it.syncedLyrics != null }
        .minByOrNull { abs(it.duration.toInt() - duration) }
        ?.takeIf { abs(it.duration.toInt() - duration) <= DURATION_TOLERANCE_SECONDS }

    if (syncedMatch != null) return syncedMatch

    // Fall back to any lyrics within tolerance
    return minByOrNull { abs(it.duration.toInt() - duration) }
        ?.takeIf { abs(it.duration.toInt() - duration) <= DURATION_TOLERANCE_SECONDS }
}

/**
 * Picks the candidate that matches the playing track on **both** duration and name.
 *
 * Duration alone is not enough: `LrcLib.queryLyrics` deliberately widens its search by dropping the
 * artist (and eventually the artist *and* the exact title) when the strict query returns nothing, so
 * the candidate list regularly contains covers, remixes and entirely unrelated songs that happen to
 * share a title and run for about as long. Matching on duration only made those win, which is how
 * the wrong lyrics ended up attached to a song.
 */
internal fun List<Track>.bestMatchingFor(
    duration: Int,
    trackName: String? = null,
    artistName: String? = null,
): Track? {
    if (isEmpty()) return null

    if (trackName == null || artistName == null) {
        return bestMatchingForRelaxed(duration)
    }

    if (duration == -1) {
        return findBestMatch(trackName, artistName, duration = -1)
    }

    val withinTolerance = filter { abs(it.duration.toInt() - duration) <= DURATION_TOLERANCE_SECONDS }
    if (withinTolerance.isEmpty()) return null

    return withinTolerance.findBestMatch(trackName, artistName, duration)
}

private fun List<Track>.findBestMatch(
    trackName: String,
    artistName: String,
    duration: Int,
): Track? {
    val normalizedTrackName = trackName.trim().lowercase()
    val normalizedArtistName = artistName.trim().lowercase()

    fun titleScore(track: Track) = calculateSimilarity(normalizedTrackName, track.trackName.trim().lowercase())
    fun artistScore(track: Track) = calculateSimilarity(normalizedArtistName, track.artistName.trim().lowercase())

    val best = maxByOrNull { track ->
        var score = (titleScore(track) + artistScore(track)) / 2.0

        if (track.syncedLyrics != null) score += 0.1

        // Break ties towards the closest duration so two equally named candidates cannot be picked
        // by list order alone.
        if (duration != -1) {
            score += (1.0 - abs(track.duration.toInt() - duration) / (DURATION_TOLERANCE_SECONDS + 1.0)) * 0.05
        }

        score
    } ?: return null

    val titleSimilarity = titleScore(best)
    val artistSimilarity = artistScore(best)

    // The title has to line up. A weak artist match is tolerated only when the runtime is a near
    // exact match, which covers "Artist" vs "Artist feat. Someone" style metadata differences
    // without letting a same-titled different song through.
    val durationIsStrongEvidence =
        duration != -1 && abs(best.duration.toInt() - duration) <= DURATION_STRONG_MATCH_SECONDS

    val accepted = titleSimilarity >= MIN_TITLE_SIMILARITY &&
        (artistSimilarity >= MIN_ARTIST_SIMILARITY || durationIsStrongEvidence)

    return best.takeIf { accepted }
}

private fun calculateSimilarity(str1: String, str2: String): Double {
    if (str1 == str2) return 1.0
    if (str1.isEmpty() || str2.isEmpty()) return 0.0

    val containsScore = when {
        str1.contains(str2) || str2.contains(str1) -> 0.8
        else -> 0.0
    }

    val maxLength = maxOf(str1.length, str2.length)
    val distance = levenshteinDistance(str1, str2)
    val distanceScore = 1.0 - (distance.toDouble() / maxLength)

    return maxOf(containsScore, distanceScore)
}

private fun levenshteinDistance(str1: String, str2: String): Int {
    val len1 = str1.length
    val len2 = str2.length
    val matrix = Array(len1 + 1) { IntArray(len2 + 1) }

    for (i in 0..len1) matrix[i][0] = i
    for (j in 0..len2) matrix[0][j] = j

    for (i in 1..len1) {
        for (j in 1..len2) {
            val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
            matrix[i][j] = minOf(
                matrix[i - 1][j] + 1,      // deletion
                matrix[i][j - 1] + 1,      // insertion
                matrix[i - 1][j - 1] + cost // substitution
            )
        }
    }

    return matrix[len1][len2]
}
