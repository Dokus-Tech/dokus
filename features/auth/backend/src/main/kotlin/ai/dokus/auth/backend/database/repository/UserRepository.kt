package ai.dokus.auth.backend.database.repository

import ai.dokus.auth.backend.database.mappers.FinancialMappers.toBusinessUser
import ai.dokus.auth.backend.database.tables.UsersTable
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.UserId
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.model.BusinessUser
import ai.dokus.foundation.ktor.crypto.PasswordCryptoService
import ai.dokus.foundation.ktor.database.dbQuery
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
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

@OptIn(ExperimentalUuidApi::class)
class UserRepository(
    private val passwordCrypto: PasswordCryptoService
) {
    private val logger = LoggerFactory.getLogger(UserRepository::class.java)

    suspend fun register(
        tenantId: TenantId,
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
            it[UsersTable.tenantId] = tenantId.value.toJavaUuid()
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.role] = role
            it[UsersTable.firstName] = firstName
            it[UsersTable.lastName] = lastName
            it[UsersTable.isActive] = true
        }.value

        logger.info("Registered new user: $userId with email: $email for tenant: $tenantId")

        UsersTable
            .selectAll()
            .where { UsersTable.id eq userId }
            .single()
            .toBusinessUser()
    }

    suspend fun findById(id: UserId): BusinessUser? = dbQuery {
        val javaUuid = UUID.fromString(id.value)
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

    suspend fun listByTenant(tenantId: TenantId, activeOnly: Boolean): List<BusinessUser> =
        dbQuery {
            val javaUuid = tenantId.value.toJavaUuid()

            val query = if (activeOnly) {
                UsersTable.selectAll().where {
                    (UsersTable.tenantId eq javaUuid) and (UsersTable.isActive eq true)
                }
            } else {
                UsersTable.selectAll().where { UsersTable.tenantId eq javaUuid }
            }

            query.map { it.toBusinessUser() }
        }

    suspend fun updateRole(userId: UserId, newRole: UserRole) = dbQuery {
        val javaUuid = UUID.fromString(userId.value)
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
            val javaUuid = UUID.fromString(userId.value)
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
        val javaUuid = UUID.fromString(userId.value)
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
        val javaUuid = UUID.fromString(userId.value)
        val updated = UsersTable.update({ UsersTable.id eq javaUuid }) {
            it[isActive] = true
        }

        if (updated == 0) {
            throw IllegalArgumentException("User not found: $userId")
        }

        logger.info("Reactivated user $userId")
    }

    suspend fun updatePassword(userId: UserId, newPassword: String) = dbQuery {
        val javaUuid = UUID.fromString(userId.value)
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
        val javaUuid = UUID.fromString(userId.value)
        UsersTable.update({ UsersTable.id eq javaUuid }) {
            it[lastLoginAt] = loginTime.toLocalDateTime(TimeZone.UTC)
        }

        logger.debug("Recorded login for user $userId at $loginTime")
    }

    suspend fun setupMfa(userId: UserId, mfaSecret: String) = dbQuery {
        val javaUuid = UUID.fromString(userId.value)
        val updated = UsersTable.update({ UsersTable.id eq javaUuid }) {
            it[UsersTable.mfaSecret] = mfaSecret
        }

        if (updated == 0) {
            throw IllegalArgumentException("User not found: $userId")
        }

        logger.info("Set up MFA for user $userId")
    }

    suspend fun removeMfa(userId: UserId) = dbQuery {
        val javaUuid = UUID.fromString(userId.value)
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
}