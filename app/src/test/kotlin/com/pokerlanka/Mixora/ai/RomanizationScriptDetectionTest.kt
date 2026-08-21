/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [needsRomanization] is the whole on-device script check now that per-language detection is gone.
 * A false negative silently drops romanization for a script; a false positive spends tokens on
 * text that is already Latin. Both are quiet failures, so they are pinned here.
 */
class RomanizationScriptDetectionTest {
    @Test
    fun `detects scripts that need romanizing`() {
        val needsIt =
            listOf(
                "君の名前を呼ぶよ" to "Japanese kanji/kana",
                "サヨナラ" to "Katakana",
                "안녕하세요" to "Hangul",
                "我爱你" to "Han",
                "Я тебя люблю" to "Cyrillic",
                "कमल" to "Devanagari",
                "ਸੱਜਣ" to "Gurmukhi",
                "காதல்" to "Tamil",
                "ආදරේ" to "Sinhala",
                "ప్రేమ" to "Telugu",
                "ಪ್ರೀತಿ" to "Kannada",
                "സ്നേഹം" to "Malayalam",
                "ভালোবাসা" to "Bengali",
                "σ' αγαπώ" to "Greek",
                "สวัสดี" to "Thai",
                "أنا أحبك" to "Arabic",
                "Baby, 안녕" to "mixed Latin and Hangul",
            )
        needsIt.forEach { (line, label) ->
            assertTrue("expected $label to need romanization: $line", needsRomanization(line))
        }
    }

    @Test
    fun `leaves latin text alone`() {
        val skips =
            listOf(
                "" to "empty",
                "   " to "whitespace",
                "Baby you're the one" to "plain ASCII",
                "(ooh ooh ooh)" to "ad-libs",
                "Ne me quitte pas" to "French",
                "Naïve café résumé" to "precomposed Latin diacritics",
                // Decomposed "e" + U+0301: the combining mark sits above the script cutoff
                // but is still Latin text, so it must not trigger a request. Written with an
                // explicit escape because this source file is stored precomposed.
                "caf\u0065\u0301 r\u0065\u0301sum\u0065\u0301" to "decomposed Latin diacritics",
                "1234 !?.,-()" to "digits and punctuation",
            )
        skips.forEach { (line, label) ->
            assertFalse("expected $label to be skipped: $line", needsRomanization(line))
        }
    }
}
