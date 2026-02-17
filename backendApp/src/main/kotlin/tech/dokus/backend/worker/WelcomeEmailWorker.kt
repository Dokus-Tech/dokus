package tech.dokus.backend.worker

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tech.dokus.backend.services.auth.EmailService
import tech.dokus.backend.services.auth.EmailTemplateRenderer
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.database.repository.auth.WelcomeEmailJob
import tech.dokus.database.repository.auth.WelcomeEmailJobRepository
import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import tech.dokus.foundation.backend.config.EmailConfig
import tech.dokus.foundation.backend.utils.loggerFor
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class WelcomeEmailWorker(
    private val welcomeEmailJobRepository: WelcomeEmailJobRepository,
    private val userRepository: UserRepository,
    private val tenantRepository: TenantRepository,
    private val peppolSettingsRepository: PeppolSettingsRepository,
    private val emailService: EmailService,
    private val emailTemplateRenderer: EmailTemplateRenderer,
    private val emailConfig: EmailConfig
) {
    private val logger = loggerFor()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    private val running = AtomicBoolean(false)

    companion object {
        private val PollInterval = 1.minutes
        private val StaleProcessingLease = 15.minutes
        private const val BatchSize = 25
    }

    fun start() {
        if (!running.compareAndSet(false, true)) {
            logger.warn("Welcome email worker already running")
            return
        }

        logger.info("Starting welcome email worker (interval={}s)", PollInterval.inWholeSeconds)
        pollingJob = scope.launch {
            while (isActive && running.get()) {
                try {
                    processBatch()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("Welcome email worker cycle failed", e)
                }
                delay(PollInterval)
            }
        }
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) {
            logger.warn("Welcome email worker not running")
            return
        }

        logger.info("Stopping welcome email worker...")
        pollingJob?.cancel()
        pollingJob = null
        logger.info("Welcome email worker stopped")
    }

    private suspend fun processBatch() {
        val nowInstant = Clock.System.now()
        val now = nowInstant.toLocalDateTime(TimeZone.UTC)
        val staleBefore = (nowInstant - StaleProcessingLease).toLocalDateTime(TimeZone.UTC)

        welcomeEmailJobRepository.recoverStaleProcessing(
            staleBefore = staleBefore,
            retryAt = now
        ).onFailure { error ->
            logger.error("Failed to recover stale welcome email jobs", error)
        }

        val jobs = welcomeEmailJobRepository.claimDue(
            now = now,
            limit = BatchSize
        ).getOrElse { error ->
            logger.error("Failed to claim welcome email jobs", error)
            return
        }

        if (jobs.isEmpty()) {
            return
        }

        logger.info("Processing {} welcome email job(s)", jobs.size)
        for (job in jobs) {
            processJob(job)
        }
    }

    private suspend fun processJob(job: WelcomeEmailJob) {
        try {
            val user = userRepository.findById(job.userId)
            if (user == null) {
                markSentWithoutDelivery(job, "User not found")
                return
            }

            if (userRepository.hasWelcomeEmailSent(job.userId)) {
                markSentWithoutDelivery(job, "Welcome email already marked as sent")
                return
            }

            val tenant = tenantRepository.findById(job.tenantId)
            if (tenant == null) {
                markSentWithoutDelivery(job, "Tenant not found")
                return
            }

            val peppolConnected = peppolSettingsRepository.getSettings(job.tenantId)
                .getOrNull()
                ?.isEnabled == true

            val tenantName = runCatching { tenantRepository.getSettings(job.tenantId).companyName }
                .getOrNull()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: tenant.displayName.value

            val userName = user.firstName?.value
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: user.email.value.substringBefore('@')

            val template = emailTemplateRenderer.renderWelcomeWorkspaceActive(
                userName = userName,
                tenantName = tenantName,
                peppolConnected = peppolConnected,
                language = tenant.language
            )

            emailService.send(
                to = user.email.value,
                subject = template.subject,
                htmlBody = template.htmlBody,
                textBody = template.textBody,
                fromAddress = emailConfig.welcome.fromAddress,
                replyToAddress = emailConfig.welcome.replyToAddress
            ).onSuccess {
                persistSentState(job)
            }.onFailure { error ->
                logger.error("Failed to send welcome email for user {}", job.userId, error)
                scheduleRetry(job, error)
            }
        } catch (e: Exception) {
            logger.error("Failed to process welcome email job {}", job.id, e)
            scheduleRetry(job, e)
        }
    }

    private suspend fun markSentWithoutDelivery(
        job: WelcomeEmailJob,
        reason: String
    ) {
        val sentAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        welcomeEmailJobRepository.markSent(job.id, sentAt)
            .onFailure { error ->
                logger.error("Failed to close welcome email job {} ({})", job.id, reason, error)
            }
            .onSuccess { marked ->
                if (!marked) {
                    logger.warn("Welcome email job {} was not closed ({})", job.id, reason)
                } else {
                    logger.info("Closed welcome email job {} without delivery ({})", job.id, reason)
                }
            }
    }

    private suspend fun persistSentState(job: WelcomeEmailJob) {
        val sentAtInstant = Clock.System.now()
        val sentAt = sentAtInstant.toLocalDateTime(TimeZone.UTC)

        var persisted = false
        repeat(3) {
            val marked = welcomeEmailJobRepository.markSent(job.id, sentAt)
                .getOrElse { error ->
                    logger.error("Failed to persist welcome sent state for job {}", job.id, error)
                    false
                }
            if (marked) {
                persisted = true
                return@repeat
            }
            delay((it + 1) * 100L)
        }

        if (!persisted) {
            scheduleRetry(job, IllegalStateException("Email sent but failed to persist sent state"))
            return
        }

        runCatching {
            userRepository.markWelcomeEmailSent(job.userId, sentAtInstant)
        }.onFailure { error ->
            logger.error("Failed to persist user welcome sent flag for {}", job.userId, error)
        }
    }

    private suspend fun scheduleRetry(
        job: WelcomeEmailJob,
        error: Throwable
    ) {
        val nextAttemptCount = job.attemptCount + 1
        val retryIn = retryBackoff(nextAttemptCount)
        val nextAttemptAt = (Clock.System.now() + retryIn).toLocalDateTime(TimeZone.UTC)
        val message = error.message?.takeIf { it.isNotBlank() } ?: "Unknown welcome email error"

        val updated = welcomeEmailJobRepository.scheduleRetry(
            jobId = job.id,
            attemptCount = nextAttemptCount,
            nextAttemptAt = nextAttemptAt,
            errorMessage = message
        ).getOrElse { repositoryError ->
            logger.error("Failed to schedule retry for welcome job {}", job.id, repositoryError)
            false
        }

        if (updated) {
            logger.info(
                "Scheduled retry for welcome job {} in {} (attempt={})",
                job.id,
                retryIn.inWholeMinutes,
                nextAttemptCount
            )
        }
    }

    private fun retryBackoff(attemptCount: Int): Duration = when {
        attemptCount <= 1 -> 1.minutes
        attemptCount == 2 -> 5.minutes
        attemptCount == 3 -> 15.minutes
        attemptCount == 4 -> 1.hours
        else -> 6.hours
    }
}
