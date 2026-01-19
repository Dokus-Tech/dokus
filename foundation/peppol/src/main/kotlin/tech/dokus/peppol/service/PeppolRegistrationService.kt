package tech.dokus.peppol.service

import tech.dokus.database.repository.auth.AddressRepository
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.peppol.PeppolRegistrationRepository
import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import tech.dokus.domain.enums.PeppolRegistrationStatus
import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.PeppolNextAction
import tech.dokus.domain.model.PeppolRegistrationResponse
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.peppol.config.PeppolModuleConfig
import tech.dokus.peppol.provider.client.RecommandCompaniesClient
import tech.dokus.peppol.provider.client.recommand.model.RecommandCompanyCountry
import tech.dokus.peppol.provider.client.recommand.model.RecommandCreateCompanyRequest
import tech.dokus.peppol.provider.client.recommand.model.RecommandUpdateCompanyRequest

/**
 * Service for managing PEPPOL registration state machine.
 *
 * State transitions:
 * - NOT_CONFIGURED -> PENDING (via enablePeppol when ID is available)
 * - NOT_CONFIGURED -> WAITING_TRANSFER (via waitForTransfer when ID is blocked)
 * - NOT_CONFIGURED -> EXTERNAL (via optOut)
 * - PENDING -> ACTIVE (automatic after successful registration)
 * - PENDING -> FAILED (if registration fails)
 * - WAITING_TRANSFER -> ACTIVE (when transfer completes)
 * - WAITING_TRANSFER -> SENDING_ONLY (if transfer not possible)
 * - FAILED -> PENDING (via retryRegistration)
 */
class PeppolRegistrationService(
    private val registrationRepository: PeppolRegistrationRepository,
    private val settingsRepository: PeppolSettingsRepository,
    private val verificationService: PeppolVerificationService,
    private val recommandCompaniesClient: RecommandCompaniesClient,
    private val tenantRepository: TenantRepository,
    private val addressRepository: AddressRepository,
    private val moduleConfig: PeppolModuleConfig
) {
    private val logger = loggerFor()

    private data class TenantPeppolContext(
        val tenantId: TenantId,
        val vatNumber: VatNumber,
        val peppolId: PeppolId,
        val companyName: String,
        val addressLine: String,
        val postalCode: String,
        val city: String,
        val country: RecommandCompanyCountry,
        val enterpriseNumber: String,
    )

    private suspend fun loadTenantPeppolContext(tenantId: TenantId): TenantPeppolContext {
        val tenant = requireNotNull(tenantRepository.findById(tenantId)) {
            "Tenant not found"
        }
        val vatNumber = requireNotNull(tenant.vatNumber) {
            "Tenant VAT number is missing"
        }
        require(vatNumber.isValid) { "Tenant VAT number is invalid" }

        val address = requireNotNull(addressRepository.getCompanyAddress(tenantId)) {
            "Tenant company address is missing"
        }

        val streetLine1 = address.streetLine1?.trim().orEmpty()
        val streetLine2 = address.streetLine2?.trim().orEmpty()
        val addressLine = listOf(streetLine1, streetLine2).filter { it.isNotBlank() }.joinToString(", ")
        require(addressLine.isNotBlank()) { "Tenant address is incomplete (street)" }

        val postalCode = address.postalCode?.trim().orEmpty()
        require(postalCode.isNotBlank()) { "Tenant address is incomplete (postalCode)" }

        val city = address.city?.trim().orEmpty()
        require(city.isNotBlank()) { "Tenant address is incomplete (city)" }

        val countryCode = address.country?.trim()?.uppercase().orEmpty()
        require(countryCode.isNotBlank()) { "Tenant address is incomplete (country)" }
        val country = runCatching { RecommandCompanyCountry.valueOf(countryCode) }
            .getOrElse { throw IllegalStateException("Unsupported country for Peppol provider: $countryCode") }

        val peppolId = PeppolId("0208:${vatNumber.normalized}")
        val enterpriseNumber = vatNumber.companyNumber

        return TenantPeppolContext(
            tenantId = tenantId,
            vatNumber = vatNumber,
            peppolId = peppolId,
            companyName = tenant.legalName.value,
            addressLine = addressLine,
            postalCode = postalCode,
            city = city,
            country = country,
            enterpriseNumber = enterpriseNumber,
        )
    }

    /**
     * Enable PEPPOL for a tenant.
     *
     * Flow:
     * 1. Convert VAT number to PEPPOL ID
     * 2. Verify if ID is blocked
     * 3. If blocked: return blocked result (user chooses wait or opt-out)
     * 4. If available: register with Recommand -> ACTIVE
     */
    suspend fun enablePeppol(
        tenantId: TenantId,
    ): Result<PeppolRegistrationResponse> = runCatching {
        val ctx = loadTenantPeppolContext(tenantId)
        val vatNumber = ctx.vatNumber
        logger.info("Enabling PEPPOL for tenant $tenantId with VAT number: ${vatNumber.normalized}")

        // Convert VAT number to PEPPOL ID format (0208 = Belgian scheme)
        val peppolId = ctx.peppolId.value

        // Check existing registration
        val existing = registrationRepository.getRegistration(tenantId).getOrNull()
        if (existing != null && existing.status == PeppolRegistrationStatus.Active) {
            logger.info("Tenant $tenantId already has active PEPPOL registration")
            return@runCatching PeppolRegistrationResponse(
                registration = existing,
                nextAction = PeppolNextAction.NONE
            )
        }

        // Create or update registration in PENDING state
        if (existing == null) {
            registrationRepository.createRegistration(
                tenantId = tenantId,
                peppolId = peppolId,
                status = PeppolRegistrationStatus.Pending,
                testMode = moduleConfig.globalTestMode
            ).getOrThrow()
        } else {
            registrationRepository.updateStatus(tenantId, PeppolRegistrationStatus.Pending).getOrThrow()
        }

        // Verify if PEPPOL ID is available
        val verifyResult = verificationService.verify(vatNumber).getOrThrow()

        if (verifyResult.isBlocked) {
            // ID is blocked - user needs to choose what to do
            logger.warn("PEPPOL ID $peppolId is blocked (registered elsewhere)")
            registrationRepository.updateStatus(
                tenantId,
                PeppolRegistrationStatus.NotConfigured,
                "PEPPOL ID is already registered with another service"
            ).getOrThrow()

            val updatedReg = registrationRepository.getRegistration(tenantId).getOrThrow()!!
            return@runCatching PeppolRegistrationResponse(
                registration = updatedReg,
                nextAction = PeppolNextAction.WAIT_FOR_TRANSFER
            )
        }

        // ID is available - register with Recommand
        try {
            val masterCreds = moduleConfig.masterCredentials

            // Find or create company at Recommand
            val companies = recommandCompaniesClient.listCompanies(
                apiKey = masterCreds.apiKey,
                apiSecret = masterCreds.apiSecret,
                vatNumber = vatNumber.normalized
            ).getOrThrow()

            val matchingCompany = companies.find { VatNumber(it.vatNumber).normalized == vatNumber.normalized }

            val companyId = matchingCompany?.id
                ?: recommandCompaniesClient.createCompany(
                    apiKey = masterCreds.apiKey,
                    apiSecret = masterCreds.apiSecret,
                    request = RecommandCreateCompanyRequest(
                        name = ctx.companyName,
                        address = ctx.addressLine,
                        postalCode = ctx.postalCode,
                        city = ctx.city,
                        country = ctx.country,
                        enterpriseNumber = ctx.enterpriseNumber,
                        vatNumber = vatNumber.normalized,
                        isSmpRecipient = true,
                    )
                ).getOrThrow().id

            // Ensure company is configured for receiving (SMP recipient) and updated with our tenant info
            recommandCompaniesClient.updateCompany(
                apiKey = masterCreds.apiKey,
                apiSecret = masterCreds.apiSecret,
                companyId = companyId,
                request = RecommandUpdateCompanyRequest(
                    name = ctx.companyName,
                    address = ctx.addressLine,
                    postalCode = ctx.postalCode,
                    city = ctx.city,
                    country = ctx.country,
                    enterpriseNumber = ctx.enterpriseNumber,
                    vatNumber = vatNumber.normalized,
                    isSmpRecipient = true,
                )
            ).getOrThrow()

            // Update registration with company ID and set to ACTIVE
            registrationRepository.updateRecommandCompanyId(tenantId, companyId).getOrThrow()
            registrationRepository.updateStatus(tenantId, PeppolRegistrationStatus.Active).getOrThrow()
            registrationRepository.updateCapabilities(tenantId, canReceive = true, canSend = true).getOrThrow()

            // Create peppol_settings row so polling worker can find this tenant
            settingsRepository.saveSettings(
                tenantId = tenantId,
                companyId = companyId,
                peppolId = peppolId,
                isEnabled = true,
                testMode = moduleConfig.globalTestMode
            ).getOrThrow()

            val finalReg = registrationRepository.getRegistration(tenantId).getOrThrow()!!
            logger.info("PEPPOL registration successful for tenant $tenantId")

            PeppolRegistrationResponse(
                registration = finalReg,
                nextAction = PeppolNextAction.NONE
            )
        } catch (e: Exception) {
            logger.error("PEPPOL registration failed for tenant $tenantId", e)
            registrationRepository.updateStatus(
                tenantId,
                PeppolRegistrationStatus.Failed,
                e.message ?: "Registration failed"
            ).getOrThrow()

            val failedReg = registrationRepository.getRegistration(tenantId).getOrThrow()!!
            PeppolRegistrationResponse(
                registration = failedReg,
                nextAction = PeppolNextAction.RETRY
            )
        }
    }

    /**
     * Enable Peppol sending only.
     *
     * Used when receiving is blocked because the PEPPOL ID is registered with another service.
     * This registers the company in Recommand without publishing SMP recipient capabilities.
     */
    suspend fun enableSendingOnly(tenantId: TenantId): Result<PeppolRegistrationResponse> = runCatching {
        val ctx = loadTenantPeppolContext(tenantId)
        val vatNumber = ctx.vatNumber
        logger.info("Enabling PEPPOL sending-only for tenant $tenantId")

        // Ensure a registration row exists
        val existing = registrationRepository.getRegistration(tenantId).getOrNull()
        if (existing == null) {
            registrationRepository.createRegistration(
                tenantId = tenantId,
                peppolId = ctx.peppolId.value,
                status = PeppolRegistrationStatus.SendingOnly,
                testMode = moduleConfig.globalTestMode
            ).getOrThrow()
        }

        val masterCreds = moduleConfig.masterCredentials

        val companies = recommandCompaniesClient.listCompanies(
            apiKey = masterCreds.apiKey,
            apiSecret = masterCreds.apiSecret,
            vatNumber = vatNumber.normalized
        ).getOrThrow()

        val matchingCompany = companies.find { VatNumber(it.vatNumber).normalized == vatNumber.normalized }

        val companyId = matchingCompany?.id
            ?: recommandCompaniesClient.createCompany(
                apiKey = masterCreds.apiKey,
                apiSecret = masterCreds.apiSecret,
                request = RecommandCreateCompanyRequest(
                    name = ctx.companyName,
                    address = ctx.addressLine,
                    postalCode = ctx.postalCode,
                    city = ctx.city,
                    country = ctx.country,
                    enterpriseNumber = ctx.enterpriseNumber,
                    vatNumber = vatNumber.normalized,
                    isSmpRecipient = false,
                )
            ).getOrThrow().id

        // Ensure company is not configured as SMP recipient (sending only)
        recommandCompaniesClient.updateCompany(
            apiKey = masterCreds.apiKey,
            apiSecret = masterCreds.apiSecret,
            companyId = companyId,
            request = RecommandUpdateCompanyRequest(
                name = ctx.companyName,
                address = ctx.addressLine,
                postalCode = ctx.postalCode,
                city = ctx.city,
                country = ctx.country,
                enterpriseNumber = ctx.enterpriseNumber,
                vatNumber = vatNumber.normalized,
                isSmpRecipient = false,
            )
        ).getOrThrow()

        registrationRepository.updateRecommandCompanyId(tenantId, companyId).getOrThrow()
        registrationRepository.updateStatus(tenantId, PeppolRegistrationStatus.SendingOnly).getOrThrow()
        registrationRepository.updateCapabilities(tenantId, canReceive = false, canSend = true).getOrThrow()

        val registration = registrationRepository.getRegistration(tenantId).getOrThrow()!!
        PeppolRegistrationResponse(
            registration = registration,
            nextAction = PeppolNextAction.NONE
        )
    }

    /**
     * Set registration to WAITING_TRANSFER status.
     * User chose to wait for ID transfer from another provider.
     */
    suspend fun waitForTransfer(tenantId: TenantId): Result<PeppolRegistrationResponse> = runCatching {
        logger.info("Setting tenant $tenantId to wait for PEPPOL transfer")

        registrationRepository.setWaitingForTransfer(tenantId).getOrThrow()
        // Also enable sending capability while waiting
        registrationRepository.updateCapabilities(tenantId, canReceive = false, canSend = true).getOrThrow()

        val registration = registrationRepository.getRegistration(tenantId).getOrThrow()!!
        PeppolRegistrationResponse(
            registration = registration,
            nextAction = PeppolNextAction.NONE
        )
    }

    /**
     * Opt out of PEPPOL via Dokus.
     * User chose to manage PEPPOL externally.
     */
    suspend fun optOut(tenantId: TenantId): Result<Unit> = runCatching {
        logger.info("Tenant $tenantId opted out of PEPPOL via Dokus")
        registrationRepository.updateStatus(tenantId, PeppolRegistrationStatus.External).getOrThrow()
        registrationRepository.updateCapabilities(tenantId, canReceive = false, canSend = false).getOrThrow()
    }

    /**
     * Retry registration after failure.
     */
    suspend fun retryRegistration(
        tenantId: TenantId,
    ): Result<PeppolRegistrationResponse> = runCatching {
        logger.info("Retrying PEPPOL registration for tenant $tenantId")

        val existing = registrationRepository.getRegistration(tenantId).getOrNull()
            ?: throw IllegalStateException("No existing registration found for tenant $tenantId")

        if (existing.status != PeppolRegistrationStatus.Failed) {
            throw IllegalStateException("Can only retry from FAILED status, current: ${existing.status}")
        }

        // Clear error and retry
        registrationRepository.updateStatus(tenantId, PeppolRegistrationStatus.NotConfigured, null).getOrThrow()

        enablePeppol(tenantId).getOrThrow()
    }

    /**
     * Poll for transfer status.
     * Checks if the PEPPOL ID is now available for registration.
     */
    suspend fun pollTransferStatus(tenantId: TenantId): Result<PeppolRegistrationResponse> = runCatching {
        logger.info("Polling transfer status for tenant $tenantId")

        val existing = registrationRepository.getRegistration(tenantId).getOrNull()
            ?: throw IllegalStateException("No existing registration found for tenant $tenantId")

        if (existing.status != PeppolRegistrationStatus.WaitingTransfer &&
            existing.status != PeppolRegistrationStatus.SendingOnly
        ) {
            return@runCatching PeppolRegistrationResponse(
                registration = existing,
                nextAction = PeppolNextAction.NONE
            )
        }

        // Record the poll
        registrationRepository.recordPoll(tenantId).getOrThrow()

        // Check if ID is now available (extract VAT number from PEPPOL ID)
        // PEPPOL ID format: "0208:BE<number>" -> extract "BE<number>"
        val vatNumberFromPeppolId = existing.peppolId.removePrefix("0208:")
        val vatNumber = VatNumber(vatNumberFromPeppolId)
        val verifyResult = verificationService.verify(vatNumber).getOrThrow()

        if (!verifyResult.isBlocked) {
            // ID is now available! Register automatically.
            logger.info("PEPPOL ID ${existing.peppolId} is now available for tenant $tenantId")
            return@runCatching enablePeppol(tenantId).getOrThrow()
        }

        // Still blocked
        val updatedReg = registrationRepository.getRegistration(tenantId).getOrThrow()!!
        PeppolRegistrationResponse(
            registration = updatedReg,
            nextAction = PeppolNextAction.WAIT_FOR_TRANSFER
        )
    }

    /**
     * Get current registration status.
     */
    suspend fun getRegistration(tenantId: TenantId): Result<PeppolRegistrationResponse?> = runCatching {
        val registration = registrationRepository.getRegistration(tenantId).getOrNull()
            ?: return@runCatching null

        val nextAction = when (registration.status) {
            PeppolRegistrationStatus.NotConfigured -> null
            PeppolRegistrationStatus.Pending -> PeppolNextAction.NONE
            PeppolRegistrationStatus.Active -> PeppolNextAction.NONE
            PeppolRegistrationStatus.WaitingTransfer -> PeppolNextAction.WAIT_FOR_TRANSFER
            PeppolRegistrationStatus.SendingOnly -> PeppolNextAction.NONE
            PeppolRegistrationStatus.External -> PeppolNextAction.NONE
            PeppolRegistrationStatus.Failed -> PeppolNextAction.RETRY
        }

        PeppolRegistrationResponse(
            registration = registration,
            nextAction = nextAction
        )
    }
}
