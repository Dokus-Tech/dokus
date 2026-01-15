package tech.dokus.peppol.service

import tech.dokus.database.repository.peppol.PeppolRegistrationRepository
import tech.dokus.domain.enums.PeppolRegistrationStatus
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.EnablePeppolRequest
import tech.dokus.domain.model.PeppolNextAction
import tech.dokus.domain.model.PeppolRegistrationResponse
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.peppol.config.PeppolModuleConfig
import tech.dokus.peppol.provider.client.RecommandCompaniesClient
import tech.dokus.peppol.provider.client.RecommandCredentials

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
    private val verificationService: PeppolVerificationService,
    private val recommandCompaniesClient: RecommandCompaniesClient,
    private val moduleConfig: PeppolModuleConfig
) {
    private val logger = loggerFor()

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
        request: EnablePeppolRequest,
        companyName: String
    ): Result<PeppolRegistrationResponse> = runCatching {
        val vatNumber = request.vatNumber
        logger.info("Enabling PEPPOL for tenant $tenantId with VAT number: ${vatNumber.normalized}")

        // Convert VAT number to PEPPOL ID format (0208 = Belgian scheme)
        val peppolId = "0208:${vatNumber.normalized}"

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
        val registration = if (existing == null) {
            registrationRepository.createRegistration(
                tenantId = tenantId,
                peppolId = peppolId,
                status = PeppolRegistrationStatus.Pending,
                testMode = moduleConfig.globalTestMode
            ).getOrThrow()
        } else {
            registrationRepository.updateStatus(tenantId, PeppolRegistrationStatus.Pending).getOrThrow()
            registrationRepository.getRegistration(tenantId).getOrThrow()!!
        }

        // Verify if PEPPOL ID is available
        val verifyResult = verificationService.verify(peppolId).getOrThrow()

        if (verifyResult.isBlocked) {
            // ID is blocked - user needs to choose what to do
            logger.warn("PEPPOL ID $peppolId is blocked by ${verifyResult.blockedBy}")
            registrationRepository.updateStatus(
                tenantId,
                PeppolRegistrationStatus.NotConfigured,
                "PEPPOL ID is registered with another provider: ${verifyResult.blockedBy}"
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

            val companyId = if (matchingCompany != null) {
                matchingCompany.id
            } else {
                // Create new company at Recommand
                val created = recommandCompaniesClient.createCompany(
                    apiKey = masterCreds.apiKey,
                    apiSecret = masterCreds.apiSecret,
                    request = tech.dokus.peppol.provider.client.recommand.model.RecommandCreateCompanyRequest(
                        name = companyName,
                        address = "", // Will be updated later
                        postalCode = "",
                        city = "",
                        country = tech.dokus.peppol.provider.client.recommand.model.RecommandCompanyCountry.BE,
                        vatNumber = vatNumber.normalized
                    )
                ).getOrThrow()
                created.id
            }

            // Update registration with company ID and set to ACTIVE
            registrationRepository.updateRecommandCompanyId(tenantId, companyId).getOrThrow()
            registrationRepository.updateStatus(tenantId, PeppolRegistrationStatus.Active).getOrThrow()
            registrationRepository.updateCapabilities(tenantId, canReceive = true, canSend = true).getOrThrow()

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
        vatNumber: VatNumber,
        companyName: String
    ): Result<PeppolRegistrationResponse> = runCatching {
        logger.info("Retrying PEPPOL registration for tenant $tenantId")

        val existing = registrationRepository.getRegistration(tenantId).getOrNull()
            ?: throw IllegalStateException("No existing registration found for tenant $tenantId")

        if (existing.status != PeppolRegistrationStatus.Failed) {
            throw IllegalStateException("Can only retry from FAILED status, current: ${existing.status}")
        }

        // Clear error and retry
        registrationRepository.updateStatus(tenantId, PeppolRegistrationStatus.NotConfigured, null).getOrThrow()

        enablePeppol(
            tenantId,
            EnablePeppolRequest(vatNumber),
            companyName
        ).getOrThrow()
    }

    /**
     * Poll for transfer status.
     * Checks if the PEPPOL ID is now available for registration.
     */
    suspend fun pollTransferStatus(tenantId: TenantId): Result<PeppolRegistrationResponse> = runCatching {
        logger.info("Polling transfer status for tenant $tenantId")

        val existing = registrationRepository.getRegistration(tenantId).getOrNull()
            ?: throw IllegalStateException("No existing registration found for tenant $tenantId")

        if (existing.status != PeppolRegistrationStatus.WaitingTransfer) {
            return@runCatching PeppolRegistrationResponse(
                registration = existing,
                nextAction = PeppolNextAction.NONE
            )
        }

        // Record the poll
        registrationRepository.recordPoll(tenantId).getOrThrow()

        // Check if ID is now available
        val verifyResult = verificationService.verify(existing.peppolId).getOrThrow()

        if (!verifyResult.isBlocked) {
            // ID is now available! Try to register
            logger.info("PEPPOL ID ${existing.peppolId} is now available for tenant $tenantId")

            // TODO: Get vatNumber and companyName from tenant
            // For now, just update status - full registration will happen on next enablePeppol call
            registrationRepository.updateStatus(
                tenantId,
                PeppolRegistrationStatus.NotConfigured,
                null
            ).getOrThrow()

            val updatedReg = registrationRepository.getRegistration(tenantId).getOrThrow()!!
            return@runCatching PeppolRegistrationResponse(
                registration = updatedReg,
                nextAction = PeppolNextAction.RETRY // Signal to try registration again
            )
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
            PeppolRegistrationStatus.SendingOnly -> PeppolNextAction.CONTACT_SUPPORT
            PeppolRegistrationStatus.External -> PeppolNextAction.NONE
            PeppolRegistrationStatus.Failed -> PeppolNextAction.RETRY
        }

        PeppolRegistrationResponse(
            registration = registration,
            nextAction = nextAction
        )
    }
}
