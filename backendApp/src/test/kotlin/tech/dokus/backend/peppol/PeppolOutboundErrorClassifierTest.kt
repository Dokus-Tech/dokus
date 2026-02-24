package tech.dokus.backend.peppol

import org.junit.jupiter.api.Test
import tech.dokus.peppol.provider.client.RecommandApiException
import tech.dokus.peppol.service.PeppolOutboundErrorClassifier
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PeppolOutboundErrorClassifierTest {

    private val classifier = PeppolOutboundErrorClassifier()

    @Test
    fun `429 and 5xx are retryable`() {
        val tooMany = classifier.classify(RecommandApiException(429, "rate limited"))
        val serverError = classifier.classify(RecommandApiException(503, "down"))

        assertTrue(tooMany.retryable)
        assertEquals("PROVIDER_RATE_LIMIT", tooMany.errorCode)

        assertTrue(serverError.retryable)
        assertEquals("PROVIDER_SERVER_ERROR", serverError.errorCode)
    }

    @Test
    fun `4xx validation and auth failures are permanent`() {
        val statuses = listOf(400, 401, 403, 404, 422)
        statuses.forEach { status ->
            val classification = classifier.classify(RecommandApiException(status, "body"))
            assertFalse(classification.retryable)
        }

        assertEquals("PROVIDER_BAD_REQUEST", classifier.classify(RecommandApiException(400, "")).errorCode)
        assertEquals("PROVIDER_UNAUTHORIZED", classifier.classify(RecommandApiException(401, "")).errorCode)
        assertEquals("PROVIDER_FORBIDDEN", classifier.classify(RecommandApiException(403, "")).errorCode)
        assertEquals("PROVIDER_NOT_FOUND", classifier.classify(RecommandApiException(404, "")).errorCode)
        assertEquals("PROVIDER_UNPROCESSABLE", classifier.classify(RecommandApiException(422, "")).errorCode)
    }

    @Test
    fun `timeout and io errors are retryable`() {
        val timeout = classifier.classify(SocketTimeoutException("timeout"))
        val io = classifier.classify(IOException("network down"))

        assertTrue(timeout.retryable)
        assertEquals("NETWORK_TIMEOUT", timeout.errorCode)

        assertTrue(io.retryable)
        assertEquals("NETWORK_IO", io.errorCode)
    }

    @Test
    fun `validation exception is permanent with stable code`() {
        val result = classifier.classify(IllegalArgumentException("bad invoice"))

        assertFalse(result.retryable)
        assertEquals("VALIDATION_ERROR", result.errorCode)
        assertEquals("bad invoice", result.humanMessage)
    }
}
