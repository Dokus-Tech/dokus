package tech.dokus.features.cashflow.gateway

import tech.dokus.domain.model.Address
import tech.dokus.domain.model.PeppolConnectResponse
import tech.dokus.domain.model.PeppolSettingsDto

/**
 * Gateway for Peppol account configuration and settings.
 */
interface PeppolConnectionGateway {
    suspend fun connectPeppol(companyAddress: Address): Result<PeppolConnectResponse>

    suspend fun getPeppolSettings(): Result<PeppolSettingsDto?>
}
