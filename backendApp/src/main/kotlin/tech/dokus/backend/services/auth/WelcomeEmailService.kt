package tech.dokus.backend.services.auth

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.database.repository.auth.WelcomeEmailJobRepository
import tech.dokus.database.utils.toKotlinxInstant
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.foundation.backend.utils.loggerFor
import kotlin.time.Duration.Companion.minutes

class WelcomeEmailService(
    private val userRepository: UserRepository,
    private val tenantRepository: TenantRepository,
    private val welcomeEmailJobRepository: WelcomeEmailJobRepository
) {
    private val logger = loggerFor()

    companion object {
        private val WelcomeDelay = 15.minutes
    }

    suspend fun scheduleIfEligible(
        userId: UserId,
        tenantId: TenantId
    ): Result<Unit> = runCatching {
        val alreadySent = userRepository.hasWelcomeEmailSent(userId)
        if (alreadySent) {
            logger.debug("Skipping welcome enqueue: already sent for user {}", userId)
            return@runCatching
        }

        val tenant = tenantRepository.findById(tenantId)
            ?: error("Tenant not found for welcome scheduling: $tenantId")

        val scheduledAt = computeScheduledAt(tenant.createdAt)
        welcomeEmailJobRepository.enqueueIfAbsent(
            userId = userId,
            tenantId = tenantId,
            scheduledAt = scheduledAt
        ).getOrThrow()

        logger.info(
            "Welcome email job ensured for user {} tenant {} scheduledAt={}",
            userId,
            tenantId,
            scheduledAt
        )
    }

    private fun computeScheduledAt(tenantCreatedAt: LocalDateTime): LocalDateTime {
        val now = Clock.System.now()
        val workspaceReadyAt = tenantCreatedAt.toKotlinxInstant() + WelcomeDelay
        return if (workspaceReadyAt > now) {
            workspaceReadyAt.toLocalDateTime(TimeZone.UTC)
        } else {
            now.toLocalDateTime(TimeZone.UTC)
        }
    }
}
