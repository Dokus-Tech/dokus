package tech.dokus.backend.worker

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import tech.dokus.backend.services.enrichment.BusinessEnrichmentService
import tech.dokus.database.repository.enrichment.BusinessDescriptionRepository
import tech.dokus.domain.ids.TenantId
import tech.dokus.foundation.backend.utils.loggerFor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Background worker that polls for PENDING enrichment records and processes them.
 *
 * Follows the same pattern as DocumentProcessingWorker:
 * - Polling loop with configurable interval
 * - Graceful start/stop
 * - Timeout per job
 */
class BusinessEnrichmentWorker(
    private val enrichmentRepo: BusinessDescriptionRepository,
    private val enrichmentService: BusinessEnrichmentService
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val logger = loggerFor()
    private var pollingJob: Job? = null
    private val isRunning = AtomicBoolean(false)

    companion object {
        private const val POLLING_INTERVAL_MS = 10_000L // 10 seconds
        private const val PROCESSING_TIMEOUT_MS = 90_000L // 90 seconds (web scraping is slow)
        private const val BATCH_SIZE = 3
    }

    fun start() {
        if (!isRunning.compareAndSet(false, true)) {
            logger.warn("Enrichment worker already running")
            return
        }

        logger.info("Starting business enrichment worker (interval=${POLLING_INTERVAL_MS}ms)")

        pollingJob = scope.launch {
            while (isActive && isRunning.get()) {
                try {
                    processBatch()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("Error in enrichment polling loop", e)
                }
                delay(POLLING_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        if (!isRunning.compareAndSet(true, false)) {
            logger.warn("Enrichment worker not running")
            return
        }

        logger.info("Stopping business enrichment worker...")
        pollingJob?.cancel()
        pollingJob = null
        logger.info("Business enrichment worker stopped")
    }

    private suspend fun processBatch() {
        val pending = enrichmentRepo.findPendingForProcessing(BATCH_SIZE)
        if (pending.isEmpty()) return

        logger.info("Processing ${pending.size} enrichment job(s)")

        for (record in pending) {
            try {
                withTimeout(PROCESSING_TIMEOUT_MS) {
                    enrichmentService.processEnrichment(record)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Failed to process enrichment ${record.id}", e)
                val tenantId = TenantId.parse(record.tenantId.toString())
                runCatching { enrichmentRepo.markFailed(record.id, tenantId) }
            }
        }
    }
}
