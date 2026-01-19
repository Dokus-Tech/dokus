package tech.dokus.peppol.service

import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.PeppolIdVerificationResult
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.peppol.config.PeppolModuleConfig
import tech.dokus.peppol.provider.client.RecommandProvider

/**
 * Service for verifying PEPPOL IDs before registration.
 *
 * Checks if a PEPPOL ID is already registered with another provider,
 * which would block Dokus from registering as the receiver.
 */
class PeppolVerificationService(
    private val recommandProvider: RecommandProvider,
    private val moduleConfig: PeppolModuleConfig
) {
    private val logger = loggerFor()

    /**
     * Verify if a PEPPOL ID is available for registration.
     *
     * @param vatNumber The VAT number to verify (will be converted to PEPPOL ID format)
     * @return Verification result indicating if the ID is blocked
     */
    suspend fun verify(vatNumber: VatNumber): Result<PeppolIdVerificationResult> = runCatching {
        // Convert VAT number to PEPPOL ID format (0208 = Belgian scheme)
        val peppolId = "0208:${vatNumber.normalized}"
        logger.info("Verifying PEPPOL ID availability: $peppolId")

        // Configure provider with master credentials
        val creds = tech.dokus.peppol.provider.client.RecommandCredentials(
            companyId = "", // Not needed for directory search
            apiKey = moduleConfig.masterCredentials.apiKey,
            apiSecret = moduleConfig.masterCredentials.apiSecret,
            peppolId = peppolId,
            testMode = moduleConfig.globalTestMode
        )
        recommandProvider.configure(creds)

        // Search the PEPPOL directory for this participant ID
        val searchResults = recommandProvider.searchDirectory(peppolId).getOrThrow()

        // Check if any result matches our exact PEPPOL ID
        val exactMatch = searchResults.find { it.peppolAddress == peppolId }

        if (exactMatch != null) {
            // ID is registered - for now we treat any registration as "blocked" because we can't
            // reliably distinguish whether the receiver is already configured by Dokus vs another service.
            //
            // IMPORTANT: Do not expose provider/receiver names in the UI ("silent by default").
            logger.warn("PEPPOL ID $peppolId is already registered (directory entry: ${exactMatch.name})")

            PeppolIdVerificationResult(
                peppolId = peppolId,
                isBlocked = true,
                blockedBy = null,
                canProceed = false
            )
        } else {
            // ID is not registered - available for registration
            logger.info("PEPPOL ID $peppolId is available for registration")

            PeppolIdVerificationResult(
                peppolId = peppolId,
                isBlocked = false,
                blockedBy = null,
                canProceed = true
            )
        }
    }.onFailure { e ->
        logger.error("Failed to verify PEPPOL ID for VAT number: ${vatNumber.normalized}", e)
    }
}
