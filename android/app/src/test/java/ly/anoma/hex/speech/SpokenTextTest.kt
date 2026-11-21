package ly.anoma.hex.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 1: lock the EN-first loop — locale resolution, translation gating,
 * and formatter edge cases the device run in Phase 4 will exercise. */
class SpokenTextTest {
    private fun english(style: Style, clean: Boolean = false, translate: Boolean = false) =
        SpeechOptions(style, Spoken.English, clean, translate)

    @Test fun localeMatchingCoversBokmalAndNynorsk() {
        assertEquals(Spoken.Norwegian, Spoken.matching("no"))
        assertEquals(Spoken.Norwegian, Spoken.matching("nb"))
        assertEquals(Spoken.Norwegian, Spoken.matching("nn"))
        assertEquals(Spoken.English, Spoken.matching("en"))
        assertEquals(Spoken.Spanish, Spoken.matching("es"))
        assertEquals(Spoken.German, Spoken.matching("de"))
        assertEquals(Spoken.Hindi, Spoken.matching("hi"))
        assertEquals(Spoken.Marathi, Spoken.matching("mr"))
        assertNull(Spoken.matching("fr"))
        assertNull(Spoken.matching(null))
    }

    @Test fun keyboardLayoutUsesBokmalCode() {
        assertEquals("nb", Spoken.Norwegian.layoutCode)
        assertEquals("en", Spoken.English.layoutCode)
        assertEquals("es", Spoken.Spanish.layoutCode)
        assertEquals("de", Spoken.German.layoutCode)
        assertEquals("hi", Spoken.Hindi.layoutCode)
        assertEquals("mr", Spoken.Marathi.layoutCode)
    }

    @Test fun translationOnlyAppliesToSpanish() {
        assertTrue(translateFor(Spoken.Spanish, true, true))
        assertFalse(translateFor(Spoken.Spanish, true, false))
        assertTrue(translateFor(Spoken.Spanish, true, null))
        assertFalse(translateFor(Spoken.Spanish, false, true))
        assertFalse(translateFor(Spoken.English, true, null))
        assertFalse(translateFor(Spoken.German, true, null))
        assertFalse(translateFor(Spoken.Norwegian, true, null))
    }

    @Test fun blankInputStaysBlankInEveryStyle() {
        for (style in Style.entries) assertEquals("", SpokenText.format("   ", english(style)))
    }

    @Test fun hesitationsAloneLeaveNothingToInsert() {
        assertEquals("", SpokenText.format("um uh", english(Style.Polished)))
        assertEquals("", SpokenText.format("um uh", english(Style.Casual)))
        assertEquals("", SpokenText.format("um uh", english(Style.Notes)))
    }

    @Test fun verbatimKeepsHesitationsAsHeard() {
        assertEquals("um uh", SpokenText.format("um uh", english(Style.Verbatim)))
        assertEquals("hello world", SpokenText.format("  hello world  ", english(Style.Verbatim)))
    }

    @Test fun polishedEndsSentencesWithoutDoublingPunctuation() {
        assertEquals("Hello.", SpokenText.format("hello", english(Style.Polished)))
        assertEquals("Really?", SpokenText.format("really?", english(Style.Polished)))
        assertEquals("Sounds good, see you at 5.", SpokenText.format("Sounds good, see you at 5.",
            english(Style.Polished)))
    }

    @Test fun casualStripsOnlyASingleFinalStop() {
        assertEquals("hello world", SpokenText.format("Hello World.", english(Style.Casual)))
        assertEquals("first. second.", SpokenText.format("First. Second.", english(Style.Casual)))
    }

    @Test fun notesWithoutMarkersIsOneCapitalisedLine() {
        assertEquals("Call sam", SpokenText.format("call sam", english(Style.Notes)))
    }

    @Test fun spanishTranslationFormatsWithEnglishRules() {
        val options = SpeechOptions(Style.Polished, Spoken.Spanish, false, true)
        assertEquals("Hola mundo.", SpokenText.format("hola mundo", options))
    }

    @Test fun hindiAndMarathiFormatWithoutCrashing() {
        // Dictation-only: no cleanup rules, so repairs stay as spoken; only
        // unambiguous hesitations go, and Devanagari has no case to fix.
        assertEquals("नमस्ते दुनिया.", SpokenText.format("नमस्ते दुनिया",
            SpeechOptions(Style.Polished, Spoken.Hindi, false, false)))
        assertEquals("नमस्ते दुनिया.", SpokenText.format("हम्म नमस्ते दुनिया",
            SpeechOptions(Style.Polished, Spoken.Hindi, false, false)))
        assertEquals("Tumhi kase aahat?", SpokenText.format("tumhi kase aahat?",
            SpeechOptions(Style.Polished, Spoken.Marathi, false, false)))
    }

    @Test fun hindiAndMarathiCleanupFallsBackToAsHeard() {
        assertEquals("मतलब कल मिलते हैं", SpokenCleanup.clean("मतलब कल मिलते हैं", "hi"))
        assertEquals("म्हणजे उद्या भेटू", SpokenCleanup.clean("म्हणजे उद्या भेटू", "mr"))
    }
}
