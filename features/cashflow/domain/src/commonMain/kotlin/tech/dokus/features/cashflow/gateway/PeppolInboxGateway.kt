package tech.dokus.features.cashflow.gateway

import tech.dokus.domain.model.PeppolInboxPollResponse

/**
 * Gateway for polling inbound Peppol messages.
 */
interface PeppolInboxGateway {
    suspend fun pollPeppolInbox(): Result<PeppolInboxPollResponse>
}
