package ly.anoma.hex.keyboard

import ly.anoma.hex.speech.SpeechPhase

/** The view an input session shows: the full KeyPlane or the slim DictationBar.
 * KeyPlane-only behavior (layouts, suggestions, menus) stays on KeyPlane. */
internal interface ImeView {
    fun status(phase: SpeechPhase, hint: String)
    fun setLevel(value: Float)
    fun cancelGestures()
}
