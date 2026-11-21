package ly.anoma.hex.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Test

/** Phase 2: pin which editors get full assistance. A password misclassified as
 * ordinary would enable context reads and dictation where they must not run. */
class EditorKindTest {
    @Test fun plainTextGetsFullAssistance() {
        assertEquals(EditorKind(false, true),
            classifyEditor(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL, 0))
    }

    @Test fun allPasswordVariationsAreTypingOnly() {
        val passwords = listOf(InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
        for (variation in passwords) assertEquals(EditorKind(true, false),
            classifyEditor(InputType.TYPE_CLASS_TEXT or variation, 0))
        assertEquals(EditorKind(true, false), classifyEditor(InputType.TYPE_CLASS_NUMBER or
            InputType.TYPE_NUMBER_VARIATION_PASSWORD, 0))
    }

    @Test fun secureNeverImpliesOrdinaryEvenWithAssistanceFlags() {
        assertEquals(EditorKind(true, false), classifyEditor(InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE, 0))
    }

    @Test fun sensitiveNonPasswordFieldsLoseSuggestionsButKeepTyping() {
        val variations = listOf(InputType.TYPE_TEXT_VARIATION_URI, InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS, InputType.TYPE_TEXT_VARIATION_FILTER)
        for (variation in variations) assertEquals(EditorKind(false, false),
            classifyEditor(InputType.TYPE_CLASS_TEXT or variation, 0))
    }

    @Test fun flagsThatForbidLearningOrCompletionDisableAssistance() {
        val text = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
        assertEquals(EditorKind(false, false),
            classifyEditor(text, EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING))
        assertEquals(EditorKind(false, false), classifyEditor(text or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS, 0))
        assertEquals(EditorKind(false, false), classifyEditor(text or InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE, 0))
    }

    @Test fun nonTextClassesNeverGetAssistance() {
        assertEquals(EditorKind(false, false),
            classifyEditor(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_NORMAL, 0))
        assertEquals(EditorKind(false, false),
            classifyEditor(InputType.TYPE_CLASS_PHONE, 0))
        assertEquals(EditorKind(false, false),
            classifyEditor(InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_NORMAL, 0))
        assertEquals(EditorKind(false, false), classifyEditor(InputType.TYPE_NULL, 0))
    }
}
