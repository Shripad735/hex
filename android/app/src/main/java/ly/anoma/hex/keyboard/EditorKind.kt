package ly.anoma.hex.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo

internal data class EditorKind(val secure: Boolean, val ordinary: Boolean)

/** Pure classification of the editor Hex is typing into. Passwords and other
 * sensitive fields get typing only: no context reads, no suggestions, no mic.
 * Extracted from HexKeyboard.onStartInput so JVM tests pin the matrix. */
internal fun classifyEditor(inputType: Int, imeOptions: Int): EditorKind {
    val kind = inputType and InputType.TYPE_MASK_CLASS
    val variation = inputType and InputType.TYPE_MASK_VARIATION
    val secure = when (kind) {
        InputType.TYPE_CLASS_TEXT -> variation in setOf(InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
        InputType.TYPE_CLASS_NUMBER -> variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        else -> false
    }
    val ordinary = kind == InputType.TYPE_CLASS_TEXT && !secure &&
        imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING == 0 &&
        variation !in setOf(InputType.TYPE_TEXT_VARIATION_URI, InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS, InputType.TYPE_TEXT_VARIATION_FILTER) &&
        inputType and (InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) == 0
    return EditorKind(secure, ordinary)
}
