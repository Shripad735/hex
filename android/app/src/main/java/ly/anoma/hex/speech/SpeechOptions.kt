package ly.anoma.hex.speech

import android.content.Context
import android.content.res.Resources

internal enum class Style { Polished, Casual, Notes, Verbatim }
internal enum class Spoken(val code: String, val label: String) {
    English("en", "English"), Spanish("es", "Español"), German("de", "Deutsch"), Norwegian("no", "Norsk"),
    Hindi("hi", "हिन्दी"), Marathi("mr", "मराठी (beta)");

    // Whisper calls Bokmål "no"; the keyboard and its model use BCP 47 "nb".
    val layoutCode get() = if (this == Norwegian) "nb" else code
    companion object {
        fun matching(code: String?): Spoken? = when (code) {
            "no", "nb", "nn" -> Norwegian
            else -> entries.firstOrNull { it.code == code }
        }
    }
}
internal data class SpeechOptions(val style: Style, val spoken: Spoken, val clean: Boolean, val translate: Boolean)

/** v1 model catalog: bundled Tiny (instant) vs downloaded Small (best accuracy).
 * Null choice is Automatic: Small when installed, else Tiny. */
internal enum class DictationModel(val id: String, val label: String, val detail: String) {
    Tiny("tiny", "Tiny", "On this phone, instant, lower accuracy"),
    Small("small", "Small", "190 MB download, best accuracy");
    companion object {
        fun matching(id: String?) = entries.firstOrNull { it.id == id }
    }
}

/** Pure model resolution, kept out of LocalSpeech so JVM tests pin it:
 * an explicit choice wins, except Small without its download falls back to Tiny. */
internal fun resolveModel(choice: DictationModel?, smallInstalled: Boolean) =
    choice != DictationModel.Tiny && (choice == DictationModel.Small || smallInstalled)

/** Pure translation rule, kept out of SpeechPreferences so JVM tests can pin it:
 * translation is a Spanish-only, opt-in, per-dictation choice. */
internal fun translateFor(spoken: Spoken, enabled: Boolean, nextEnglish: Boolean?) =
    enabled && spoken == Spoken.Spanish && (nextEnglish ?: true)
internal class SpeechPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("speech", Context.MODE_PRIVATE)
    var style: Style
        get() = Style.entries.firstOrNull { it.name == prefs.getString("style", null) } ?: Style.Polished
        set(value) { prefs.edit().putString("style", value.name).apply() }
    // Null means Automatic; never save the resolved locale over that choice.
    var language: Spoken?
        get() = Spoken.matching(prefs.getString("spoken", null))
        set(value) { prefs.edit().putString("spoken", value?.code).apply() }
    val spoken: Spoken
        get() {
            language?.let { return it }
            val locales = Resources.getSystem().configuration.locales
            for (i in 0 until locales.size()) Spoken.matching(locales[i].language)?.let { return it }
            return Spoken.English
        }
    var clean: Boolean
        get() = prefs.getBoolean("clean", false)
        set(value) { prefs.edit().putBoolean("clean", value).apply() }
    // Null is Automatic: Small when its download is installed, else Tiny.
    var model: String?
        get() = prefs.getString("model", null)
        set(value) { prefs.edit().putString("model", value).apply() }
    var translation: Boolean
        get() = prefs.getBoolean("translation", false)
        set(value) { prefs.edit().putBoolean("translation", value).apply() }
    fun options(nextEnglish: Boolean?) = SpeechOptions(style, spoken, clean,
        translateFor(spoken, translation, nextEnglish))
}
