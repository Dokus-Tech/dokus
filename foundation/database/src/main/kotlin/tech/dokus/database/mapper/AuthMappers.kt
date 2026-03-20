package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.entity.ActiveTokenEntity
import tech.dokus.database.entity.TenantInvitationEntity
import tech.dokus.database.entity.WelcomeEmailJobEntity
import tech.dokus.database.repository.auth.RefreshTokenInfo
import tech.dokus.database.tables.auth.RefreshTokensTable
import tech.dokus.database.tables.auth.TenantInvitationsTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.database.tables.auth.WelcomeEmailJobsTable
import tech.dokus.database.utils.toKotlinxInstant
import tech.dokus.domain.Email
import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.TenantInvitationDto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
internal fun TenantInvitationEntity.Companion.from(row: ResultRow): TenantInvitationEntity {
    val inviterFirstName = row[UsersTable.firstName]
    val inviterLastName = row[UsersTable.lastName]
    val inviterEmail = row[UsersTable.email]
    val inviterName = listOfNotNull(inviterFirstName, inviterLastName)
        .joinToString(" ")
        .ifEmpty { inviterEmail }

    return TenantInvitationEntity(
        id = InvitationId(row[TenantInvitationsTable.id].value.toKotlinUuid()),
        tenantId = TenantId(row[TenantInvitationsTable.tenantId].value.toKotlinUuid()),
        email = Email(row[TenantInvitationsTable.email]),
        role = row[TenantInvitationsTable.role],
        invitedByName = inviterName,
        status = row[TenantInvitationsTable.status],
        expiresAt = row[TenantInvitationsTable.expiresAt],
        createdAt = row[TenantInvitationsTable.createdAt]
    )
}

fun TenantInvitationDto.Companion.from(entity: TenantInvitationEntity): TenantInvitationDto = TenantInvitationDto(
    id = entity.id,
    tenantId = entity.tenantId,
    email = entity.email,
    role = entity.role,
    invitedByName = entity.invitedByName,
    status = entity.status,
    expiresAt = entity.expiresAt,
    createdAt = entity.createdAt,
)

internal fun ActiveTokenEntity.Companion.from(row: ResultRow): ActiveTokenEntity {
    val rowId = row[RefreshTokensTable.id].value
    val tokenId = rowId.toString()
    val storedSessionId = SessionId(row[RefreshTokensTable.sessionId].toString())
    val accessTokenJti = row[RefreshTokensTable.accessTokenJti]

    return ActiveTokenEntity(
        rowId = rowId,
        token = RefreshTokenInfo(
            tokenId = tokenId,
            storedSessionId = storedSessionId,
            sessionId = storedSessionId,
            createdAt = row[RefreshTokensTable.createdAt].toKotlinxInstant(),
            expiresAt = row[RefreshTokensTable.expiresAt].toKotlinxInstant(),
            isRevoked = row[RefreshTokensTable.isRevoked],
            accessTokenJti = accessTokenJti,
            accessTokenExpiresAt = row[RefreshTokensTable.accessTokenExpiresAt]?.toKotlinxInstant(),
            deviceType = row[RefreshTokensTable.deviceType],
            ipAddress = row[RefreshTokensTable.ipAddress],
            userAgent = row[RefreshTokensTable.userAgent],
        )
    )
}

@OptIn(ExperimentalUuidApi::class)
internal fun WelcomeEmailJobEntity.Companion.from(row: ResultRow): WelcomeEmailJobEntity = WelcomeEmailJobEntity(
    id = row[WelcomeEmailJobsTable.id].value,
    userId = UserId(row[WelcomeEmailJobsTable.userId].toKotlinUuid()),
    tenantId = TenantId(row[WelcomeEmailJobsTable.tenantId].toKotlinUuid()),
    status = row[WelcomeEmailJobsTable.status],
    scheduledAt = row[WelcomeEmailJobsTable.scheduledAt],
    nextAttemptAt = row[WelcomeEmailJobsTable.nextAttemptAt],
    attemptCount = row[WelcomeEmailJobsTable.attemptCount],
    lastError = row[WelcomeEmailJobsTable.lastError],
    sentAt = row[WelcomeEmailJobsTable.sentAt],
    createdAt = row[WelcomeEmailJobsTable.createdAt],
    updatedAt = row[WelcomeEmailJobsTable.updatedAt]
)
