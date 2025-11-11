package ai.dokus.foundation.domain.model.auth

import ai.dokus.foundation.domain.DeviceType
import ai.dokus.foundation.domain.SessionId
import ai.dokus.foundation.domain.database.DbEnum
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class SessionDto(
    val id: SessionId,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val deviceId: String? = null,
    val deviceType: DeviceType,
    val location: IpLocationInfo? = null,
    val createdAt: Long? = null,
    val expiresAt: Long? = null,
    val lastActivityAt: Long? = null,
    val revokedAt: Long? = null,
    val revokedReason: SessionRevokeReason? = null,
    val revokedBy: String? = null,
    val isCurrent: Boolean = false,
) {
    @OptIn(ExperimentalUuidApi::class)
    val idUuid: Uuid? get() = runCatching { Uuid.parse(id.value) }.getOrNull()
    val createdAtInstant: Instant? get() = createdAt?.let { Instant.fromEpochSeconds(it) }
    val expiresAtInstant: Instant? get() = expiresAt?.let { Instant.fromEpochSeconds(it) }
    val lastActivityAtInstant: Instant? get() = lastActivityAt?.let { Instant.fromEpochSeconds(it) }
    val revokedAtInstant: Instant? get() = revokedAt?.let { Instant.fromEpochSeconds(it) }
}

enum class SessionRevokeReason(override val dbValue: String) : DbEnum {
    EXPIRED("EXPIRED"),
    LOGOUT("LOGOUT"),
    LOGOUT_ALL("LOGOUT_ALL"),
    REVOKE("REVOKE"),
    TOKEN_REFRESH("TOKEN_REFRESH"),
    MAX_SESSIONS("MAX_SESSIONS"),
    OTHER("OTHER")
}