package tech.dokus.peppol.service

import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.PeppolConnectResponse
import tech.dokus.domain.model.PeppolConnectStatus
import tech.dokus.domain.model.RecommandCompanySummary
import tech.dokus.domain.model.Tenant
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.peppol.config.PeppolModuleConfig
import tech.dokus.peppol.provider.client.RecommandCompaniesClient
import tech.dokus.peppol.provider.client.RecommandUnauthorizedException
import tech.dokus.peppol.provider.client.recommand.model.RecommandCompany
import tech.dokus.peppol.provider.client.recommand.model.RecommandCompanyCountry
import tech.dokus.peppol.provider.client.recommand.model.RecommandCreateCompanyRequest

/**
 * Service for connecting tenants to Peppol.
 * Uses master credentials from environment variables.
 */
class PeppolConnectionService(
    private val settingsRepository: PeppolSettingsRepository,
    private val recommandCompaniesClient: RecommandCompaniesClient,
    private val moduleConfig: PeppolModuleConfig,
) {
    private val logger = loggerFor()

    /**
     * Connect a tenant to Peppol using master credentials.
     * Auto-detects or creates the Recommand company based on tenant VAT.
     *
     * @param tenant The tenant to connect
     * @param companyAddress The tenant's company address (for creating new companies)
     * @param testMode Whether to use test mode (defaults to global config)
     * @return Connection result
     */
    suspend fun connect(
        tenant: Tenant,
        companyAddress: Address?,
        testMode: Boolean = moduleConfig.globalTestMode
    ): Result<PeppolConnectResponse> = runCatching {
        val masterCreds = moduleConfig.masterCredentials

        val tenantVat = tenant.vatNumber
            ?: return@runCatching PeppolConnectResponse(PeppolConnectStatus.MissingVatNumber)

        val vatNormalized = tenantVat.normalized

        val companies = try {
            recommandCompaniesClient.listCompanies(
                apiKey = masterCreds.apiKey,
                apiSecret = masterCreds.apiSecret,
                vatNumber = vatNormalized,
            ).getOrThrow()
        } catch (e: RecommandUnauthorizedException) {
            logger.error("Master Peppol credentials rejected - check PEPPOL_MASTER_API_KEY/SECRET")
            return@runCatching PeppolConnectResponse(PeppolConnectStatus.InvalidCredentials)
        }

        val matchingCompanies = companies
            .filter { VatNumber(it.vatNumber).normalized == vatNormalized }
            .sortedBy { it.name.lowercase() }

        // Auto-select single match, pick first if multiple, or auto-create if none
        val resolvedCompany = when {
            matchingCompanies.size == 1 -> matchingCompanies.single()
            matchingCompanies.isEmpty() -> {
                // Auto-create company
                try {
                    createCompany(tenant, tenantVat, companyAddress)
                } catch (_: MissingCompanyAddressException) {
                    return@runCatching PeppolConnectResponse(PeppolConnectStatus.MissingCompanyAddress)
                }
            }
            else -> {
                // Multiple matches - pick the first one
                logger.warn("Multiple Peppol companies found for tenant ${tenant.id}, using first match")
                matchingCompanies.first()
            }
        }

        val peppolId = "0208:$vatNormalized"

        val savedSettings = settingsRepository.saveSettings(
            tenantId = tenant.id,
            companyId = resolvedCompany.id,
            peppolId = peppolId,
            isEnabled = true,
            testMode = testMode
        ).getOrThrow()

        logger.info("Tenant ${tenant.id} connected to Peppol with company ${resolvedCompany.id}")

        PeppolConnectResponse(
            status = PeppolConnectStatus.Connected,
            settings = savedSettings,
            company = resolvedCompany.toSummary(),
            createdCompany = matchingCompanies.isEmpty()
        )
    }

    private suspend fun createCompany(
        tenant: Tenant,
        vatNumber: VatNumber,
        companyAddress: Address?,
    ): RecommandCompany {
        val address = companyAddress ?: throw MissingCompanyAddressException()
        val street = address.streetLine1
        val postalCode = address.postalCode
        val city = address.city
        val country = address.country

        if (street.isNullOrBlank() || postalCode.isNullOrBlank() || city.isNullOrBlank() || country.isNullOrBlank()) {
            throw MissingCompanyAddressException()
        }

        logger.info("Creating Peppol company for tenant ${tenant.id}")

        val masterCreds = moduleConfig.masterCredentials
        return recommandCompaniesClient.createCompany(
            apiKey = masterCreds.apiKey,
            apiSecret = masterCreds.apiSecret,
            request = RecommandCreateCompanyRequest(
                name = tenant.legalName.value,
                address = street,
                postalCode = postalCode,
                city = city,
                country = RecommandCompanyCountry.valueOf(country),
                vatNumber = vatNumber.normalized
            ),
        ).getOrThrow()
    }

    private fun RecommandCompany.toSummary(): RecommandCompanySummary = RecommandCompanySummary(
        id = id,
        name = name,
        vatNumber = vatNumber,
        enterpriseNumber = enterpriseNumber
    )
}

private class MissingCompanyAddressException : RuntimeException()
