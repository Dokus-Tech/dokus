package ai.dokus.foundation.ktor.db.entity

import ai.dokus.foundation.domain.DeviceType
import ai.dokus.auth.domain.model.SessionRevokeReason
import kotlinx.datetime.Instant
import java.util.UUID

data class UserSession(
    val id: UUID,
    val userId: UUID,
    val sessionToken: String,
    val refreshToken: String?,
    val ipAddress: String,
    val userAgent: String?,
    val deviceId: String?,
    val deviceType: DeviceType,
    val location: String?,
    val createdAt: Instant,
    val expiresAt: Instant,
    val lastActivityAt: Instant,
    val revokedAt: Instant?,
    val revokedReason: SessionRevokeReason?,
    val revokedBy: UUID?,
)