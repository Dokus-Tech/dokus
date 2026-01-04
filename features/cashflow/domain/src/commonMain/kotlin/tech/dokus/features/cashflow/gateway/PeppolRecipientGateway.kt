package tech.dokus.features.cashflow.gateway

import tech.dokus.domain.model.PeppolVerifyResponse

/**
 * Gateway for Peppol recipient verification.
 */
interface PeppolRecipientGateway {
    suspend fun verifyPeppolRecipient(peppolId: String): Result<PeppolVerifyResponse>
}
