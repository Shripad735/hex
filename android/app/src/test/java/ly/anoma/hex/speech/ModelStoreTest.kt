package ly.anoma.hex.speech

import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Phase 3: pin the single network operation's allowlist. A bypass here would
 * send the model download anywhere; the check must stay fail-closed. */
class ModelStoreTest {
    @Test fun fixedHostsPass() {
        assertTrue(ModelStore.isModelUrl(URL("https://huggingface.co/ggerganov/whisper.cpp/resolve/r/ggml-small-q5_1.bin")))
        assertTrue(ModelStore.isModelUrl(URL("https://xyz.cdn.hf.co/big-model.bin")))
        assertTrue(ModelStore.isModelUrl(URL("https://cas-bridge.xethub.hf.co/model.bin")))
        assertTrue(ModelStore.isModelUrl(URL("https://huggingface.co:443/model.bin")))
    }

    @Test fun lookalikeHostsFail() {
        assertFalse(ModelStore.isModelUrl(URL("https://huggingface.co.evil.com/model.bin")))
        assertFalse(ModelStore.isModelUrl(URL("https://evilcdn.hf.co/model.bin")))
        assertFalse(ModelStore.isModelUrl(URL("https://hf.co/model.bin")))
    }

    @Test fun nonHttpsUserInfoAndPortsFail() {
        assertFalse(ModelStore.isModelUrl(URL("http://huggingface.co/model.bin")))
        assertFalse(ModelStore.isModelUrl(URL("https://user@huggingface.co/model.bin")))
        assertFalse(ModelStore.isModelUrl(URL("https://huggingface.co:8443/model.bin")))
        assertFalse(ModelStore.isModelUrl(URL("https://huggingface.co:80/model.bin")))
    }

    @Test fun explicitTinyAlwaysUsesTiny() {
        assertFalse(resolveModel(DictationModel.Tiny, true))
        assertFalse(resolveModel(DictationModel.Tiny, false))
    }

    @Test fun explicitSmallPrefersSmallButFallsBackToTiny() {
        // LocalSpeech checks the installed file; resolveModel states the preference.
        assertTrue(resolveModel(DictationModel.Small, true))
        assertTrue(resolveModel(DictationModel.Small, false))
    }

    @Test fun automaticUsesSmallWhenInstalledElseTiny() {
        assertTrue(resolveModel(null, true))
        assertFalse(resolveModel(null, false))
        assertEquals(DictationModel.Tiny, DictationModel.matching("tiny"))
        assertEquals(DictationModel.Small, DictationModel.matching("small"))
        assertNull(DictationModel.matching("medium"))
        assertNull(DictationModel.matching(null))
    }
}
