package tech.dokus.database.tables.auth

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.domain.DeviceType
import tech.dokus.foundation.backend.database.dbEnumeration

/**
 * JWT refresh token management for authentication.
 * Enables long-lived sessions with short-lived access tokens.
 *
 * OWNER: auth service
 */
object RefreshTokensTable : UUIDTable("refresh_tokens") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE).index()
    val tokenHash = varchar("token_hash", 64).uniqueIndex()
    val expiresAt = datetime("expires_at").index()
    val isRevoked = bool("is_revoked").default(false)
    val accessTokenJti = varchar("access_token_jti", 255).nullable().index()
    val accessTokenExpiresAt = datetime("access_token_expires_at").nullable()
    val deviceType = dbEnumeration<DeviceType>("device_type").default(DeviceType.Desktop)
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        // token already unique index; user/expires indexed on columns
    }
}
