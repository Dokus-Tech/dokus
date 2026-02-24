package tech.dokus.backend.peppol

import org.junit.jupiter.api.Test
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.peppol.service.PeppolTransmissionStateMachine
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PeppolTransmissionStateMachineTest {

    private val stateMachine = PeppolTransmissionStateMachine()

    @Test
    fun `terminal states cannot be downgraded`() {
        assertTrue(stateMachine.isTerminal(PeppolStatus.Delivered))
        assertTrue(stateMachine.isTerminal(PeppolStatus.Rejected))
        assertTrue(stateMachine.isTerminal(PeppolStatus.Failed))

        assertFalse(stateMachine.canTransition(PeppolStatus.Delivered, PeppolStatus.Sent))
        assertFalse(stateMachine.canTransition(PeppolStatus.Rejected, PeppolStatus.Sent))
        assertFalse(stateMachine.canTransition(PeppolStatus.Failed, PeppolStatus.FailedRetryable))
    }

    @Test
    fun `out-of-order provider updates are ignored`() {
        assertFalse(stateMachine.canTransition(PeppolStatus.Sending, PeppolStatus.Queued))
        assertFalse(stateMachine.canTransition(PeppolStatus.Sent, PeppolStatus.Sending))
        assertFalse(stateMachine.canTransition(PeppolStatus.Delivered, PeppolStatus.Queued))
    }

    @Test
    fun `sent can transition to delivered or rejected`() {
        assertTrue(stateMachine.canTransition(PeppolStatus.Sent, PeppolStatus.Delivered))
        assertTrue(stateMachine.canTransition(PeppolStatus.Sent, PeppolStatus.Rejected))
    }

    @Test
    fun `failed retryable can be reconciled directly to delivered`() {
        assertTrue(stateMachine.canTransition(PeppolStatus.FailedRetryable, PeppolStatus.Delivered))
    }
}
