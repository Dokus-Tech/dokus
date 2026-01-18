package tech.dokus.peppol.service

import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import tech.dokus.domain.ids.TenantId
import tech.dokus.peppol.config.PeppolModuleConfig
import tech.dokus.peppol.provider.client.RecommandCredentials

/**
 * Centralized credential resolver for Peppol operations.
 *
 * This is the SINGLE source of truth for obtaining API credentials.
 * All Peppol operations (polling, sending, connection) MUST use this resolver.
 *
 * Credentials always come from environment variables (PEPPOL_MASTER_API_KEY/SECRET).
 */
interface PeppolCredentialResolver {
    /**
     * Resolve credentials for a tenant.
     *
     * @param tenantId The tenant to resolve credentials for
     * @return RecommandCredentials ready to use for API calls
     * @throws IllegalStateException if credentials cannot be resolved
     */
    suspend fun resolve(tenantId: TenantId): RecommandCredentials

    /**
     * Check if this deployment uses managed credentials.
     * Always returns true - credentials are always managed via environment variables.
     */
    fun isManagedCredentials(): Boolean
}

class PeppolCredentialResolverImpl(
    private val peppolSettingsRepository: PeppolSettingsRepository,
    private val peppolConfig: PeppolModuleConfig
) : PeppolCredentialResolver {

    override fun isManagedCredentials(): Boolean = true

    override suspend fun resolve(tenantId: TenantId): RecommandCredentials {
        val settings = peppolSettingsRepository.getSettings(tenantId).getOrNull()
            ?: throw IllegalStateException("Peppol settings not found for tenant: $tenantId")

        return RecommandCredentials(
            companyId = settings.companyId,
            apiKey = peppolConfig.masterCredentials.apiKey,
            apiSecret = peppolConfig.masterCredentials.apiSecret,
            peppolId = settings.peppolId.value,
            testMode = settings.testMode || peppolConfig.globalTestMode
        )
    }
}
