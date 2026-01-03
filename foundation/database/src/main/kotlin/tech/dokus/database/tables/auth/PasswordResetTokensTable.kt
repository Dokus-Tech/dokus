package tech.dokus.database.tables.auth

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Password reset token management for secure password recovery.
 *
 * Security features:
 * - One-time use tokens (isUsed flag)
 * - 1-hour expiration window
 * - Cryptographically secure token generation
 * - Cascade deletion when user is removed
 * - Indexed for efficient lookups and cleanup
 *
 * OWNER: auth service
 */
object PasswordResetTokensTable : UUIDTable("password_reset_tokens") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE).index()
    val tokenHash = varchar("token_hash", 64).uniqueIndex()
    val expiresAt = datetime("expires_at").index()
    val isUsed = bool("is_used").default(false)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init { }
}
