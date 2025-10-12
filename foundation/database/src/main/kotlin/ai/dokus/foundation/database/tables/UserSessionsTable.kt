package ai.dokus.foundation.database.tables

import ai.dokus.foundation.database.dbEnumeration
import ai.dokus.foundation.domain.DeviceType
import ai.dokus.foundation.domain.model.auth.SessionRevokeReason
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object UserSessionsTable : UUIDTable("user_sessions") {
    val userId = reference("user_id", UsersTable)
    val sessionToken = varchar("session_token", 500).uniqueIndex()
    val refreshToken = varchar("refresh_token", 500).nullable()
    val ipAddress = varchar("ip_address", 45)
    val userAgent = text("user_agent").nullable()
    val deviceId = varchar("device_id", 255).nullable()
    val deviceType = dbEnumeration<DeviceType>("device_type")
    val location = varchar("location", 255).nullable()
    val createdAt = timestamp("created_at")
    val expiresAt = timestamp("expires_at")
    val lastActivityAt = timestamp("last_activity_at")
    val revokedAt = timestamp("revoked_at").nullable()
    val revokedReason = dbEnumeration<SessionRevokeReason>("revoked_reason").nullable()
    val revokedBy = reference("revoked_by", UsersTable).nullable()
}