package ai.dokus.foundation.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

/**
 * JWT refresh token management for authentication
 * Enables long-lived sessions with short-lived access tokens
 */
object RefreshTokensTable : UUIDTable("refresh_tokens") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val token = varchar("token", 500).uniqueIndex()
    val expiresAt = datetime("expires_at")
    val isRevoked = bool("is_revoked").default(false)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, userId)
        index(false, token)
        index(false, expiresAt) // For cleanup jobs
    }
}