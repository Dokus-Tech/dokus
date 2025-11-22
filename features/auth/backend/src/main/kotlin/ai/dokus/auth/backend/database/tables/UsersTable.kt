package ai.dokus.auth.backend.database.tables

import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.ktor.database.dbEnumeration
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * People who can access a tenant's account
 * Support for team members, accountants, and viewers
 */
object UsersTable : UUIDTable("users") {
    val organizationId = reference("organization_id", OrganizationTable, onDelete = ReferenceOption.CASCADE)
    val email = varchar("email", 255).uniqueIndex()

    // Authentication
    val passwordHash = varchar("password_hash", 255) // bcrypt
    val mfaSecret = varchar("mfa_secret", 255).nullable() // TOTP, must be encrypted!

    // Authorization
    val role = dbEnumeration<UserRole>("role")

    // Profile
    val firstName = varchar("first_name", 100).nullable()
    val lastName = varchar("last_name", 100).nullable()

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
        index(false, organizationId)
        index(false, email)
        index(false, organizationId, isActive) // Composite for active users query
    }
}