package tech.dokus.peppol.service

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tech.dokus.database.repository.peppol.PeppolRegistrationRepository
import tech.dokus.domain.ids.TenantId
import tech.dokus.foundation.backend.utils.loggerFor
import kotlin.time.Duration.Companion.hours

/**
 * Background service for polling WAITING_TRANSFER registrations.
 *
 * Periodically checks if blocked PEPPOL IDs have become available
 * (e.g., after the user transferred their registration from another provider).
 */
class PeppolTransferPollingService(
    private val registrationRepository: PeppolRegistrationRepository,
    private val registrationService: PeppolRegistrationService
) {
    private val logger = loggerFor()

    companion object {
        /** Minimum hours between polls for each registration */
        const val POLL_INTERVAL_HOURS = 6
    }

    /**
     * Poll all pending transfers.
     *
     * Called periodically by a scheduled job.
     * Checks each WAITING_TRANSFER registration if enough time has passed since last poll.
     *
     * @return Number of registrations polled
     */
    suspend fun pollPendingTransfers(): Int {
        logger.info("Starting PEPPOL transfer polling")

        val pending = registrationRepository.listPendingTransfers().getOrElse {
            logger.error("Failed to list pending transfers", it)
            return 0
        }

        if (pending.isEmpty()) {
            logger.debug("No pending PEPPOL transfers to poll")
            return 0
        }

        logger.info("Found ${pending.size} pending PEPPOL transfers")

        var polledCount = 0
        for (registration in pending) {
            if (shouldPoll(registration)) {
                try {
                    logger.info("Polling transfer status for tenant ${registration.tenantId}")
                    registrationService.pollTransferStatus(registration.tenantId).getOrThrow()
                    polledCount++
                } catch (e: Exception) {
                    logger.error("Failed to poll transfer for tenant ${registration.tenantId}", e)
                }
            } else {
                logger.debug("Skipping poll for tenant ${registration.tenantId} - too soon since last poll")
            }
        }

        logger.info("Polled $polledCount PEPPOL transfers")
        return polledCount
    }

    /**
     * Poll a specific tenant's transfer status.
     *
     * Can be called manually by user action.
     */
    suspend fun pollTenant(tenantId: TenantId) {
        logger.info("Manual poll requested for tenant $tenantId")
        registrationService.pollTransferStatus(tenantId).getOrElse {
            logger.error("Failed to poll transfer for tenant $tenantId", it)
            throw it
        }
    }

    private fun shouldPoll(registration: tech.dokus.domain.model.PeppolRegistrationDto): Boolean {
        val lastPoll = registration.lastPolledAt ?: return true
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        // Simple hour comparison - if more than POLL_INTERVAL_HOURS have passed
        val hoursDiff = (now.hour - lastPoll.hour + 24 * (now.dayOfYear - lastPoll.dayOfYear))
        return hoursDiff >= POLL_INTERVAL_HOURS
    }
}
