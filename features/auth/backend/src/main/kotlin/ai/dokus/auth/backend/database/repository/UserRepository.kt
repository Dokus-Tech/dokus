package ai.dokus.auth.backend.database.repository

import ai.dokus.auth.backend.database.mappers.FinancialMappers.toUser
import ai.dokus.auth.backend.database.mappers.FinancialMappers.toOrganizationMembership
import ai.dokus.auth.backend.database.mappers.FinancialMappers.toUserInOrganization
import ai.dokus.auth.backend.database.tables.OrganizationMembersTable
import ai.dokus.auth.backend.database.tables.UsersTable
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.User
import ai.dokus.foundation.domain.model.OrganizationMembership
import ai.dokus.foundation.domain.model.UserInOrganization
import ai.dokus.foundation.ktor.crypto.PasswordCryptoService
import ai.dokus.foundation.ktor.database.dbQuery
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toStdlibInstant
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

/**
 * Helper function to convert kotlinx.datetime.LocalDateTime to kotlinx.datetime.Instant
 */
@OptIn(ExperimentalTime::class)
private fun LocalDateTime.toKotlinxInstant(): Instant {
    val kotlinTimeInstant = toInstant(TimeZone.UTC)
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

    /**
     * Register a new user without associating them with any organization.
     * The user can later create or join organizations.
     */
    suspend fun register(
        email: String,
        password: String,
        firstName: String?,
        lastName: String?
    ): User = dbQuery {
        // Check if email already exists
        val existing = UsersTable
            .selectAll()
            .where { UsersTable.email eq email }
            .singleOrNull()

        if (existing != null) {
            throw IllegalArgumentException("User with email $email already exists")
        }

        val passwordHash = passwordCrypto.hashPassword(Password(password))

        // Create user without organization membership
        val userId = UsersTable.insertAndGetId {
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.firstName] = firstName
            it[UsersTable.lastName] = lastName
            it[UsersTable.isActive] = true
        }.value

        logger.info("Registered new user: $userId with email: $email (no organization)")

        UsersTable
            .selectAll()
            .where { UsersTable.id eq userId }
            .single()
            .toUser()
    }

    /**
     * Register a new user and add them to an organization with a role.
     * Used when inviting users to existing organizations.
     */
    suspend fun registerWithOrganization(
        organizationId: OrganizationId,
        email: String,
        password: String,
        firstName: String?,
        lastName: String?,
        role: UserRole
    ): User = dbQuery {
        // Check if email already exists
        val existing = UsersTable
            .selectAll()
            .where { UsersTable.email eq email }
            .singleOrNull()

        if (existing != null) {
            throw IllegalArgumentException("User with email $email already exists")
        }

        val passwordHash = passwordCrypto.hashPassword(Password(password))

        // Create user
        val userId = UsersTable.insertAndGetId {
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.firstName] = firstName
            it[UsersTable.lastName] = lastName
            it[UsersTable.isActive] = true
        }.value

        // Create membership
        OrganizationMembersTable.insert {
            it[OrganizationMembersTable.userId] = userId
            it[OrganizationMembersTable.organizationId] = organizationId.value.toJavaUuid()
            it[OrganizationMembersTable.role] = role
            it[OrganizationMembersTable.isActive] = true
        }

        logger.info("Registered new user: $userId with email: $email for organization: $organizationId")

        UsersTable
            .selectAll()
            .where { UsersTable.id eq userId }
            .single()
            .toUser()
    }

    suspend fun findById(id: UserId): User? = dbQuery {
        val javaUuid = id.value.toJavaUuid()
        UsersTable
            .selectAll()
            .where { UsersTable.id eq javaUuid }
            .singleOrNull()
            ?.toUser()
    }

    suspend fun findByEmail(email: String): User? = dbQuery {
        UsersTable
            .selectAll()
            .where { UsersTable.email eq email }
            .singleOrNull()
            ?.toUser()
    }

    /**
     * List all users in an organization.
     */
    suspend fun listByOrganization(
        organizationId: OrganizationId,
        activeOnly: Boolean
    ): List<UserInOrganization> =
        dbQuery {
            val javaUuid = organizationId.value.toJavaUuid()

            val query = if (activeOnly) {
                UsersTable
                    .innerJoin(OrganizationMembersTable)
                    .selectAll()
                    .where {
                        (OrganizationMembersTable.organizationId eq javaUuid) and
                                (UsersTable.isActive eq true) and
                                (OrganizationMembersTable.isActive eq true)
                    }
            } else {
                UsersTable
                    .innerJoin(OrganizationMembersTable)
                    .selectAll()
                    .where { OrganizationMembersTable.organizationId eq javaUuid }
            }

            query.map { it.toUserInOrganization() }
        }

    /**
     * Get all organizations a user belongs to.
     */
    suspend fun getUserOrganizations(userId: UserId): List<OrganizationMembership> = dbQuery {
        val javaUuid = userId.value.toJavaUuid()
        OrganizationMembersTable
            .selectAll()
            .where { OrganizationMembersTable.userId eq javaUuid }
            .map { it.toOrganizationMembership() }
    }

    /**
     * Get user's membership in a specific organization.
     */
    suspend fun getMembership(
        userId: UserId,
        organizationId: OrganizationId
    ): OrganizationMembership? = dbQuery {
        OrganizationMembersTable
            .selectAll()
            .where {
                (OrganizationMembersTable.userId eq userId.value.toJavaUuid()) and
                        (OrganizationMembersTable.organizationId eq organizationId.value.toJavaUuid())
            }
            .singleOrNull()
            ?.toOrganizationMembership()
    }

    /**
     * Add a user to an organization with a role.
     */
    suspend fun addToOrganization(userId: UserId, organizationId: OrganizationId, role: UserRole) =
        dbQuery {
            OrganizationMembersTable.insert {
                it[OrganizationMembersTable.userId] = userId.value.toJavaUuid()
                it[OrganizationMembersTable.organizationId] = organizationId.value.toJavaUuid()
                it[OrganizationMembersTable.role] = role
                it[OrganizationMembersTable.isActive] = true
            }
            logger.info("Added user $userId to organization $organizationId with role $role")
        }

    /**
     * Update a user's role in an organization.
     */
    suspend fun updateRole(userId: UserId, organizationId: OrganizationId, newRole: UserRole) =
        dbQuery {
            val updated = OrganizationMembersTable.update({
                (OrganizationMembersTable.userId eq userId.value.toJavaUuid()) and
                        (OrganizationMembersTable.organizationId eq organizationId.value.toJavaUuid())
            }) {
                it[role] = newRole
            }

            if (updated == 0) {
                throw IllegalArgumentException("Membership not found for user $userId in organization $organizationId")
            }

            logger.info("Updated role for user $userId in organization $organizationId to $newRole")
        }

    /**
     * Remove a user from an organization (deactivate membership).
     */
    suspend fun removeFromOrganization(userId: UserId, organizationId: OrganizationId) = dbQuery {
        val updated = OrganizationMembersTable.update({
            (OrganizationMembersTable.userId eq userId.value.toJavaUuid()) and
                    (OrganizationMembersTable.organizationId eq organizationId.value.toJavaUuid())
        }) {
            it[isActive] = false
        }

        if (updated == 0) {
            throw IllegalArgumentException("Membership not found for user $userId in organization $organizationId")
        }

        logger.info("Removed user $userId from organization $organizationId")
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

    @OptIn(ExperimentalTime::class)
    suspend fun recordLogin(userId: UserId, loginTime: Instant) = dbQuery {
        val javaUuid = userId.value.toJavaUuid()
        UsersTable.update({ UsersTable.id eq javaUuid }) {
            it[lastLoginAt] = loginTime.toStdlibInstant().toLocalDateTime(TimeZone.UTC)
        }

        logger.debug("Recorded login for user {} at {}", userId, loginTime)
    }

    suspend fun verifyCredentials(email: String, password: String): User? =
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

            user.toUser()
        }

    // Email verification methods

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

        logger.debug("Set email verification token for user {}", userId)
    }

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