package ly.anoma.hex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateCheckTest {
    @Test fun parsesReleaseTargetCommit() {
        assertEquals("0123456789abcdef0123456789abcdef01234567",
            UpdateCheck.parseTargetSha("""{"tag_name":"android-vtt-latest","target_commitish":"0123456789abcdef0123456789abcdef01234567"}"""))
    }

    @Test fun rejectsMissingShortAndNonHex() {
        assertNull(UpdateCheck.parseTargetSha("""{"tag_name":"android-vtt-latest"}"""))
        assertNull(UpdateCheck.parseTargetSha(""))
        assertNull(UpdateCheck.parseTargetSha("""{"target_commitish":"abc123"}"""))
        assertNull(UpdateCheck.parseTargetSha("""{"target_commitish":"zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"}"""))
    }

    @Test fun fallsBackToTitleShaWhenTargetIsBranchName() {
        assertEquals("1d7c970aa410",
            UpdateCheck.parseTargetSha("""{"tag_name":"android-vtt-latest","target_commitish":"android-vtt","name":"Hex for Android — latest (1d7c970aa410)"}"""))
        assertNull(UpdateCheck.parseTargetSha("""{"target_commitish":"android-vtt","name":"no sha here"}"""))
    }
}
