package tech.dokus.backend.routes.cashflow

import org.junit.jupiter.api.Test
import tech.dokus.domain.exceptions.DokusException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PeppolRegistrationRouteExceptionMappingTest {

    @Test
    fun `keeps typed dokus exceptions unchanged`() {
        val typed = DokusException.PeppolDirectoryUnavailable()

        val mapped = typed.toPeppolRegistrationRouteException("Failed to verify PEPPOL ID")

        assertSame(typed, mapped)
        assertIs<DokusException.PeppolDirectoryUnavailable>(mapped)
        assertEquals("PEPPOL_DIRECTORY_UNAVAILABLE", mapped.errorCode)
        assertEquals(503, mapped.httpStatusCode)
    }

    @Test
    fun `maps non domain exceptions to internal error`() {
        val mapped = IllegalStateException("boom")
            .toPeppolRegistrationRouteException("Failed to enable PEPPOL")

        val internal = assertIs<DokusException.InternalError>(mapped)
        assertTrue(internal.recoverable)
        assertEquals("INTERNAL_ERROR", internal.errorCode)
        assertEquals("Failed to enable PEPPOL", internal.errorMessage)
    }
}
