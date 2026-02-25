package tech.dokus.backend.worker

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tech.dokus.database.repository.peppol.PeppolTransmissionRepository
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.peppol.service.PeppolService
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Reliable outbound sender loop.
 *
 * Flow:
 * - recover stale SENDING leases
 * - claim due QUEUED / FAILED_RETRYABLE rows via CAS
 * - send via provider
 * - persist deterministic retry/final status
 */
class PeppolOutboundWorker(
    private val transmissionRepository: PeppolTransmissionRepository,
    private val peppolService: PeppolService
) {
    companion object {
        private val PollInterval = 15.seconds
        private val StaleSendingLease = 15.minutes
        private const val BatchSize = 50
    }

    private val logger = loggerFor("PeppolOutboundWorker")
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null
    private val isRunning = AtomicBoolean(false)

    fun start() {
        if (!isRunning.compareAndSet(false, true)) {
            logger.warn("PEPPOL outbound worker already running")
            return
        }

        logger.info("Starting PEPPOL outbound worker")
        job = scope.launch {
            while (isActive && isRunning.get()) {
                try {
                    drainOnce()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("PEPPOL outbound worker cycle failed", e)
                }
                delay(PollInterval)
            }
        }
    }

    fun stop() {
        if (!isRunning.compareAndSet(true, false)) {
            logger.warn("PEPPOL outbound worker not running")
            return
        }

        logger.info("Stopping PEPPOL outbound worker")
        job?.cancel()
        job = null
    }

    suspend fun drainOnce() {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val recovered = transmissionRepository.recoverStaleOutboundSending(
            staleBefore = (Clock.System.now() - StaleSendingLease).toLocalDateTime(TimeZone.UTC),
            retryAt = now
        ).getOrElse {
            logger.warn("Failed stale outbound lease recovery", it)
            0
        }
        if (recovered > 0) {
            logger.info("Recovered {} stale PEPPOL outbound transmissions", recovered)
        }

        val claimed = transmissionRepository.claimDueOutbound(
            now = now,
            limit = BatchSize
        ).getOrElse {
            logger.warn("Failed claiming PEPPOL outbound transmissions", it)
            emptyList()
        }

        if (claimed.isEmpty()) {
            return
        }

        logger.info("Claimed {} PEPPOL outbound transmissions", claimed.size)

        for (transmission in claimed) {
            peppolService.processOutboundTransmission(transmission)
                .onFailure {
                    logger.error(
                        "Failed processing PEPPOL outbound transmission {}",
                        transmission.id,
                        it
                    )
                }
        }
    }
}
