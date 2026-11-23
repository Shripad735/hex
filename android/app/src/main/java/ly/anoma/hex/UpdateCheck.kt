package ly.anoma.hex

import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** On-demand update check against this fork's rolling release. Hex makes no
 * network request for this unless the person taps Check for updates; the only
 * other network operation remains the one-time speech-model download. */
internal sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Current(val sha: String) : UpdateState
    data class Available(val sha: String) : UpdateState
    data object Failed : UpdateState
}

internal object UpdateCheck {
    const val RELEASE_PAGE = "https://github.com/Shripad735/hex/releases/tag/android-vtt-latest"
    const val SOURCE_PAGE = "https://github.com/Shripad735/hex/tree/android-vtt"
    const val DEVELOPER_PAGE = "https://www.shiphandari.in"
    private const val RELEASE_API = "https://api.github.com/repos/Shripad735/hex/releases/tags/android-vtt-latest"

    /** The release's target commit SHA; null when the response isn't ours. */
    fun parseTargetSha(json: String): String? =
        Regex("\"target_commitish\"\\s*:\\s*\"([0-9a-f]{40})\"").find(json)?.groupValues?.get(1)

    /** Blocking network call — run off the main thread. Null on any failure. */
    fun fetchLatestSha(): String? {
        return try {
            val http = URL(RELEASE_API).openConnection() as HttpsURLConnection
            try {
                http.connectTimeout = 15_000
                http.readTimeout = 15_000
                http.setRequestProperty("User-Agent", "HexUpdateCheck/1")
                http.setRequestProperty("Accept", "application/vnd.github+json")
                if (http.responseCode != 200) null
                else parseTargetSha(http.inputStream.bufferedReader().use { it.readText() })
            } finally {
                http.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }
}
