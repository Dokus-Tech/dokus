package tech.dokus.domain.model.auth

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.database.DbEnum
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.DeviceType
import kotlin.time.ExperimentalTime
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
    val idUuid: Uuid get() = id.value

    @OptIn(ExperimentalTime::class)
    val createdAtInstant: Instant? get() = createdAt?.let { Instant.fromEpochSeconds(it) }

    @OptIn(ExperimentalTime::class)
    val expiresAtInstant: Instant? get() = expiresAt?.let { Instant.fromEpochSeconds(it) }

    @OptIn(ExperimentalTime::class)
    val lastActivityAtInstant: Instant? get() = lastActivityAt?.let { Instant.fromEpochSeconds(it) }

    @OptIn(ExperimentalTime::class)
    val revokedAtInstant: Instant? get() = revokedAt?.let { Instant.fromEpochSeconds(it) }
}

@Serializable
enum class SessionRevokeReason(override val dbValue: String) : DbEnum {
    @SerialName("EXPIRED") Expired("EXPIRED"),
    @SerialName("LOGOUT") Logout("LOGOUT"),
    @SerialName("LOGOUT_ALL") LogoutAll("LOGOUT_ALL"),
    @SerialName("REVOKE") Revoke("REVOKE"),
    @SerialName("TOKEN_REFRESH") TokenRefresh("TOKEN_REFRESH"),
    @SerialName("MAX_SESSIONS") MaxSessions("MAX_SESSIONS"),
    @SerialName("OTHER") Other("OTHER")
}