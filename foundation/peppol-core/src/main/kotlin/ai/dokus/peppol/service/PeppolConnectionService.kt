package ai.dokus.peppol.service

import ai.dokus.foundation.database.repository.peppol.PeppolSettingsRepository
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.Address
import ai.dokus.foundation.domain.model.PeppolConnectRequest
import ai.dokus.foundation.domain.model.PeppolConnectResponse
import ai.dokus.foundation.domain.model.PeppolConnectStatus
import ai.dokus.foundation.domain.model.RecommandCompanySummary
import ai.dokus.foundation.domain.model.SavePeppolSettingsRequest
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.ktor.utils.loggerFor
import ai.dokus.peppol.providers.recommand.RecommandCompaniesClient
import ai.dokus.peppol.providers.recommand.RecommandCompany
import ai.dokus.peppol.providers.recommand.RecommandCreateCompanyRequest
import ai.dokus.peppol.providers.recommand.RecommandUnauthorizedException

class PeppolConnectionService(
    private val settingsRepository: PeppolSettingsRepository,
    private val recommandCompaniesClient: RecommandCompaniesClient,
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
                testMode = request.testMode
            ).getOrThrow()
        } catch (e: RecommandUnauthorizedException) {
            logger.warn("Recommand credentials rejected for tenant {}", tenant.id)
            return@runCatching PeppolConnectResponse(PeppolConnectStatus.InvalidCredentials)
        }

        val matchingCompanies = companies
            .filter { normalizeVat(it.vatNumber) == vatNormalized }
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

        val peppolId = "0208:${vatNormalized}"

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
                country = country,
                vatNumber = vatNumber.normalized
            ),
            testMode = request.testMode
        ).getOrThrow()
    }

    private fun RecommandCompany.toSummary(): RecommandCompanySummary = RecommandCompanySummary(
        id = id,
        name = name,
        vatNumber = vatNumber,
        enterpriseNumber = enterpriseNumber
    )

    private fun normalizeVat(raw: String): String = raw
        .replace(".", "")
        .replace(" ", "")
        .uppercase()
}

private class MissingCompanyAddressException : RuntimeException()
