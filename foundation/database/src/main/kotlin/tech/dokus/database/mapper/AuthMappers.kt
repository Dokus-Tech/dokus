package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.repository.auth.ActiveTokenRow
import tech.dokus.database.repository.auth.RefreshTokenInfo
import tech.dokus.database.repository.auth.WelcomeEmailJob
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
import tech.dokus.domain.model.TenantInvitation
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
internal fun ResultRow.toTenantInvitation(): TenantInvitation {
    val inviterFirstName = this[UsersTable.firstName]
    val inviterLastName = this[UsersTable.lastName]
    val inviterEmail = this[UsersTable.email]
    val inviterName = listOfNotNull(inviterFirstName, inviterLastName)
        .joinToString(" ")
        .ifEmpty { inviterEmail }

    return TenantInvitation(
        id = InvitationId(this[TenantInvitationsTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[TenantInvitationsTable.tenantId].value.toKotlinUuid()),
        email = Email(this[TenantInvitationsTable.email]),
        role = this[TenantInvitationsTable.role],
        invitedByName = inviterName,
        status = this[TenantInvitationsTable.status],
        expiresAt = this[TenantInvitationsTable.expiresAt],
        createdAt = this[TenantInvitationsTable.createdAt]
    )
}

internal fun ResultRow.toActiveTokenRow(): ActiveTokenRow {
    val rowId = this[RefreshTokensTable.id].value
    val tokenId = rowId.toString()
    val storedSessionId = SessionId(this[RefreshTokensTable.sessionId].toString())
    val accessTokenJti = this[RefreshTokensTable.accessTokenJti]

    return ActiveTokenRow(
        rowId = rowId,
        token = RefreshTokenInfo(
            tokenId = tokenId,
            storedSessionId = storedSessionId,
            sessionId = storedSessionId,
            createdAt = this[RefreshTokensTable.createdAt].toKotlinxInstant(),
            expiresAt = this[RefreshTokensTable.expiresAt].toKotlinxInstant(),
            isRevoked = this[RefreshTokensTable.isRevoked],
            accessTokenJti = accessTokenJti,
            accessTokenExpiresAt = this[RefreshTokensTable.accessTokenExpiresAt]?.toKotlinxInstant(),
            deviceType = this[RefreshTokensTable.deviceType],
            ipAddress = this[RefreshTokensTable.ipAddress],
            userAgent = this[RefreshTokensTable.userAgent],
        )
    )
}

@OptIn(ExperimentalUuidApi::class)
internal fun ResultRow.toWelcomeEmailJob(): WelcomeEmailJob = WelcomeEmailJob(
    id = this[WelcomeEmailJobsTable.id].value,
    userId = UserId(this[WelcomeEmailJobsTable.userId].toKotlinUuid()),
    tenantId = TenantId(this[WelcomeEmailJobsTable.tenantId].toKotlinUuid()),
    status = this[WelcomeEmailJobsTable.status],
    scheduledAt = this[WelcomeEmailJobsTable.scheduledAt],
    nextAttemptAt = this[WelcomeEmailJobsTable.nextAttemptAt],
    attemptCount = this[WelcomeEmailJobsTable.attemptCount],
    lastError = this[WelcomeEmailJobsTable.lastError],
    sentAt = this[WelcomeEmailJobsTable.sentAt],
    createdAt = this[WelcomeEmailJobsTable.createdAt],
    updatedAt = this[WelcomeEmailJobsTable.updatedAt]
)
