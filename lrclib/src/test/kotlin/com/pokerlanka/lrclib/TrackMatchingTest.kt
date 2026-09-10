package com.pokerlanka.lrclib

import com.pokerlanka.lrclib.models.Track
import com.pokerlanka.lrclib.models.bestMatchingFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TrackMatchingTest {

    private fun track(
        id: Int,
        title: String,
        artist: String,
        duration: Double,
        synced: String? = "[00:01.00] Test",
    ) = Track(
        id = id,
        trackName = title,
        artistName = artist,
        duration = duration,
        plainLyrics = "Test",
        syncedLyrics = synced,
    )

    @Test
    fun `exact title and artist matches within standard tolerance`() {
        val tracks = listOf(
            track(1, "Freedom", "Akon", 253.0),
        )
        val match = tracks.bestMatchingFor(duration = 255, trackName = "Freedom", artistName = "Akon")
        assertNotNull(match)
        assertEquals(1, match?.id)
    }

    @Test
    fun `extended duration tolerance allows matching YouTube video intro or outro for exact track`() {
        val tracks = listOf(
            track(1, "Freedom", "Akon", 253.0),
        )
        // YouTube video has intro/outro making it 262 seconds (9 seconds difference)
        val match = tracks.bestMatchingFor(duration = 262, trackName = "Freedom", artistName = "Akon")
        assertNotNull(match)
        assertEquals(1, match?.id)
    }

    @Test
    fun `unrelated song with differing title is rejected even if duration is close`() {
        val tracks = listOf(
            track(2, "Sunny Day", "Akon", 232.0),
            track(3, "Keep You Much Longer", "Akon", 260.0),
        )
        // Searching for Freedom at 260s should NOT match "Keep You Much Longer"
        val match = tracks.bestMatchingFor(duration = 260, trackName = "Freedom", artistName = "Akon")
        assertNull(match)
    }

    @Test
    fun `picks closest duration among same-named candidates`() {
        val tracks = listOf(
            track(1, "Freedom", "Akon", 253.0),
            track(2, "Freedom", "Akon", 256.0),
        )
        val match = tracks.bestMatchingFor(duration = 257, trackName = "Freedom", artistName = "Akon")
        assertNotNull(match)
        assertEquals(2, match?.id)
    }
}
