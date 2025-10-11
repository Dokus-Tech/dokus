package ai.dokus.features.auth.backend.database.entity

import ai.dokus.foundation.domain.database.DbEnum
import kotlinx.datetime.Instant
import java.util.UUID

enum class LoginResult(override val dbValue: String) : DbEnum {
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    LOCKED("LOCKED"),
    EXPIRED("EXPIRED")
}

data class UserLoginAttempt(
    val id: UUID,
    val matricule: String? = null,
    val email: String? = null,
    val userId: UUID? = null,
    val ipAddress: String,
    val userAgent: String? = null,
    val result: LoginResult,
    val failureReason: String? = null,
    val attemptedAt: Instant
)