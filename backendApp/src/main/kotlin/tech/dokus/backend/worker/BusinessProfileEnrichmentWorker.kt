@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.backend.worker

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tech.dokus.backend.services.business.BusinessLogoSelectionService
import tech.dokus.backend.services.business.BusinessProfileService
import tech.dokus.backend.services.business.BusinessWebsiteProbe
import tech.dokus.backend.services.business.BusinessWebsiteRanker
import tech.dokus.backend.services.business.LogoPipelineTotalBudgetMs
import tech.dokus.database.repository.auth.AddressRepository
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.business.BusinessProfileEnrichmentJob
import tech.dokus.database.repository.business.BusinessProfileEnrichmentJobRepository
import tech.dokus.database.repository.business.BusinessProfileRepository
import tech.dokus.database.repository.contacts.ContactAddressRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.features.ai.agents.BusinessLogoFallbackAgent
import tech.dokus.features.ai.agents.BusinessProfileContentExtractionAgent
import tech.dokus.features.ai.queue.LlmQueue
import tech.dokus.foundation.backend.config.BusinessProfileEnrichmentConfig
import tech.dokus.foundation.backend.storage.AvatarStorageService
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi

@Suppress("LongParameterList")
class BusinessProfileEnrichmentWorker(
    private val config: BusinessProfileEnrichmentConfig,
    private val jobRepository: BusinessProfileEnrichmentJobRepository,
    profileRepository: BusinessProfileRepository,
    businessProfileService: BusinessProfileService,
    tenantRepository: TenantRepository,
    addressRepository: AddressRepository,
    contactRepository: ContactRepository,
    contactAddressRepository: ContactAddressRepository,
    avatarStorageService: AvatarStorageService,
    contentExtractionAgent: BusinessProfileContentExtractionAgent,
    logoFallbackAgent: BusinessLogoFallbackAgent,
    websiteProbe: BusinessWebsiteProbe,
    websiteRanker: BusinessWebsiteRanker,
    logoSelectionService: BusinessLogoSelectionService,
    llmQueue: LlmQueue,
) {
    private companion object {
        val MinStaleProcessingLease: Duration = LogoPipelineTotalBudgetMs.milliseconds + 5.minutes
    }

    private val subjectContextLoader = BusinessSubjectContextLoader(
        tenantRepository = tenantRepository,
        addressRepository = addressRepository,
        contactRepository = contactRepository,
        contactAddressRepository = contactAddressRepository
    )
    private val logoResolver = BusinessProfileLogoResolver(
        profileRepository = profileRepository,
        tenantRepository = tenantRepository,
        avatarStorageService = avatarStorageService,
        logoFallbackAgent = logoFallbackAgent,
        logoSelectionService = logoSelectionService,
        llmQueue = llmQueue
    )
    private val jobProcessor = BusinessProfileEnrichmentJobProcessor(
        config = config,
        jobRepository = jobRepository,
        businessProfileService = businessProfileService,
        contentExtractionAgent = contentExtractionAgent,
        websiteProbe = websiteProbe,
        websiteRanker = websiteRanker,
        subjectContextLoader = subjectContextLoader,
        logoResolver = logoResolver,
        llmQueue = llmQueue
    )
    private val logger = loggerFor()
    private var scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pollingJob: Job? = null
    private val running = AtomicBoolean(false)

    fun start() {
        if (!config.enabled) {
            logger.info("Business profile enrichment worker disabled by config")
            return
        }
        if (config.serperApiKey.isBlank()) {
            logger.warn("Business profile enrichment worker disabled: serperApiKey is not configured")
            return
        }
        if (!running.compareAndSet(false, true)) {
            logger.warn("Business profile enrichment worker already running")
            return
        }
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        pollingJob = scope.launch {
            while (isActive && running.get()) {
                try {
                    processBatch()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("Business profile enrichment loop failed", e)
                }
                delay(config.pollingIntervalMs)
            }
        }
        logger.info(
            "Started business profile enrichment worker (interval={}ms, batch={}, maxAttempts={})",
            config.pollingIntervalMs,
            config.batchSize,
            config.maxAttempts
        )
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        pollingJob?.cancel()
        pollingJob = null
        scope.coroutineContext[Job]?.cancel()
        logger.info("Stopped business profile enrichment worker")
    }

    private suspend fun processBatch() {
        val nowInstant = Clock.System.now()
        val now = nowInstant.toLocalDateTime(TimeZone.UTC)
        val staleLease = maxOf(config.staleLeaseMinutes.minutes, MinStaleProcessingLease)
        val staleBefore = (nowInstant - staleLease).toLocalDateTime(TimeZone.UTC)
        jobRepository.recoverStaleProcessing(
            staleBefore = staleBefore,
            retryAt = now,
            reason = "Recovered stale business profile enrichment lease"
        ).onFailure { logger.error("Failed to recover stale enrichment jobs", it) }

        val jobs = jobRepository.claimDue(now = now, limit = config.batchSize)
            .getOrElse { error ->
                logger.error("Failed to claim enrichment jobs", error)
                return
            }

        if (jobs.isEmpty()) {
            logger.debug("No business profile enrichment jobs due")
            return
        }
        logger.info("Processing {} business profile enrichment job(s)", jobs.size)

        supervisorScope {
            jobs.map { job -> async { processSingleJob(job) } }.awaitAll()
        }
    }

    internal suspend fun processBatchForTest() {
        running.set(true)
        try {
            processBatch()
        } finally {
            running.set(false)
        }
    }

    private suspend fun processSingleJob(job: BusinessProfileEnrichmentJob) {
        runSuspendCatching {
            jobProcessor.process(job)
        }.onFailure { error ->
            logger.error(
                "Business profile enrichment failed for job={}, subjectType={}, subjectId={}",
                job.id,
                job.subjectType,
                job.subjectId,
                error
            )
            scheduleRetry(job, error)
        }
    }

    private suspend fun scheduleRetry(job: BusinessProfileEnrichmentJob, error: Throwable) {
        val nextAttemptCount = job.attemptCount + 1
        if (nextAttemptCount >= config.maxAttempts) {
            jobRepository.markCompletedWithError(
                jobId = job.id,
                error = "Max attempts reached: ${error.message ?: "unknown"}"
            )
            return
        }
        val delayDuration = retryDelay(nextAttemptCount)
        val nextAttemptAt = (Clock.System.now() + delayDuration).toLocalDateTime(TimeZone.UTC)
        jobRepository.scheduleRetry(
            jobId = job.id,
            attemptCount = nextAttemptCount,
            nextAttemptAt = nextAttemptAt,
            error = error.message ?: error::class.simpleName.orEmpty()
        )
    }

    private fun retryDelay(attemptCount: Int): Duration {
        return when (attemptCount) {
            1 -> 1.minutes
            2 -> 5.minutes
            3 -> 15.minutes
            4 -> 1.hours
            else -> 6.hours
        }
    }
}
