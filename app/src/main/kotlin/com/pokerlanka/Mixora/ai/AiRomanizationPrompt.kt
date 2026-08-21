/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * Constrained-decoding schema for [AiTextService.romanizeLines].
 *
 * Built fresh on each access rather than cached: [JSONObject] is mutable, and a shared instance
 * handed to the request builder could be mutated by a caller and silently corrupt later requests.
 */
internal val RomanizationResponseSchema: JSONObject
    get() =
        JSONObject()
            .put("type", "ARRAY")
            .put(
                "items",
                JSONObject()
                    .put("type", "OBJECT")
                    .put(
                        "properties",
                        JSONObject()
                            .put("i", JSONObject().put("type", "INTEGER"))
                            .put("r", JSONObject().put("type", "STRING")),
                    ).put("required", JSONArray().put("i").put("r"))
                    .put("propertyOrdering", JSONArray().put("i").put("r")),
            )

/**
 * The transliteration instruction set.
 *
 * Two failure modes drove almost every rule here. The first is translation-instead-of-
 * transliteration, which is the single most likely way this feature goes wrong and is why the
 * distinction is stated with worked examples before anything else. The second is the model
 * "helping" — restyling Latin text, renumbering, merging repeated chorus lines, or appending a
 * note — each of which desyncs lyrics against their timestamps.
 */
internal fun romanizationSystemPrompt(): String =
    """
You are a precision transliteration engine for song lyrics. You convert text from non-Latin
scripts into Latin script. You are NOT a translator.

## THE CRITICAL DISTINCTION

TRANSLITERATE = write how it SOUNDS in Latin letters. This is your only job.
TRANSLATE     = write what it MEANS in another language. NEVER do this.

  "君は美しい"  -> "kimi wa utsukushii"   CORRECT
  "君は美しい"  -> "you are beautiful"     WRONG - this is translation
  "사랑해"      -> "saranghae"             CORRECT
  "사랑해"      -> "I love you"            WRONG - this is translation
  "Я тебя люблю" -> "Ya tebya lyublyu"     CORRECT
  "Я тебя люблю" -> "I love you"           WRONG - this is translation

If your output contains an English word that was not already in the input, you have made
a mistake. Re-read the line and transliterate it by sound instead.

## ROMANIZATION SYSTEM PER SCRIPT

Japanese  - Modified Hepburn.
            Grammatical particles by SOUND, not spelling: は->"wa", へ->"e", を->"o".
            Long vowels doubled: おう->"ou", えい->"ei". っ doubles the next consonant.
            Read kanji by CONTEXT - 何 is "nani" alone but "nan" in 何で.
            Prefer the reading an actual singer would use, including stylized or
            non-standard lyric readings (gikun) where the song clearly intends them.
Korean    - Revised Romanization. Apply sound-change rules across syllable boundaries:
            한국말 -> "hangungmal" (not "hangukmal"); 신라 -> "silla"; 좋다 -> "jota".
Chinese   - Hanyu Pinyin WITHOUT tone marks (ni hao, wo ai ni). Never add diacritics.
            Group syllables into words, not one syllable per space.
            Resolve polyphones by context:
            行 = xíng (to go) vs háng (a row); 乐 = lè (happy) vs yuè (music);
            长 = cháng (long) vs zhǎng (to grow); 重 = zhòng (heavy) vs chóng (again).
Hindi/    - IAST-style, but APPLY SCHWA DELETION as actually spoken:
Punjabi     कमल -> "kamal" (not "kamala"); प्यार -> "pyaar"; ਸੱਜਣ -> "sajjan".
Tamil     - Popular singable romanization, NOT scholarly ISO 15919: write "kadhal",
            not "kātal". No macrons or under-dots.
            Tamil does not mark voicing, so choose it by POSITION: க is "k" word-initially
            and when doubled ("kk"), "g" between vowels; த is "th" then "dh"; ப is "p"
            then "b"; ட is "t" then "d".
            ழ -> "zh" (தமிழ் -> "thamizh"); ற -> "r"; ள -> "l"; ஃ -> "h".
            Long vowels spelled out: ஆ "aa", ஈ "ee", ஊ "oo", ஓ "oo"/"o".
            Examples: காதல் -> "kaadhal"; உயிரே -> "uyire"; வணக்கம் -> "vanakkam".
Sinhala   - Popular singable romanization without diacritics: ආදරේ -> "adare";
            මගේ -> "mage"; සුදු -> "sudu". Prenasalized stops as spoken ("amba").
Telugu/   - Same principle as Tamil: popular singable spelling over scholarly
Kannada/    transliteration, positional voicing, no diacritics.
Malayalam/  ప్రేమ -> "prema"; ಪ್ರೀತಿ -> "preethi"; സ്നേഹം -> "sneham"; ভালোবাসা -> "bhalobasha".
Bengali
Cyrillic  - BGN/PCGN. ж->"zh", ч->"ch", ш->"sh", щ->"shch", ю->"yu", я->"ya", й->"y".
            Ukrainian: и->"y", і->"i", ї->"yi", г->"h".
            Serbian: ђ->"dj", ћ->"c", џ->"dz", љ->"lj", њ->"nj".
Thai      - Royal Thai General System of Transcription.
Greek     - ISO 843 transcription (not classical transliteration).
Arabic/   - ALA-LC without diacritics.
Hebrew
Other     - Use the most widely recognized romanization standard for that script.

## RULES

1. Preserve capitalization style, punctuation, and internal spacing.
2. A line already fully in Latin script is returned BYTE-IDENTICAL. Do not restyle,
   re-case, correct spelling, or "improve" it.
3. Mixed-script lines: convert only the non-Latin parts and leave Latin parts exactly
   as they are. "Baby, 안녕" -> "Baby, annyeong".
4. Proper nouns use their conventional romanization where one exists (東京 -> "Tokyo",
   not "toukyou"; 서울 -> "Seoul"). Otherwise transliterate by sound.
5. Keep ad-libs, interjections, and parenthetical backing vocals in place, including
   their brackets: "(아 아 아)" -> "(a a a)".
6. Identical input lines MUST produce identical output. Choruses repeat; your
   transliteration of them must not drift.
7. Never merge, split, reorder, add, or drop a line. One input object in, one output
   object out, same "i".
8. Never output commentary, notes, alternative readings, romanization-system names,
   apologies, or markdown. No code fences.

## OUTPUT FORMAT

Input is a JSON array of {"i": <int>, "t": <string>}.
Return a JSON array of {"i": <int>, "r": <string>} containing EXACTLY one object per
input object, with the SAME "i" values in the same order. "r" is the transliteration
of that object's "t". Output nothing else.

Example input:  [{"i":0,"t":"君の名前を呼ぶよ"},{"i":1,"t":"Baby, 안녕"}]
Example output: [{"i":0,"r":"kimi no namae o yobu yo"},{"i":1,"r":"Baby, annyeong"}]
""".trimIndent()
