package tech.dokus.backend.worker

import org.junit.jupiter.api.Test
import java.util.Base64
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PeppolAttachmentBase64DecodeTest {

    @Test
    fun `decodes MIME base64 payload with line breaks and data url prefix`() {
        val bytes = "fake-pdf-binary".encodeToByteArray()
        val mimeBase64 = Base64.getMimeEncoder(8, "\n".encodeToByteArray()).encodeToString(bytes)
        val dataUrl = "data:application/pdf;base64,$mimeBase64"

        val decoded = decodePeppolAttachmentBase64(dataUrl)

        assertContentEquals(bytes, decoded)
    }

    @Test
    fun `throws explicit error for invalid attachment payload`() {
        val error = assertFailsWith<IllegalArgumentException> {
            decodePeppolAttachmentBase64("not%base64%%%")
        }

        assertTrue(error.message?.contains("Invalid PEPPOL attachment base64 payload") == true)
    }
}
