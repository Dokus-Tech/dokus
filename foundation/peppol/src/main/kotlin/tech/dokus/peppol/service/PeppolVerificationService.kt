package tech.dokus.peppol.service

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
     * @param peppolId The PEPPOL participant ID to verify (format: "0208:BE0123456789")
     * @return Verification result indicating if the ID is blocked
     */
    suspend fun verify(peppolId: String): Result<PeppolIdVerificationResult> = runCatching {
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
            // ID is registered - check if it's registered with us (Dokus/Recommand) or another provider
            // For now, we consider any registration as "blocked" since we can't distinguish
            // TODO: In future, check if the registration is with our master account
            logger.warn("PEPPOL ID $peppolId is already registered: ${exactMatch.name}")

            PeppolIdVerificationResult(
                peppolId = peppolId,
                isBlocked = true,
                blockedBy = exactMatch.name,
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
        logger.error("Failed to verify PEPPOL ID: $peppolId", e)
    }
}
