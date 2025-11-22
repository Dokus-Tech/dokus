package ai.dokus.auth.backend.database.repository

import ai.dokus.auth.backend.database.mappers.FinancialMappers.toBusinessUser
import ai.dokus.auth.backend.database.tables.UsersTable
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.model.BusinessUser
import ai.dokus.foundation.ktor.crypto.PasswordCryptoService
import ai.dokus.foundation.ktor.database.dbQuery
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

/**
 * Helper function to convert kotlinx.datetime.LocalDateTime to kotlinx.datetime.Instant
 */
@OptIn(kotlin.time.ExperimentalTime::class)
private fun kotlinx.datetime.LocalDateTime.toKotlinxInstant(): Instant {
    val kotlinTimeInstant = this.toInstant(TimeZone.UTC)
    return Instant.fromEpochSeconds(
        kotlinTimeInstant.epochSeconds,
        kotlinTimeInstant.nanosecondsOfSecond.toLong()
    )
}

@OptIn(ExperimentalUuidApi::class)
class UserRepository(
    private val passwordCrypto: PasswordCryptoService
) {
    private val logger = LoggerFactory.getLogger(UserRepository::class.java)

    suspend fun register(
        organizationId: OrganizationId,
        email: String,
        password: String,
        firstName: String?,
        lastName: String?,
        role: UserRole
    ): BusinessUser = dbQuery {
        // Check if email already exists
        val existing = UsersTable
            .selectAll()
            .where { UsersTable.email eq email }
            .singleOrNull()

        if (existing != null) {
            throw IllegalArgumentException("User with email $email already exists")
        }

        val passwordHash = passwordCrypto.hashPassword(Password(password))

        val userId = UsersTable.insertAndGetId {
            it[UsersTable.organizationId] = organizationId.value.toJavaUuid()
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.role] = role
            it[UsersTable.firstName] = firstName
            it[UsersTable.lastName] = lastName
            it[UsersTable.isActive] = true
        }.value

        logger.info("Registered new user: $userId with email: $email for tenant: $organizationId")

        UsersTable
            .selectAll()
            .where { UsersTable.id eq userId }
            .single()
            .toBusinessUser()
    }

    suspend fun findById(id: UserId): BusinessUser? = dbQuery {
        val javaUuid = id.value.toJavaUuid()
        UsersTable
            .selectAll()
            .where { UsersTable.id eq javaUuid }
            .singleOrNull()
            ?.toBusinessUser()
    }

    suspend fun findByEmail(email: String): BusinessUser? = dbQuery {
        UsersTable
            .selectAll()
            .where { UsersTable.email eq email }
            .singleOrNull()
            ?.toBusinessUser()
    }

    suspend fun listByTenant(organizationId: OrganizationId, activeOnly: Boolean): List<BusinessUser> =
        dbQuery {
            val javaUuid = organizationId.value.toJavaUuid()

            val query = if (activeOnly) {
                UsersTable.selectAll().where {
                    (UsersTable.organizationId eq javaUuid) and (UsersTable.isActive eq true)
                }
            } else {
                UsersTable.selectAll().where { UsersTable.organizationId eq javaUuid }
            }

            query.map { it.toBusinessUser() }
        }

    suspend fun updateRole(userId: UserId, newRole: UserRole) = dbQuery {
        val javaUuid = userId.value.toJavaUuid()
        val updated = UsersTable.update({ UsersTable.id eq javaUuid }) {
            it[role] = newRole
        }

        if (updated == 0) {
            throw IllegalArgumentException("User not found: $userId")
        }

        logger.info("Updated role for user $userId to $newRole")
    }

    suspend fun updateProfile(userId: UserId, firstName: String?, lastName: String?) =
        dbQuery {
            val javaUuid = userId.value.toJavaUuid()
            val updated = UsersTable.update({ UsersTable.id eq javaUuid }) {
                if (firstName != null) it[UsersTable.firstName] = firstName
                if (lastName != null) it[UsersTable.lastName] = lastName
            }

            if (updated == 0) {
                throw IllegalArgumentException("User not found: $userId")
            }

            logger.info("Updated profile for user $userId")
        }

    suspend fun deactivate(userId: UserId, reason: String?) = dbQuery {
        val javaUuid = userId.value.toJavaUuid()
        val updated = UsersTable.update({ UsersTable.id eq javaUuid }) {
            it[isActive] = false
        }

        if (updated == 0) {
            throw IllegalArgumentException("User not found: $userId")
        }

        val reasonLog = if (reason != null) " - Reason: $reason" else ""
        logger.info("Deactivated user $userId$reasonLog")
    }

    suspend fun reactivate(userId: UserId) = dbQuery {
        val javaUuid = userId.value.toJavaUuid()
        val updated = UsersTable.update({ UsersTable.id eq javaUuid }) {
            it[isActive] = true
        }

        if (updated == 0) {
            throw IllegalArgumentException("User not found: $userId")
        }

        logger.info("Reactivated user $userId")
    }

    suspend fun updatePassword(userId: UserId, newPassword: String) = dbQuery {
        val javaUuid = userId.value.toJavaUuid()
        val passwordHash = passwordCrypto.hashPassword(Password(newPassword))

        val updated = UsersTable.update({ UsersTable.id eq javaUuid }) {
            it[UsersTable.passwordHash] = passwordHash
        }

        if (updated == 0) {
            throw IllegalArgumentException("User not found: $userId")
        }

        logger.info("Updated password for user $userId")
    }

    suspend fun recordLogin(userId: UserId, loginTime: Instant) = dbQuery {
        val javaUuid = userId.value.toJavaUuid()
        UsersTable.update({ UsersTable.id eq javaUuid }) {
            it[lastLoginAt] = loginTime.toLocalDateTime(TimeZone.UTC)
        }

        logger.debug("Recorded login for user $userId at $loginTime")
    }

    suspend fun setupMfa(userId: UserId, mfaSecret: String) = dbQuery {
        val javaUuid = userId.value.toJavaUuid()
        val updated = UsersTable.update({ UsersTable.id eq javaUuid }) {
            it[UsersTable.mfaSecret] = mfaSecret
        }

        if (updated == 0) {
            throw IllegalArgumentException("User not found: $userId")
        }

        logger.info("Set up MFA for user $userId")
    }

    suspend fun removeMfa(userId: UserId) = dbQuery {
        val javaUuid = userId.value.toJavaUuid()
        val updated = UsersTable.update({ UsersTable.id eq javaUuid }) {
            it[mfaSecret] = null
        }

        if (updated == 0) {
            throw IllegalArgumentException("User not found: $userId")
        }

        logger.info("Removed MFA for user $userId")
    }

    suspend fun verifyCredentials(email: String, password: String): BusinessUser? =
        dbQuery {
            val user = UsersTable
                .selectAll()
                .where { UsersTable.email eq email }
                .singleOrNull()
                ?: return@dbQuery null

            val passwordHash = user[UsersTable.passwordHash]
            val isValid = passwordCrypto.verifyPassword(password, passwordHash)

            if (!isValid) {
                logger.warn("Invalid password attempt for user with email: $email")
                return@dbQuery null
            }

            if (!user[UsersTable.isActive]) {
                logger.warn("Login attempt for inactive user: $email")
                return@dbQuery null
            }

            user.toBusinessUser()
        }

    // Email verification methods

    /**
     * Set email verification token for a user.
     *
     * @param userId The user ID to set verification token for
     * @param token The cryptographically secure verification token
     * @param expiry When the verification token expires
     */
    suspend fun setEmailVerificationToken(
        userId: UserId,
        token: String,
        expiry: Instant
    ) = dbQuery {
        val javaUuid = userId.value.toJavaUuid()
        val updated = UsersTable.update({ UsersTable.id eq javaUuid }) {
            it[emailVerificationToken] = token
            it[emailVerificationExpiry] = expiry.toLocalDateTime(TimeZone.UTC)
        }

        if (updated == 0) {
            throw IllegalArgumentException("User not found: $userId")
        }

        logger.debug("Set email verification token for user $userId")
    }

    /**
     * Find a user by their email verification token.
     *
     * @param token The verification token to search for
     * @return User info if found and not yet verified, null otherwise
     */
    suspend fun findByVerificationToken(token: String): EmailVerificationUserInfo? = dbQuery {
        UsersTable
            .selectAll()
            .where {
                (UsersTable.emailVerificationToken eq token) and
                (UsersTable.emailVerified eq false)
            }
            .singleOrNull()
            ?.let { row ->
                val expiry = row[UsersTable.emailVerificationExpiry]
                    ?: return@let null

                EmailVerificationUserInfo(
                    userId = UserId(row[UsersTable.id].value.toString()),
                    email = row[UsersTable.email],
                    expiresAt = expiry.toKotlinxInstant()
                )
            }
    }

    /**
     * Mark a user's email as verified and clear the verification token.
     *
     * @param userId The user ID to mark as verified
     */
    suspend fun markEmailVerified(userId: UserId) = dbQuery {
        val javaUuid = userId.value.toJavaUuid()
        val updated = UsersTable.update({ UsersTable.id eq javaUuid }) {
            it[emailVerified] = true
            it[emailVerificationToken] = null
            it[emailVerificationExpiry] = null
        }

        if (updated == 0) {
            throw IllegalArgumentException("User not found: $userId")
        }

        logger.info("Marked email as verified for user $userId")
    }

    /**
     * Check if a user's email is already verified.
     *
     * @param userId The user ID to check
     * @return true if email is verified, false otherwise
     */
    suspend fun isEmailVerified(userId: UserId): Boolean = dbQuery {
        val javaUuid = userId.value.toJavaUuid()
        UsersTable
            .selectAll()
            .where { UsersTable.id eq javaUuid }
            .singleOrNull()
            ?.get(UsersTable.emailVerified)
            ?: false
    }
}

/**
 * Information about a user for email verification purposes
 */
data class EmailVerificationUserInfo(
    val userId: UserId,
    val email: String,
    val expiresAt: Instant
)