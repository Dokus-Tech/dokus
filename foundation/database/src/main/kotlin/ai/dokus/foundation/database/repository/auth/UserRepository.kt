package ai.dokus.foundation.database.repository.auth

import ai.dokus.foundation.database.mappers.auth.UserMappers.toUser
import ai.dokus.foundation.database.mappers.auth.UserMappers.toTenantMembership
import ai.dokus.foundation.database.mappers.auth.UserMappers.toUserInTenant
import ai.dokus.foundation.database.tables.auth.TenantMembersTable
import ai.dokus.foundation.database.tables.auth.UsersTable
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.User
import ai.dokus.foundation.domain.model.TenantMembership
import ai.dokus.foundation.domain.model.UserInTenant
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
     * Register a new user without associating them with any tenant.
     * The user can later create or join tenants.
     */
    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String
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

        // Create user without tenant membership
        val userId = UsersTable.insertAndGetId {
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.firstName] = firstName
            it[UsersTable.lastName] = lastName
            it[UsersTable.isActive] = true
        }.value

        logger.info("Registered new user: $userId with email: $email (no tenant)")

        UsersTable
            .selectAll()
            .where { UsersTable.id eq userId }
            .single()
            .toUser()
    }

    /**
     * Register a new user and add them to a tenant with a role.
     * Used when inviting users to existing tenants.
     */
    suspend fun registerWithTenant(
        tenantId: TenantId,
        email: String,
        password: String,
        firstName: String,
        lastName: String,
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
        TenantMembersTable.insert {
            it[TenantMembersTable.userId] = userId
            it[TenantMembersTable.tenantId] = tenantId.value.toJavaUuid()
            it[TenantMembersTable.role] = role
            it[TenantMembersTable.isActive] = true
        }

        logger.info("Registered new user: $userId with email: $email for tenant: $tenantId")

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
     * List all users in a tenant.
     */
    suspend fun listByTenant(
        tenantId: TenantId,
        activeOnly: Boolean
    ): List<UserInTenant> =
        dbQuery {
            val javaUuid = tenantId.value.toJavaUuid()

            val query = if (activeOnly) {
                UsersTable
                    .innerJoin(TenantMembersTable)
                    .selectAll()
                    .where {
                        (TenantMembersTable.tenantId eq javaUuid) and
                                (UsersTable.isActive eq true) and
                                (TenantMembersTable.isActive eq true)
                    }
            } else {
                UsersTable
                    .innerJoin(TenantMembersTable)
                    .selectAll()
                    .where { TenantMembersTable.tenantId eq javaUuid }
            }

            query.map { it.toUserInTenant() }
        }

    /**
     * Get all tenants a user belongs to.
     */
    suspend fun getUserTenants(userId: UserId): List<TenantMembership> = dbQuery {
        val javaUuid = userId.value.toJavaUuid()
        TenantMembersTable
            .selectAll()
            .where { TenantMembersTable.userId eq javaUuid }
            .map { it.toTenantMembership() }
    }

    /**
     * Get user's membership in a specific tenant.
     */
    suspend fun getMembership(
        userId: UserId,
        tenantId: TenantId
    ): TenantMembership? = dbQuery {
        TenantMembersTable
            .selectAll()
            .where {
                (TenantMembersTable.userId eq userId.value.toJavaUuid()) and
                        (TenantMembersTable.tenantId eq tenantId.value.toJavaUuid())
            }
            .singleOrNull()
            ?.toTenantMembership()
    }

    /**
     * Add a user to a tenant with a role.
     */
    suspend fun addToTenant(userId: UserId, tenantId: TenantId, role: UserRole) =
        dbQuery {
            TenantMembersTable.insert {
                it[TenantMembersTable.userId] = userId.value.toJavaUuid()
                it[TenantMembersTable.tenantId] = tenantId.value.toJavaUuid()
                it[TenantMembersTable.role] = role
                it[TenantMembersTable.isActive] = true
            }
            logger.info("Added user $userId to tenant $tenantId with role $role")
        }

    /**
     * Update a user's role in a tenant.
     */
    suspend fun updateRole(userId: UserId, tenantId: TenantId, newRole: UserRole) =
        dbQuery {
            val updated = TenantMembersTable.update({
                (TenantMembersTable.userId eq userId.value.toJavaUuid()) and
                        (TenantMembersTable.tenantId eq tenantId.value.toJavaUuid())
            }) {
                it[role] = newRole
            }

            if (updated == 0) {
                throw IllegalArgumentException("Membership not found for user $userId in tenant $tenantId")
            }

            logger.info("Updated role for user $userId in tenant $tenantId to $newRole")
        }

    /**
     * Remove a user from a tenant (deactivate membership).
     */
    suspend fun removeFromTenant(userId: UserId, tenantId: TenantId) = dbQuery {
        val updated = TenantMembersTable.update({
            (TenantMembersTable.userId eq userId.value.toJavaUuid()) and
                    (TenantMembersTable.tenantId eq tenantId.value.toJavaUuid())
        }) {
            it[isActive] = false
        }

        if (updated == 0) {
            throw IllegalArgumentException("Membership not found for user $userId in tenant $tenantId")
        }

        logger.info("Removed user $userId from tenant $tenantId")
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
