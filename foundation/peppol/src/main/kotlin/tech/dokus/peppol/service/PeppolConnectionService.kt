package tech.dokus.peppol.service

import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.PeppolConnectRequest
import tech.dokus.domain.model.PeppolConnectResponse
import tech.dokus.domain.model.PeppolConnectStatus
import tech.dokus.domain.model.RecommandCompanySummary
import tech.dokus.domain.model.SavePeppolSettingsRequest
import tech.dokus.domain.model.Tenant
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.peppol.config.PeppolModuleConfig
import tech.dokus.peppol.provider.client.RecommandCompaniesClient
import tech.dokus.peppol.provider.client.RecommandUnauthorizedException
import tech.dokus.peppol.provider.client.recommand.model.RecommandCompany
import tech.dokus.peppol.provider.client.recommand.model.RecommandCompanyCountry
import tech.dokus.peppol.provider.client.recommand.model.RecommandCreateCompanyRequest

class PeppolConnectionService(
    private val settingsRepository: PeppolSettingsRepository,
    private val recommandCompaniesClient: RecommandCompaniesClient,
    private val moduleConfig: PeppolModuleConfig,
) {
    private val logger = loggerFor()

    suspend fun connectRecommand(
        tenant: Tenant,
        companyAddress: Address?,
        request: PeppolConnectRequest,
    ): Result<PeppolConnectResponse> = runCatching {
        val tenantVat = tenant.vatNumber
            ?: return@runCatching PeppolConnectResponse(PeppolConnectStatus.MissingVatNumber)

        if (request.apiKey.isBlank() || request.apiSecret.isBlank()) {
            return@runCatching PeppolConnectResponse(PeppolConnectStatus.InvalidCredentials)
        }

        val vatNormalized = tenantVat.normalized

        val companies = try {
            recommandCompaniesClient.listCompanies(
                apiKey = request.apiKey,
                apiSecret = request.apiSecret,
                vatNumber = vatNormalized,
            ).getOrThrow()
        } catch (e: RecommandUnauthorizedException) {
            logger.warn("Recommand credentials rejected for tenant {}", tenant.id)
            return@runCatching PeppolConnectResponse(PeppolConnectStatus.InvalidCredentials)
        }

        val matchingCompanies = companies
            .filter { VatNumber(it.vatNumber).normalized == vatNormalized }
            .sortedBy { it.name.lowercase() }

        val selectedCompany = when {
            request.companyId != null -> matchingCompanies.firstOrNull { it.id == request.companyId }
            matchingCompanies.size == 1 -> matchingCompanies.single()
            else -> null
        }

        if (selectedCompany == null && matchingCompanies.isNotEmpty()) {
            return@runCatching PeppolConnectResponse(
                status = PeppolConnectStatus.MultipleMatches,
                candidates = matchingCompanies.map { it.toSummary() }
            )
        }

        val resolvedCompany = selectedCompany ?: run {
            // If no matching company and user hasn't confirmed creation, ask for confirmation
            if (!request.createCompanyIfMissing) {
                return@runCatching PeppolConnectResponse(PeppolConnectStatus.NoCompanyFound)
            }
            try {
                createCompanyForTenant(tenant, tenantVat, companyAddress, request)
            } catch (_: MissingCompanyAddressException) {
                return@runCatching PeppolConnectResponse(PeppolConnectStatus.MissingCompanyAddress)
            }
        }

        val peppolId = "0208:$vatNormalized"

        val savedSettings = settingsRepository.saveSettings(
            tenantId = tenant.id,
            request = SavePeppolSettingsRequest(
                companyId = resolvedCompany.id,
                apiKey = request.apiKey,
                apiSecret = request.apiSecret,
                peppolId = peppolId,
                isEnabled = request.isEnabled,
                testMode = request.testMode
            )
        ).getOrThrow()

        PeppolConnectResponse(
            status = PeppolConnectStatus.Connected,
            settings = savedSettings,
            company = resolvedCompany.toSummary(),
            createdCompany = selectedCompany == null && matchingCompanies.isEmpty()
        )
    }

    private suspend fun createCompanyForTenant(
        tenant: Tenant,
        vatNumber: VatNumber,
        companyAddress: Address?,
        request: PeppolConnectRequest,
    ): RecommandCompany {
        val address = companyAddress ?: throw MissingCompanyAddressException()
        val street = address.streetLine1
        val postalCode = address.postalCode
        val city = address.city
        val country = address.country.dbValue

        if (street.isBlank() || postalCode.isBlank() || city.isBlank()) {
            throw MissingCompanyAddressException()
        }

        return recommandCompaniesClient.createCompany(
            apiKey = request.apiKey,
            apiSecret = request.apiSecret,
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

    // ========================================================================
    // CLOUD CONNECTION (using master credentials)
    // ========================================================================

    /**
     * Connect a cloud-hosted tenant to Peppol using Dokus master credentials.
     * This is called automatically for cloud tenants - no user input required.
     *
     * @param tenant The tenant to connect
     * @param companyAddress The tenant's company address (for creating new companies)
     * @param testMode Whether to use test mode (usually determined by deployment env)
     * @return Connection result
     */
    suspend fun connectCloud(
        tenant: Tenant,
        companyAddress: Address?,
        testMode: Boolean = moduleConfig.globalTestMode
    ): Result<PeppolConnectResponse> = runCatching {
        val masterCreds = moduleConfig.masterCredentials
            ?: return@runCatching PeppolConnectResponse(
                status = PeppolConnectStatus.InvalidCredentials,
                // This shouldn't happen in cloud - indicates misconfiguration
            )

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

        // For cloud, auto-select single match or auto-create if none
        val resolvedCompany = when {
            matchingCompanies.size == 1 -> matchingCompanies.single()
            matchingCompanies.isEmpty() -> {
                // Auto-create company for cloud tenants
                createCompanyForCloudTenant(tenant, tenantVat, companyAddress, masterCreds.apiKey, masterCreds.apiSecret)
            }
            else -> {
                // Multiple matches - pick the first one for cloud (or could log a warning)
                logger.warn("Multiple Peppol companies found for tenant ${tenant.id}, using first match")
                matchingCompanies.first()
            }
        }

        val peppolId = "0208:$vatNormalized"

        // Save settings WITHOUT credentials (cloud tenants use master creds)
        val savedSettings = settingsRepository.saveCloudSettings(
            tenantId = tenant.id,
            companyId = resolvedCompany.id,
            peppolId = peppolId,
            isEnabled = true,
            testMode = testMode
        ).getOrThrow()

        logger.info("Cloud tenant ${tenant.id} connected to Peppol with company ${resolvedCompany.id}")

        PeppolConnectResponse(
            status = PeppolConnectStatus.Connected,
            settings = savedSettings,
            company = resolvedCompany.toSummary(),
            createdCompany = matchingCompanies.isEmpty()
        )
    }

    private suspend fun createCompanyForCloudTenant(
        tenant: Tenant,
        vatNumber: VatNumber,
        companyAddress: Address?,
        apiKey: String,
        apiSecret: String,
    ): RecommandCompany {
        val address = companyAddress ?: throw MissingCompanyAddressException()
        val street = address.streetLine1
        val postalCode = address.postalCode
        val city = address.city
        val country = address.country.dbValue

        if (street.isBlank() || postalCode.isBlank() || city.isBlank()) {
            throw MissingCompanyAddressException()
        }

        logger.info("Creating Peppol company for cloud tenant ${tenant.id}")

        return recommandCompaniesClient.createCompany(
            apiKey = apiKey,
            apiSecret = apiSecret,
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
}
private class MissingCompanyAddressException : RuntimeException()
