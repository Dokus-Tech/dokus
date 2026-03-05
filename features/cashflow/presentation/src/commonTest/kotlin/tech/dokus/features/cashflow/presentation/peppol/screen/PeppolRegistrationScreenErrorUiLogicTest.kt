package tech.dokus.features.cashflow.presentation.peppol.screen

import tech.dokus.domain.exceptions.DokusException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PeppolRegistrationScreenErrorUiLogicTest {

    @Test
    fun `directory outage uses setup fallback and allows retry`() {
        val exception = DokusException.PeppolDirectoryUnavailable()

        assertTrue(isPeppolSetupFlowError(exception))
        assertTrue(shouldShowPeppolSetupRetry(exception))
    }

    @Test
    fun `connection error uses setup fallback and allows retry`() {
        val exception = DokusException.ConnectionError()

        assertTrue(isPeppolSetupFlowError(exception))
        assertTrue(shouldShowPeppolSetupRetry(exception))
    }

    @Test
    fun `unrelated errors use generic error content`() {
        val exception = DokusException.NotFound()

        assertFalse(isPeppolSetupFlowError(exception))
        assertFalse(shouldShowPeppolSetupRetry(exception))
    }
}
