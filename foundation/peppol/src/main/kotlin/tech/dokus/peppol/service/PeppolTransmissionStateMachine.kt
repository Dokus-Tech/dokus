package tech.dokus.peppol.service

import tech.dokus.domain.enums.PeppolStatus

/**
 * Monotonic outbound PEPPOL transmission state machine.
 */
class PeppolTransmissionStateMachine {

    private val terminalStates = setOf(
        PeppolStatus.Delivered,
        PeppolStatus.Rejected,
        PeppolStatus.Failed
    )

    fun isTerminal(status: PeppolStatus): Boolean = status in terminalStates

    fun canTransition(from: PeppolStatus, to: PeppolStatus): Boolean {
        if (from == to) return true
        if (isTerminal(from)) return false

        return when (from) {
            PeppolStatus.Pending -> to in setOf(
                PeppolStatus.Queued,
                PeppolStatus.Sending,
                PeppolStatus.Sent,
                PeppolStatus.FailedRetryable,
                PeppolStatus.Failed,
                PeppolStatus.Delivered,
                PeppolStatus.Rejected
            )

            PeppolStatus.Queued -> to in setOf(
                PeppolStatus.Sending,
                PeppolStatus.Sent,
                PeppolStatus.FailedRetryable,
                PeppolStatus.Failed,
                PeppolStatus.Delivered,
                PeppolStatus.Rejected
            )

            PeppolStatus.Sending -> to in setOf(
                PeppolStatus.Sent,
                PeppolStatus.FailedRetryable,
                PeppolStatus.Failed,
                PeppolStatus.Delivered,
                PeppolStatus.Rejected
            )

            PeppolStatus.Sent -> to in setOf(
                PeppolStatus.Delivered,
                PeppolStatus.Rejected,
                PeppolStatus.Failed
            )

            PeppolStatus.FailedRetryable -> to in setOf(
                PeppolStatus.Sending,
                PeppolStatus.Sent,
                PeppolStatus.Delivered,
                PeppolStatus.Rejected,
                PeppolStatus.Failed
            )

            PeppolStatus.Delivered,
            PeppolStatus.Rejected,
            PeppolStatus.Failed -> false
        }
    }
}
