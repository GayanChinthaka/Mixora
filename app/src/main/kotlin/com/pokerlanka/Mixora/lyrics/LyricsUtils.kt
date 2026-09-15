/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.lyrics

import android.text.format.DateUtils
import java.util.Locale

val LINE_REGEX = "((\\[\\d\\d:\\d\\d\\.\\d{2,3}\\] ?)+)(.*)".toRegex()
val TIME_REGEX = "\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\]".toRegex()

// Regex for rich sync format: [MM:SS.mm]<MM:SS.mm> word <MM:SS.mm> word ...
private val RICH_SYNC_LINE_REGEX = "\\[(\\d{1,2}):(\\d{2})\\.(\\d{2,3})\\](.*)".toRegex()
private val RICH_SYNC_WORD_REGEX = "<(\\d{1,2}):(\\d{2})\\.(\\d{2,3})>([^<]+)".toRegex()

// Regex for Paxsenix v1/v2/bg format
// [00:00.000]v1: <00:00.000>I <00:00.154>promise...
// [bg: <02:18.078>Yeah<02:19.341>]
private val PAXSENIX_AGENT_LINE_REGEX = "\\[(\\d{1,2}):(\\d{2})\\.(\\d{2,3})\\](v\\d+):\\s*(.*)".toRegex()
private val PAXSENIX_BG_LINE_REGEX = "^\\[bg:\\s*(.*)\\]$".toRegex()

// Regex for agent and background markers (existing format)
private val AGENT_REGEX = "\\{agent:([^}]+)\\}".toRegex()
private val BACKGROUND_REGEX = "^\\{bg\\}".toRegex()

@Suppress("RegExpRedundantEscape")
object LyricsUtils {
    private val TITLE_CLEANUP_REGEXES = listOf(
        Regex("""\s*\((?:official|video|audio|lyrics?|lyric\s+video|visualizer|hd|hq|4k|remaster(?:ed)?|remix|live|acoustic|version|edit|extended|radio|clean|explicit|clip).*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[(?:official|video|audio|lyrics?|lyric\s+video|visualizer|hd|hq|4k|remaster(?:ed)?|remix|live|acoustic|version|edit|extended|radio|clean|explicit|clip).*?\]""", RegexOption.IGNORE_CASE),
        Regex("""\s*【.*?】"""),
        Regex("""\s*\|.*$"""),
        Regex("""\s*-\s*(?:official|video|audio|lyrics?|visualizer).*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\((?:feat\.|ft\.|featuring).*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[(?:feat\.|ft\.|featuring).*?\]""", RegexOption.IGNORE_CASE),
        Regex("""\s*(?:feat\.|ft\.|featuring)\s+.*$""", RegexOption.IGNORE_CASE),
    )

    private val ARTIST_NOISE_REGEX = Regex("""(?i)\s*-\s*Topic|\s*VEVO|\s*Official""")

    fun cleanArtist(artist: String?): String {
        if (artist.isNullOrBlank()) return ""
        return artist.replace(ARTIST_NOISE_REGEX, "").trim()
    }

    /**
     * Cleans up song title and artist. If title contains "Artist - Title",
     * it separates them cleanly and resolves the actual song title and primary artist.
     */
    fun cleanTitleAndArtist(rawTitle: String, rawArtist: String? = null): Pair<String, String> {
        var title = rawTitle.trim()
        var artist = cleanArtist(rawArtist)

        // 1. Remove bracketed junk from title
        for (pattern in TITLE_CLEANUP_REGEXES) {
            title = title.replace(pattern, "")
        }
        title = title.trim()

        // 2. Detect "Artist - Title" or "Artist: Title"
        val separators = listOf(" - ", " – ", " — ", ": ")
        for (sep in separators) {
            if (title.contains(sep)) {
                val parts = title.split(sep, limit = 2)
                val part1 = parts[0].trim()
                val part2 = parts[1].trim()

                if (part1.isNotBlank() && part2.isNotBlank()) {
                    val part1Lower = part1.lowercase(Locale.ROOT)
                    val artistLower = artist.lowercase(Locale.ROOT)

                    if (artist.isNotBlank() && (part1Lower == artistLower || artistLower.contains(part1Lower) || part1Lower.contains(artistLower))) {
                        // "Akon - Freedom" where artist is "Akon" -> title is "Freedom"
                        title = part2
                        break
                    } else if (artist.isNotBlank() && (part2.lowercase(Locale.ROOT) == artistLower)) {
                        // "Freedom - Akon" where artist is "Akon" -> title is "Freedom"
                        title = part1
                        break
                    } else if (artist.isBlank()) {
                        // If no artist was given, split into artist and title
                        artist = cleanArtist(part1)
                        title = part2
                        break
                    }
                }
            }
        }

        // 3. Fallback: if title still has leftover brackets e.g. "(...)"
        if (title.contains("(") || title.contains("[")) {
            val stripped = title.replace(Regex("""\s*[(\[].*?[)\]]"""), "").trim()
            if (stripped.isNotBlank()) {
                title = stripped
            }
        }

        return Pair(title.trim(), artist.trim())
    }

    fun hasSinhalaScript(text: String): Boolean =
        text.any { it.code in 0x0D80..0x0DFF }

    private val SINHALA_CONSONANTS = mapOf(
        '\u0D9A' to "k", '\u0D9B' to "kh", '\u0D9C' to "g", '\u0D9D' to "gh", '\u0D9E' to "ng", '\u0D9F' to "ng",
        '\u0DA0' to "ch", '\u0DA1' to "chh", '\u0DA2' to "j", '\u0DA3' to "jh", '\u0DA4' to "ny", '\u0DA5' to "jny", '\u0DA6' to "ndj",
        '\u0DA7' to "t", '\u0DA8' to "th", '\u0DA9' to "d", '\u0DAA' to "dh", '\u0DAB' to "n", '\u0DAC' to "nd",
        '\u0DAD' to "th", '\u0DAE' to "th", '\u0DAF' to "d", '\u0DB0' to "dh", '\u0DB1' to "n", '\u0DB3' to "nd",
        '\u0DB4' to "p", '\u0DB5' to "ph", '\u0DB6' to "b", '\u0DB7' to "bh", '\u0DB8' to "m", '\u0DB9' to "mb",
        '\u0DBA' to "y", '\u0DBB' to "r", '\u0DBD' to "l", '\u0DC0' to "w",
        '\u0DC1' to "sh", '\u0DC2' to "sh", '\u0DC3' to "s", '\u0DC4' to "h", '\u0DC5' to "l", '\u0DC6' to "f",
    )

    private val SINHALA_VOWEL_SIGNS = mapOf(
        '\u0DCF' to "a", '\u0DD0' to "a", '\u0DD1' to "aa", '\u0DD2' to "i", '\u0DD3' to "ee",
        '\u0DD4' to "u", '\u0DD6' to "oo", '\u0DD8' to "ru", '\u0DD9' to "e", '\u0DDA' to "e",
        '\u0DDB' to "ai", '\u0DDC' to "o", '\u0DDD' to "o", '\u0DDE' to "au", '\u0DCA' to "",
    )

    private val SINHALA_INDEP_VOWELS = mapOf(
        '\u0D85' to "a", '\u0D86' to "aa", '\u0D87' to "ae", '\u0D88' to "aae", '\u0D89' to "i",
        '\u0D8A' to "ee", '\u0D8B' to "u", '\u0D8C' to "oo", '\u0D8F' to "e", '\u0D90' to "e",
        '\u0D91' to "e", '\u0D92' to "e", '\u0D93' to "ai", '\u0D94' to "o", '\u0D95' to "o", '\u0D96' to "au",
    )

    fun transliterateSinhalaToLatin(input: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < input.length) {
            val c = input[i]
            val indep = SINHALA_INDEP_VOWELS[c]
            if (indep != null) {
                out.append(indep)
                i++
            } else {
                val cons = SINHALA_CONSONANTS[c]
                if (cons != null) {
                    i++
                    if (i < input.length && SINHALA_VOWEL_SIGNS.containsKey(input[i])) {
                        val vs = SINHALA_VOWEL_SIGNS[input[i]].orEmpty()
                        out.append(cons).append(vs)
                        i++
                    } else {
                        out.append(cons).append("a")
                    }
                } else {
                    out.append(c)
                    i++
                }
            }
        }
        return out.toString()
    }

    /**
     * Extracts candidate search titles from a potentially bilingual or bracketed title.
     * E.g. "සරාගයේ (Saragaye)" -> ["සරාගයේ", "Saragaye"]
     */
    fun extractSearchCandidateTitles(rawTitle: String, rawArtist: String? = null): List<String> {
        val candidates = mutableListOf<String>()
        val (cleanedTitle, _) = cleanTitleAndArtist(rawTitle, rawArtist)
        if (cleanedTitle.isNotBlank()) {
            candidates.add(cleanedTitle)
            if (hasSinhalaScript(cleanedTitle)) {
                val transliterated = transliterateSinhalaToLatin(cleanedTitle).trim()
                if (transliterated.isNotBlank()) {
                    candidates.add(transliterated)
                }
            }
        }

        val bracketMatch = Regex("""\(([^)]+)\)""").find(rawTitle)
        if (bracketMatch != null) {
            val inside = bracketMatch.groupValues[1].trim()
            val outside = rawTitle.replace(bracketMatch.value, "").trim()
            val (cleanedInside, _) = cleanTitleAndArtist(inside, rawArtist)
            val (cleanedOutside, _) = cleanTitleAndArtist(outside, rawArtist)
            if (cleanedInside.isNotBlank() && cleanedInside.length > 2 && !candidates.contains(cleanedInside)) {
                candidates.add(cleanedInside)
                if (hasSinhalaScript(cleanedInside)) {
                    val transliterated = transliterateSinhalaToLatin(cleanedInside).trim()
                    if (transliterated.isNotBlank() && !candidates.contains(transliterated)) {
                        candidates.add(transliterated)
                    }
                }
            }
            if (cleanedOutside.isNotBlank() && cleanedOutside.length > 2 && !candidates.contains(cleanedOutside)) {
                candidates.add(cleanedOutside)
                if (hasSinhalaScript(cleanedOutside)) {
                    val transliterated = transliterateSinhalaToLatin(cleanedOutside).trim()
                    if (transliterated.isNotBlank() && !candidates.contains(transliterated)) {
                        candidates.add(transliterated)
                    }
                }
            }
        }
        return candidates.distinct()
    }

    fun cleanTitleForSearch(title: String, artist: String? = null): String {
        return cleanTitleAndArtist(title, artist).first
    }

    fun filterLyricsCreditLines(lyrics: String): String {
        return lyrics.lines().filter { line ->
            // Strip leading bracketed/braced content, version tags, and timestamps
            // Handles [00:00.00], {agent:v1}, {bg}, [bg: ...], v1: etc.
            var textContent = line.trim()
            
            // Repeatedly strip prefixes while they match common patterns
            var stripping = true
            while (stripping) {
                val prevLength = textContent.length
                textContent = textContent
                    .replaceFirst(Regex("^\\[\\d\\d:\\d\\d\\.\\d{2,3}\\]"), "")
                    .replaceFirst(Regex("^\\{agent:[^}]+\\}"), "")
                    .replaceFirst(Regex("^\\{bg\\}"), "")
                    .replaceFirst(Regex("^\\[bg:.*\\]"), "")
                    .replaceFirst(Regex("^v\\d+:"), "")
                    .trim()
                stripping = textContent.length < prevLength
            }

            val lowerText = textContent.lowercase(Locale.getDefault())
            
            val isCredit = lowerText.startsWith("synced by") ||
                    lowerText.startsWith("lyrics by") ||
                    lowerText.startsWith("music by") ||
                    lowerText.startsWith("arranged by") ||
                    (lowerText.startsWith("[") && lowerText.endsWith("]") && lowerText.length < 40 && lowerText.contains("synced by"))
            
            !isCredit
        }.joinToString("\n")
    }

    private val HEX_ENTITY_REGEX = "&#x([0-9a-fA-F]+);".toRegex()
    private val DEC_ENTITY_REGEX = "&#(\\d+);".toRegex()

    private fun decodeHtmlEntities(text: String): String {
        if (!text.contains('&')) return text
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '&') {
                val end = text.indexOf(';', i + 1)
                if (end != -1 && end - i < 12) {
                    val entity = text.substring(i, end + 1)
                    val decoded = when {
                        entity == "&apos;" -> "'"
                        entity == "&quot;" -> "\""
                        entity == "&lt;" -> "<"
                        entity == "&gt;" -> ">"
                        entity == "&nbsp;" -> " "
                        entity == "&amp;" -> "&"
                        entity.startsWith("&#x") -> {
                            entity.substring(3, entity.length - 1).toIntOrNull(16)?.let { codePoint ->
                                if (Character.isValidCodePoint(codePoint)) String(Character.toChars(codePoint)) else "\uFFFD"
                            }
                        }
                        entity.startsWith("&#") -> {
                            entity.substring(2, entity.length - 1).toIntOrNull()?.let { codePoint ->
                                if (Character.isValidCodePoint(codePoint)) String(Character.toChars(codePoint)) else "\uFFFD"
                            }
                        }
                        else -> null
                    }
                    if (decoded != null) {
                        sb.append(decoded)
                        i = end + 1
                        continue
                    }
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    fun parseLyrics(lyrics: String): List<LyricsEntry> {
        if (lyrics.isBlank()) return emptyList()

        // Fast unescape
        val unescapedLyrics = if (lyrics.contains('\\') || lyrics.startsWith("\"")) {
            val s = lyrics.trim().removePrefix("\"").removeSuffix("\"")
            val sb = StringBuilder(s.length)
            var j = 0
            while (j < s.length) {
                val c = s[j]
                if (c == '\\' && j + 1 < s.length) {
                    when (val next = s[j + 1]) {
                        '\\' -> sb.append('\\')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        else -> sb.append(c).append(next)
                    }
                    j += 2
                } else {
                    sb.append(c)
                    j++
                }
            }
            sb.toString()
        } else lyrics

        val decodedLyrics = decodeHtmlEntities(unescapedLyrics)

        val lines = decodedLyrics.lines()
            .filter { 
                it.isNotBlank() || it.trim().startsWith("[") || it.trim().startsWith("<")
            }
            .filter { !it.trim().startsWith("[offset:") }

        // Check if this is rich sync format (contains <MM:SS.mm> patterns)
        val isRichSync = lines.any { line ->
            RICH_SYNC_LINE_REGEX.matches(line.trim()) &&
            RICH_SYNC_WORD_REGEX.containsMatchIn(line)
        }

        return if (isRichSync) {
            parseRichSyncLyrics(lines)
        } else {
            parseStandardLyrics(lines)
        }
    }

    /**
     * Parse rich sync lyrics format: [MM:SS.mm]<MM:SS.mm> word <MM:SS.mm> word ...
     * This format provides word-by-word timing for karaoke-style highlighting
     */
    private fun parseRichSyncLyrics(lines: List<String>): List<LyricsEntry> {
        val result = mutableListOf<LyricsEntry>()
        var lastNonBgAgent: String? = null

        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            
            // Try Paxsenix bg format first: [bg: <02:18.078>Yeah<02:19.341>]
            val bgMatch = PAXSENIX_BG_LINE_REGEX.find(trimmedLine)
            if (bgMatch != null) {
                val content = bgMatch.groupValues[1]
                
                // Parse word-level timestamps from content
                val wordTimings = parseRichSyncWords(content, index, lines)
                    ?: run {
                        val nextLine = lines.getOrNull(index + 1)?.trim() ?: ""
                        if (nextLine.startsWith("<") && nextLine.endsWith(">")) {
                            parseWordTimestamps(nextLine.removeSurrounding("<", ">"))
                        } else null
                    }
                
                // Extract plain text (remove all <MM:SS.mm> tags)
                val plainText = content.replace(Regex("<\\d{1,2}:\\d{2}\\.\\d{2,3}>\\s*"), "").trim()
                
                val lineTimeMs = wordTimings?.firstOrNull()?.startTime?.let { (it * 1000).toLong() } ?: 0L
                result.add(LyricsEntry(lineTimeMs, plainText, wordTimings, agent = lastNonBgAgent ?: "bg", isBackground = true))
                return@forEachIndexed
            }
            
            // Try Paxsenix agent format: [00:00.000]v1: <00:00.000>I <00:00.154>promise...
            val agentMatch = PAXSENIX_AGENT_LINE_REGEX.find(trimmedLine)
            if (agentMatch != null) {
                val minutes = agentMatch.groupValues[1].toLongOrNull() ?: 0L
                val seconds = agentMatch.groupValues[2].toLongOrNull() ?: 0L
                val centiseconds = agentMatch.groupValues[3].toLongOrNull() ?: 0L
                val agent = agentMatch.groupValues[4] // v1, v2, etc.
                val content = agentMatch.groupValues[5]
                
                val millisPart = if (agentMatch.groupValues[3].length == 3) centiseconds else centiseconds * 10
                val lineTimeMs = minutes * DateUtils.MINUTE_IN_MILLIS + seconds * DateUtils.SECOND_IN_MILLIS + millisPart
                
                // Parse word-level timestamps from content
                val wordTimings = parseRichSyncWords(content, index, lines)
                    ?: run {
                        val nextLine = lines.getOrNull(index + 1)?.trim() ?: ""
                        if (nextLine.startsWith("<") && nextLine.endsWith(">")) {
                            parseWordTimestamps(nextLine.removeSurrounding("<", ">"))
                        } else null
                    }
                
                // Extract plain text (remove all <MM:SS.mm> tags)
                val plainText = content.replace(Regex("<\\d{1,2}:\\d{2}\\.\\d{2,3}>\\s*"), "").trim()
                
                if (!agent.isNullOrBlank()) {
                    lastNonBgAgent = agent
                }
                result.add(LyricsEntry(lineTimeMs, plainText, wordTimings, agent = agent, isBackground = false))
                return@forEachIndexed
            }
            
            // Try existing format: [MM:SS.mm]{agent:v1}... or [MM:SS.mm]{bg}...
            val matchResult = RICH_SYNC_LINE_REGEX.matchEntire(trimmedLine)
            if (matchResult != null) {
                val minutes = matchResult.groupValues[1].toLongOrNull() ?: 0L
                val seconds = matchResult.groupValues[2].toLongOrNull() ?: 0L
                val centiseconds = matchResult.groupValues[3].toLongOrNull() ?: 0L

                // Convert to milliseconds
                val millisPart = if (matchResult.groupValues[3].length == 3) centiseconds else centiseconds * 10
                val lineTimeMs = minutes * DateUtils.MINUTE_IN_MILLIS + seconds * DateUtils.SECOND_IN_MILLIS + millisPart

                var content = matchResult.groupValues[4].trimStart()

                // Parse agent marker {agent:v1}
                val oldAgentMatch = AGENT_REGEX.find(content)
                val agent = oldAgentMatch?.groupValues?.get(1)
                if (oldAgentMatch != null) {
                    content = content.replaceFirst(AGENT_REGEX, "")
                }

                // Parse background marker {bg}
                val isBackground = BACKGROUND_REGEX.containsMatchIn(content)
                if (isBackground) {
                    content = content.replaceFirst(BACKGROUND_REGEX, "")
                }

                // Parse word-level timestamps from content
                val wordTimings = parseRichSyncWords(content, index, lines)
                    ?: run {
                        val nextLine = lines.getOrNull(index + 1)?.trim() ?: ""
                        if (nextLine.startsWith("<") && nextLine.endsWith(">")) {
                            parseWordTimestamps(nextLine.removeSurrounding("<", ">"))
                        } else null
                    }

                // Extract plain text (remove all <MM:SS.mm> tags)
                val plainText = content.replace(Regex("<\\d{1,2}:\\d{2}\\.\\d{2,3}>\\s*"), "").trim()

                if (!isBackground && !agent.isNullOrBlank()) {
                    lastNonBgAgent = agent
                }
                result.add(LyricsEntry(lineTimeMs, plainText, wordTimings, agent = if (isBackground) lastNonBgAgent ?: "bg" else agent, isBackground = isBackground))
            }
        }

        return result.sorted()
    }

    /**
     * Parse word timestamps from rich sync content
     * Format: <MM:SS.mm> word <MM:SS.mm> word ...
     */
    private fun parseRichSyncWords(content: String, currentIndex: Int, allLines: List<String>): List<WordTimestamp>? {
        val wordMatches = RICH_SYNC_WORD_REGEX.findAll(content).toList()

        if (wordMatches.isEmpty()) return null

        // Check for a trailing end timestamp after the last word.
        // The provider uses two formats:
        //   - Angle brackets: <MM:SS.mmm> (used in v1:/v2: prefixed lines)
        //   - Square brackets: [MM:SS.xx] (used in non-prefixed lines)
        val lastMatchEnd = wordMatches.last().range.last
        val trailingContent = content.substring(lastMatchEnd + 1).trim()
        val angleTrailingMatch = "<(\\d{1,2}):(\\d{2})\\.(\\d{2,3})>".toRegex().find(trailingContent)
        val squareTrailingMatch = "\\[(\\d{1,2}):(\\d{2})\\.(\\d{2,3})\\]".toRegex().find(trailingContent)
        val trailingTimeMatch = angleTrailingMatch ?: squareTrailingMatch
        val trailingEndTime: Double? = if (trailingTimeMatch != null && trailingContent.substring(trailingTimeMatch.range.last + 1).removeSuffix("]").isBlank()) {
            val tMin = trailingTimeMatch.groupValues[1].toLongOrNull() ?: 0L
            val tSec = trailingTimeMatch.groupValues[2].toLongOrNull() ?: 0L
            val tFrac = trailingTimeMatch.groupValues[3].toLongOrNull() ?: 0L
            val tFracPart = if (trailingTimeMatch.groupValues[3].length == 3) tFrac / 1000.0 else tFrac / 100.0
            tMin * 60.0 + tSec + tFracPart
        } else null

        val wordTimings = mutableListOf<WordTimestamp>()

        wordMatches.forEachIndexed { index, match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: 0L
            val seconds = match.groupValues[2].toLongOrNull() ?: 0L
            val fraction = match.groupValues[3].toLongOrNull() ?: 0L

            val fractionPart = if (match.groupValues[3].length == 3) fraction / 1000.0 else fraction / 100.0
            val startTimeSeconds = minutes * 60.0 + seconds + fractionPart

            val rawText = match.groupValues[4]
            val hasTrailingSpace = rawText.endsWith(" ")
            val words = rawText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

            // Get the next timestamp for end time calculation
            val nextTimestamp: Double
            val nextLineTime: Double?

            if (index < wordMatches.size - 1) {
                val nextMatch = wordMatches[index + 1]
                val nextMin = nextMatch.groupValues[1].toLongOrNull() ?: 0L
                val nextSec = nextMatch.groupValues[2].toLongOrNull() ?: 0L
                val nextFrac = nextMatch.groupValues[3].toLongOrNull() ?: 0L
                val nextFracPart = if (nextMatch.groupValues[3].length == 3) nextFrac / 1000.0 else nextFrac / 100.0
                nextTimestamp = nextMin * 60.0 + nextSec + nextFracPart
                nextLineTime = null
            } else {
                nextLineTime = getNextLineStartTime(currentIndex, allLines)
                nextTimestamp = trailingEndTime ?: nextLineTime ?: (startTimeSeconds + 0.5)
            }

            words.forEachIndexed { wordIndex, word ->
                val isLastWordInGroup = wordIndex == words.lastIndex
                val isLastWordOverall = index == wordMatches.lastIndex && isLastWordInGroup

                val wordStartTime = startTimeSeconds + (nextTimestamp - startTimeSeconds) * wordIndex / words.size
                val wordEndTime = if (!isLastWordInGroup) {
                    startTimeSeconds + (nextTimestamp - startTimeSeconds) * (wordIndex + 1) / words.size
                } else if (!isLastWordOverall) {
                    nextTimestamp
                } else {
                    trailingEndTime ?: nextLineTime ?: (startTimeSeconds + 0.5)
                }

                val wordHasTrailingSpace = if (!isLastWordInGroup) {
                    true
                } else if (!isLastWordOverall) {
                    hasTrailingSpace
                } else {
                    // Last word of last match - check if there's text after it (excluding our optional trailing timestamp)
                    val textAfterMatch = if (trailingTimeMatch != null) {
                        trailingContent.substring(0, trailingTimeMatch.range.first)
                    } else {
                        trailingContent
                    }
                    textAfterMatch.isNotBlank()
                }

                if (word.isNotBlank()) {
                    wordTimings.add(WordTimestamp(word, wordStartTime, wordEndTime, wordHasTrailingSpace))
                }
            }
        }

        return if (wordTimings.isNotEmpty()) wordTimings else null
    }

    /**
     * Get the start time of the next line for calculating the last word's end time
     */
    private fun getNextLineStartTime(currentIndex: Int, allLines: List<String>): Double? {
        if (currentIndex + 1 >= allLines.size) return null

        val nextLine = allLines[currentIndex + 1].trim()
        
        // Try standard rich sync line
        val matchResult = RICH_SYNC_LINE_REGEX.matchEntire(nextLine)
        if (matchResult != null) {
            val minutes = matchResult.groupValues[1].toLongOrNull() ?: return null
            val seconds = matchResult.groupValues[2].toLongOrNull() ?: return null
            val fraction = matchResult.groupValues[3].toLongOrNull() ?: 0L

            val fractionPart = if (matchResult.groupValues[3].length == 3) fraction / 1000.0 else fraction / 100.0
            return minutes * 60.0 + seconds + fractionPart
        }
        
        // Try background line
        val bgMatch = PAXSENIX_BG_LINE_REGEX.matchEntire(nextLine)
        if (bgMatch != null) {
            val content = bgMatch.groupValues[1]
            val wordMatch = RICH_SYNC_WORD_REGEX.find(content) ?: return null
            val minutes = wordMatch.groupValues[1].toLongOrNull() ?: return null
            val seconds = wordMatch.groupValues[2].toLongOrNull() ?: return null
            val fraction = wordMatch.groupValues[3].toLongOrNull() ?: 0L
            val fractionPart = if (wordMatch.groupValues[3].length == 3) fraction / 1000.0 else fraction / 100.0
            return minutes * 60.0 + seconds + fractionPart
        }

        return null
    }

    /**
     * Parse standard synced lyrics format: [MM:SS.mm] text
     */
    private fun parseStandardLyrics(lines: List<String>): List<LyricsEntry> {
        val result = mutableListOf<LyricsEntry>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (!line.trim().startsWith("<") || !line.trim().endsWith(">")) {
                val entries = parseLine(line, null)
                if (entries != null) {
                    val wordTimestamps = if (i + 1 < lines.size) {
                        val nextLine = lines[i + 1]
                        if (nextLine.trim().startsWith("<") && nextLine.trim().endsWith(">")) {
                            parseWordTimestamps(nextLine.trim().removeSurrounding("<", ">"))
                        } else null
                    } else null

                    if (wordTimestamps != null) {
                        result.addAll(entries.map { entry ->
                            LyricsEntry(entry.time, entry.text, wordTimestamps, agent = entry.agent, isBackground = entry.isBackground)
                        })
                    } else {
                        result.addAll(entries)
                    }
                }
            }
            i++
        }
        return result.sorted()
    }

    private fun parseWordTimestamps(data: String): List<WordTimestamp>? {
        if (data.isBlank()) return null
        return try {
            data.split("|").mapNotNull { wordData ->
                val parts = wordData.split(":")
                if (parts.size >= 3) {
                    val text = parts.dropLast(2).joinToString(":")
                    val startTime = parts[parts.size - 2].toDoubleOrNull() ?: 0.0
                    val endTime = parts[parts.size - 1].toDoubleOrNull() ?: 0.0
                    val isLast = wordData == data.split("|").last()
                    WordTimestamp(
                        text = text,
                        startTime = startTime,
                        endTime = endTime,
                        hasTrailingSpace = !isLast
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseLine(line: String, words: List<WordTimestamp>? = null): List<LyricsEntry>? {
        val matchResult = LINE_REGEX.matchEntire(line.trim()) ?: return null
        val times = matchResult.groupValues[1]
        var text = matchResult.groupValues[3]
        val timeMatchResults = TIME_REGEX.findAll(times)

        // Parse agent marker {agent:v1}
        val agentMatch = AGENT_REGEX.find(text)
        val agent = agentMatch?.groupValues?.get(1)
        if (agentMatch != null) {
            text = text.replaceFirst(AGENT_REGEX, "")
        }

        // Parse background marker {bg}
        val isBackground = BACKGROUND_REGEX.containsMatchIn(text)
        if (isBackground) {
            text = text.replaceFirst(BACKGROUND_REGEX, "")
        }

        return timeMatchResults
            .map { timeMatchResult ->
                val min = timeMatchResult.groupValues[1].toLong()
                val sec = timeMatchResult.groupValues[2].toLong()
                val milString = timeMatchResult.groupValues[3]
                var mil = milString.toLong()
                if (milString.length == 2) {
                    mil *= 10
                }
                val time = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil
                LyricsEntry(time, text, words, agent = agent, isBackground = isBackground)
            }.toList()
    }

    fun findCurrentLineIndex(
        lines: List<LyricsEntry>,
        position: Long,
    ): Int {
        val threshold = 100L
        for (index in lines.indices) {
            if (lines[index].time >= position + threshold) {
                return index - 1
            }
        }
        return lines.lastIndex
    }

    /**
     * Returns the set of line indices that are currently active (being sung).
     * A line is active if playback position >= line.time AND position < line end time.
     * Line end time = the last word's endTime if word timings exist, otherwise the next line's start time.
     * This supports simultaneous singers whose lines overlap in time.
     */
    fun findActiveLineIndices(
        lines: List<LyricsEntry>,
        position: Long,
    ): Set<Int> {
        val active = mutableSetOf<Int>()
        val hasWordTimings = lines.any { !it.words.isNullOrEmpty() }

        for (index in lines.indices) {
            val line = lines[index]
            if (line.time > position) break // Past current position, stop early

            // Determine this line's end time
            val lineEndMs: Long = if (!line.words.isNullOrEmpty()) {
                // Use last word's endTime converted to ms
                (line.words.last().endTime * 1000).toLong()
            } else {
                // Fallback: next line's start time
                if (index + 1 < lines.size) lines[index + 1].time else Long.MAX_VALUE
            }

            if (position <= lineEndMs) {
                active.add(index)
            }
        }

        if (!hasWordTimings && active.size > 1) {
            val mainActive = active.filter { lines[it].isBackground == false }
            if (mainActive.size > 1) {
                val maxTime = mainActive.maxOf { lines[it].time }
                active.removeAll { it in mainActive && lines[it].time < maxTime }
            }
        }

        return active
    }

    // TODO: Will be useful if we let the user pick the language, useless for now
    /* enum class CyrillicLanguage {
        RUSSIAN,
        UKRAINIAN,
        SERBIAN,
        BULGARIAN,
        BELARUSIAN,
        KYRGYZ,
        MACEDONIAN
    } */

    fun isWordSynced(lyrics: String): Boolean {
        return (lyrics.contains("<") && lyrics.contains(">") && (lyrics.contains("|") || lyrics.contains(":"))) ||
                lyrics.contains(RICH_SYNC_WORD_REGEX)
    }

    fun isLineSynced(lyrics: String): Boolean {
        return lyrics.contains(TIME_REGEX) ||
                lyrics.contains(PAXSENIX_AGENT_LINE_REGEX) ||
                lyrics.contains(PAXSENIX_BG_LINE_REGEX)
    }

    fun getLyricsQuality(lyrics: String): Int {
        if (lyrics.isBlank() || lyrics == "Lyrics not found") return 0
        if (isWordSynced(lyrics)) return 3
        if (isLineSynced(lyrics)) return 2
        return 1
    }
}
