package ai.dokus.auth.backend.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

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
 * Lifecycle:
 * 1. User requests password reset via email
 * 2. System generates secure token and stores it here
 * 3. Email sent with reset link containing token
 * 4. User clicks link and submits new password
 * 5. Token marked as used after successful reset
 * 6. All user refresh tokens revoked (force re-login everywhere)
 */
object PasswordResetTokensTable : UUIDTable("password_reset_tokens") {
    val userId = reference("user_id", UsersTable, onDelete = ReferenceOption.CASCADE)
    val token = varchar("token", 255).uniqueIndex()
    val expiresAt = datetime("expires_at")
    val isUsed = bool("is_used").default(false)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, userId)
        index(false, token)
        index(false, expiresAt) // For cleanup jobs
    }
}
