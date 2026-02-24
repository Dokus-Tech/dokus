package tech.dokus.backend.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.getKoin
import org.koin.ktor.ext.inject
import tech.dokus.backend.worker.CashflowProjectionReconciliationWorker
import tech.dokus.backend.worker.DocumentProcessingWorker
import tech.dokus.backend.worker.PeppolOutboundReconciliationWorker
import tech.dokus.backend.worker.PeppolOutboundWorker
import tech.dokus.backend.worker.PeppolPollingWorker
import tech.dokus.backend.worker.RateLimitCleanupWorker
import tech.dokus.backend.worker.WelcomeEmailWorker
import tech.dokus.foundation.backend.cache.RedisClient
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.peppol.service.PeppolWebhookSyncService

private val logger = loggerFor("BackgroundWorkers")

fun Application.configureBackgroundWorkers() {
    val processingWorker by inject<DocumentProcessingWorker>()
    val rateLimitCleanupWorker by inject<RateLimitCleanupWorker>()
    val peppolPollingWorker by inject<PeppolPollingWorker>()
    val peppolOutboundWorker by inject<PeppolOutboundWorker>()
    val peppolOutboundReconciliationWorker by inject<PeppolOutboundReconciliationWorker>()
    val peppolWebhookSyncService by inject<PeppolWebhookSyncService>()
    val cashflowProjectionReconciliationWorker by inject<CashflowProjectionReconciliationWorker>()
    val welcomeEmailWorker by inject<WelcomeEmailWorker>()

    monitor.subscribe(ApplicationStarted) {
        rateLimitCleanupWorker.start()
        logger.info("Starting document processing worker")
        processingWorker.start()
        logger.info("Starting Peppol polling worker")
        peppolPollingWorker.start()
        logger.info("Starting PEPPOL outbound worker")
        peppolOutboundWorker.start()
        logger.info("Starting PEPPOL outbound reconciliation worker")
        peppolOutboundReconciliationWorker.start()
        logger.info("Starting cashflow projection reconciliation worker")
        cashflowProjectionReconciliationWorker.start()
        logger.info("Starting welcome email worker")
        welcomeEmailWorker.start()

        CoroutineScope(Dispatchers.Default).launch {
            peppolWebhookSyncService.syncAllEnabledTenants()
                .onSuccess { summary ->
                    logger.info(
                        "PEPPOL webhook startup sync done (tenants={}, created={}, updated={}, deleted={}, failures={})",
                        summary.tenantsProcessed,
                        summary.created,
                        summary.updated,
                        summary.deleted,
                        summary.failures
                    )
                }
                .onFailure { error ->
                    logger.error("PEPPOL webhook startup sync failed", error)
                }
        }
    }

    monitor.subscribe(ApplicationStopping) {
        processingWorker.stop()
        peppolPollingWorker.stop()
        peppolOutboundWorker.stop()
        peppolOutboundReconciliationWorker.stop()
        cashflowProjectionReconciliationWorker.stop()
        welcomeEmailWorker.stop()

        // Close optional Redis connection if present.
        val redisClient = runCatching { getKoin().getOrNull<RedisClient>() }.getOrNull()
        if (redisClient != null) {
            runBlocking { redisClient.close() }
        }
    }
}
