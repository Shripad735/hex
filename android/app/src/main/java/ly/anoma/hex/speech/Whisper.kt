package ly.anoma.hex.speech

import android.content.Context
import android.content.res.AssetManager

internal interface Recognizer {
    fun decode(pcm: ShortArray, count: Int, language: String, translate: Boolean): String?
}

internal class LocalSpeech(private val app: Context) : Recognizer {
    override fun decode(pcm: ShortArray, count: Int, language: String, translate: Boolean): String? {
        val threads = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
        val small = ModelStore.installed(app)
        val choice = DictationModel.matching(SpeechPreferences(app).model)
        val bytes = if (resolveModel(choice, small != null) && small != null) {
            Whisper.decode(app.assets, small.absolutePath, pcm, count, threads, language, translate)
        } else null
        return (bytes ?: Whisper.decode(app.assets, null, pcm, count, threads, language, translate))
            ?.decodeToString()?.trim()
    }
}

internal object Whisper {
    init { System.loadLibrary("hexspeech") }
    external fun decode(assets: AssetManager, file: String?, pcm: ShortArray, count: Int, threads: Int,
                        language: String, translate: Boolean): ByteArray?
}
