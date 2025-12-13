package ai.dokus.foundation.database.tables.auth

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * User identity table - stores user credentials and profile.
 * Users can belong to multiple organizations via TenantMembersTable.
 *
 * OWNER: auth service
 */
object UsersTable : UUIDTable("users") {
    val email = varchar("email", 255).uniqueIndex()

    // Authentication
    val passwordHash = varchar("password_hash", 255) // bcrypt

    // Profile
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)

    // Email verification
    val emailVerified = bool("email_verified").default(false)
    val emailVerificationToken = varchar("email_verification_token", 255).nullable().uniqueIndex()
    val emailVerificationExpiry = datetime("email_verification_expiry").nullable()

    // Status
    val isActive = bool("is_active").default(true)
    val lastLoginAt = datetime("last_login_at").nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // uniqueIndex already defined on column; avoid redundant non-unique index
    }
}
