package tech.dokus.peppol.service

import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import tech.dokus.domain.ids.TenantId
import tech.dokus.foundation.backend.config.DeploymentConfig
import tech.dokus.foundation.backend.config.HostingMode
import tech.dokus.peppol.config.PeppolModuleConfig
import tech.dokus.peppol.provider.client.RecommandCredentials

/**
 * Centralized credential resolver for Peppol operations.
 *
 * This is the SINGLE source of truth for obtaining API credentials.
 * All Peppol operations (polling, sending, connection) MUST use this resolver.
 *
 * Credential source is determined by DEPLOYMENT configuration (not per-tenant):
 * - Cloud deployment: Use master credentials from environment
 * - Self-hosted deployment: Use per-tenant credentials from database
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
     * Check if this deployment uses managed (cloud) credentials.
     */
    fun isManagedCredentials(): Boolean
}

class PeppolCredentialResolverImpl(
    private val peppolSettingsRepository: PeppolSettingsRepository,
    private val deploymentConfig: DeploymentConfig,
    private val peppolConfig: PeppolModuleConfig
) : PeppolCredentialResolver {

    override fun isManagedCredentials(): Boolean = deploymentConfig.isCloud

    override suspend fun resolve(tenantId: TenantId): RecommandCredentials {
        val settings = peppolSettingsRepository.getSettings(tenantId).getOrNull()
            ?: throw IllegalStateException("Peppol settings not found for tenant: $tenantId")

        return when (deploymentConfig.hostingMode) {
            HostingMode.Cloud -> resolveCloudCredentials(
                settings.companyId,
                settings.peppolId.value,
                settings.testMode
            )
            HostingMode.SelfHosted -> resolveSelfHostedCredentials(
                tenantId,
                settings.companyId,
                settings.peppolId.value,
                settings.testMode
            )
        }
    }

    private fun resolveCloudCredentials(
        companyId: String,
        peppolId: String,
        testMode: Boolean
    ): RecommandCredentials {
        val masterCreds = peppolConfig.masterCredentials
            ?: throw IllegalStateException(
                "Master Peppol credentials not configured. " +
                    "Cloud deployment requires PEPPOL_MASTER_API_KEY and PEPPOL_MASTER_API_SECRET."
            )

        return RecommandCredentials(
            companyId = companyId,
            apiKey = masterCreds.apiKey,
            apiSecret = masterCreds.apiSecret,
            peppolId = peppolId,
            testMode = testMode || peppolConfig.globalTestMode
        )
    }

    private suspend fun resolveSelfHostedCredentials(
        tenantId: TenantId,
        companyId: String,
        peppolId: String,
        testMode: Boolean
    ): RecommandCredentials {
        val settingsWithCreds = peppolSettingsRepository.getSettingsWithCredentials(tenantId).getOrNull()
            ?: throw IllegalStateException("Peppol settings not found for tenant: $tenantId")

        val apiKey = settingsWithCreds.apiKey
            ?: throw IllegalStateException("Self-hosted deployment requires API key for tenant $tenantId")
        val apiSecret = settingsWithCreds.apiSecret
            ?: throw IllegalStateException("Self-hosted deployment requires API secret for tenant $tenantId")

        return RecommandCredentials(
            companyId = companyId,
            apiKey = apiKey,
            apiSecret = apiSecret,
            peppolId = peppolId,
            testMode = testMode || peppolConfig.globalTestMode
        )
    }
}
