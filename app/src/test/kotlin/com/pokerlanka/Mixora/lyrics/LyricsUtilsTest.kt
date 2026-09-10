package com.pokerlanka.mixora.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsUtilsTest {

    @Test
    fun `cleanTitleAndArtist separates artist and title when formatted as Artist - Title`() {
        val (title, artist) = LyricsUtils.cleanTitleAndArtist("Akon - Freedom (Official Music Video)", "Akon")
        assertEquals("Freedom", title)
        assertEquals("Akon", artist)
    }

    @Test
    fun `cleanTitleAndArtist extracts artist when not provided in metadata`() {
        val (title, artist) = LyricsUtils.cleanTitleAndArtist("Akon - Freedom", null)
        assertEquals("Freedom", title)
        assertEquals("Akon", artist)
    }

    @Test
    fun `cleanTitleAndArtist handles Title - Artist format`() {
        val (title, artist) = LyricsUtils.cleanTitleAndArtist("Freedom - Akon", "Akon")
        assertEquals("Freedom", title)
        assertEquals("Akon", artist)
    }

    @Test
    fun `cleanTitleAndArtist removes video and feature tags`() {
        val (title1, artist1) = LyricsUtils.cleanTitleAndArtist("Shape of You [Official Lyric Video]", "Ed Sheeran")
        assertEquals("Shape of You", title1)
        assertEquals("Ed Sheeran", artist1)

        val (title2, artist2) = LyricsUtils.cleanTitleAndArtist("Beautiful feat. Colby O'Donis", "Akon")
        assertEquals("Beautiful", title2)
        assertEquals("Akon", artist2)
    }

    @Test
    fun `cleanTitleAndArtist cleans artist noise like Topic or VEVO`() {
        val (title, artist) = LyricsUtils.cleanTitleAndArtist("Freedom", "Akon - Topic")
        assertEquals("Freedom", title)
        assertEquals("Akon", artist)

        val (_, artistVevo) = LyricsUtils.cleanTitleAndArtist("Without Me", "EminemVEVO")
        assertEquals("Eminem", artistVevo)
    }

    @Test
    fun `cleanTitleForSearch returns clean title without artist prefix`() {
        assertEquals("Freedom", LyricsUtils.cleanTitleForSearch("Akon - Freedom (Official Video)", "Akon"))
        assertEquals("Freedom", LyricsUtils.cleanTitleForSearch("Akon - Freedom"))
        assertEquals("Freedom", LyricsUtils.cleanTitleForSearch("Freedom (Audio)"))
    }
}
