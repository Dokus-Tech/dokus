package ai.dokus.features.auth.backend.mapper

import ai.dokus.foundation.domain.SessionId
import ai.dokus.features.auth.domain.model.IpLocationInfo
import ai.dokus.features.auth.domain.model.SessionDto
import ai.dokus.features.auth.backend.database.entity.UserSession

fun SessionDto.Companion.fromEntity(entity: UserSession): SessionDto {
    return SessionDto(
        id = SessionId(entity.id.toString()),
        ipAddress = entity.ipAddress,
        userAgent = entity.userAgent,
        deviceId = entity.deviceId,
        deviceType = entity.deviceType,
        location = IpLocationInfo(city = entity.location),
        createdAt = entity.createdAt.epochSeconds,
        expiresAt = entity.expiresAt.epochSeconds,
        lastActivityAt = entity.lastActivityAt.epochSeconds,
        revokedAt = entity.revokedAt?.epochSeconds,
        revokedReason = entity.revokedReason,
        revokedBy = entity.revokedBy?.toString(),
    )
}