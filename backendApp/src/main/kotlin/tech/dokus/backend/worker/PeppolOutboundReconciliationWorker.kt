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
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.peppol.service.PeppolService
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.minutes

/**
 * Reconciles non-final outbound transmissions against provider status.
 */
class PeppolOutboundReconciliationWorker(
    private val peppolService: PeppolService
) {
    companion object {
        private val PollInterval = 10.minutes
        private val ReconcileThreshold = 10.minutes
        private const val BatchSize = 100
    }

    private val logger = loggerFor("PeppolOutboundReconciliationWorker")
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null
    private val isRunning = AtomicBoolean(false)

    fun start() {
        if (!isRunning.compareAndSet(false, true)) {
            logger.warn("PEPPOL outbound reconciliation worker already running")
            return
        }

        logger.info("Starting PEPPOL outbound reconciliation worker")
        job = scope.launch {
            while (isActive && isRunning.get()) {
                try {
                    reconcileOnce()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("PEPPOL reconciliation cycle failed", e)
                }
                delay(PollInterval)
            }
        }
    }

    fun stop() {
        if (!isRunning.compareAndSet(true, false)) {
            logger.warn("PEPPOL outbound reconciliation worker not running")
            return
        }

        logger.info("Stopping PEPPOL outbound reconciliation worker")
        job?.cancel()
        job = null
    }

    suspend fun reconcileOnce(): Int {
        val olderThan = (Clock.System.now() - ReconcileThreshold).toLocalDateTime(TimeZone.UTC)
        val reconciled = peppolService.reconcileStaleOutbound(
            olderThan = olderThan,
            limit = BatchSize
        ).getOrElse {
            logger.warn("Failed reconciling PEPPOL outbound transmissions", it)
            0
        }

        if (reconciled > 0) {
            logger.info("Reconciled {} PEPPOL outbound transmissions", reconciled)
        }
        return reconciled
    }
}
