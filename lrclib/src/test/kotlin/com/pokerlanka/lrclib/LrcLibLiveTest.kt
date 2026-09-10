package com.pokerlanka.lrclib

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcLibLiveTest {

    @Test
    fun testGetLyricsAkonFreedom() = runBlocking {
        val result = LrcLib.getLyrics(
            title = "Akon - Freedom (Official Video)",
            artist = "Akon",
            duration = 254,
            album = "Freedom (Int'l Version 2)",
        )
        assertTrue("Expected lyrics fetch to succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val lyrics = result.getOrNull()
        assertNotNull(lyrics)
        assertTrue("Lyrics should contain 'wanna be free'", lyrics!!.contains("wanna be free", ignoreCase = true))
    }
}
