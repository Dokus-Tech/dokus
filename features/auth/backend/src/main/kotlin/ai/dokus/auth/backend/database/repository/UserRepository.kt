package ai.dokus.auth.backend.database.repository

import ai.dokus.auth.backend.database.entity.User
import ai.dokus.auth.backend.database.tables.UserPermissionsTable
import ai.dokus.auth.backend.database.tables.UserRolesTable
import ai.dokus.auth.backend.database.tables.UserSpecializationsTable
import ai.dokus.auth.backend.database.tables.UsersTable
import ai.dokus.auth.domain.model.Email
import ai.dokus.auth.domain.model.UserRole
import ai.dokus.auth.domain.model.UserSearchCriteria
import ai.dokus.auth.domain.model.UserStatus
import ai.dokus.foundation.ktor.db.dbQuery
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.util.UUID

class UserRepository {
    private val logger = LoggerFactory.getLogger(UserRepository::class.java)

    suspend fun findById(id: UUID): User? = dbQuery {
        UsersTable
            .selectAll().where { UsersTable.id eq id }
            .mapNotNull { it.toUser() }
            .singleOrNull()
    }

    suspend fun findByMatricule(matricule: String): User? = dbQuery {
        UsersTable
            .selectAll().where { UsersTable.matricule eq matricule }
            .mapNotNull { it.toUser() }
            .singleOrNull()
    }

    suspend fun findByEmail(email: String): User? = dbQuery {
        UsersTable
            .selectAll().where { UsersTable.email.lowerCase() eq email.lowercase() }
            .mapNotNull { it.toUser() }
            .singleOrNull()
    }

    suspend fun findByMatriculeOrEmail(identifier: String): User? = dbQuery {
        UsersTable
            .selectAll().where {
                (UsersTable.matricule.lowerCase() eq identifier.lowercase()) or
                        (UsersTable.email.lowerCase() eq identifier.lowercase())
            }
            .mapNotNull { it.toUser() }
            .singleOrNull()
    }

    suspend fun findActiveByMatricule(matricule: String): User? = dbQuery {
        UsersTable
            .selectAll().where {
                (UsersTable.matricule eq matricule) and
                        (UsersTable.status eq UserStatus.ACTIVE)
            }
            .mapNotNull { it.toUser() }
            .singleOrNull()
    }

    suspend fun findActiveByEmail(email: String): User? = dbQuery {
        UsersTable
            .selectAll().where {
                (UsersTable.email.lowerCase() eq email.lowercase()) and
                        (UsersTable.status eq UserStatus.ACTIVE)
            }
            .mapNotNull { it.toUser() }
            .singleOrNull()
    }

    suspend fun findActiveUsers(offset: Int, limit: Int, excludeUserId: UUID?): List<User> = dbQuery {
        UsersTable
            .selectAll()
            .where {
                val visibilityCondition = UsersTable.status eq UserStatus.ACTIVE
                if (excludeUserId != null) {
                    visibilityCondition and (UsersTable.id neq excludeUserId)
                } else {
                    visibilityCondition
                }
            }
            .limit(limit)
            .offset(offset.toLong())
            .mapNotNull { it.toUser() }
    }

    suspend fun getVisibleUsers(offset: Int, limit: Int, excludeUserId: UUID?): List<User> = dbQuery {
        UsersTable
            .selectAll()
            .where {
                val visibilityCondition = UsersTable.status inList User.Permissions.visibleToUsers
                if (excludeUserId != null) {
                    visibilityCondition and (UsersTable.id neq excludeUserId)
                } else {
                    visibilityCondition
                }
            }
            .limit(limit)
            .offset(offset.toLong())
            .mapNotNull { it.toUser() }
    }

    suspend fun existsByMatricule(matricule: String): Boolean = dbQuery {
        UsersTable
            .selectAll().where { UsersTable.matricule eq matricule }
            .count() > 0
    }

    suspend fun existsByEmail(email: Email): Boolean = dbQuery {
        UsersTable
            .selectAll().where { UsersTable.email.lowerCase() eq email.value.lowercase() }
            .count() > 0
    }

    suspend fun existsByMatriculeAndNotId(matricule: String, excludeId: UUID): Boolean = dbQuery {
        UsersTable
            .selectAll().where {
                (UsersTable.matricule eq matricule) and
                        (UsersTable.id neq excludeId)
            }
            .count() > 0
    }

    suspend fun existsByEmailAndNotId(email: String, excludeId: UUID): Boolean = dbQuery {
        UsersTable
            .selectAll().where {
                (UsersTable.email.lowerCase() eq email.lowercase()) and
                        (UsersTable.id neq excludeId)
            }
            .count() > 0
    }

    suspend fun findByUnitCode(unitCode: String): List<User> = dbQuery {
        UsersTable
            .selectAll().where {
                (UsersTable.unitCode eq unitCode) and
                        (UsersTable.status eq UserStatus.ACTIVE)
            }
            .mapNotNull { it.toUser() }
    }

    suspend fun findByDepartment(department: String): List<User> = dbQuery {
        UsersTable
            .selectAll().where {
                (UsersTable.department eq department) and
                        (UsersTable.status eq UserStatus.ACTIVE)
            }
            .mapNotNull { it.toUser() }
    }

    suspend fun findByRole(role: UserRole): List<User> = dbQuery {
        (UsersTable innerJoin UserRolesTable)
            .selectAll().where {
                (UserRolesTable.role eq role) and
                        (UsersTable.status eq UserStatus.ACTIVE)
            }
            .mapNotNull { it.toUser() }
    }

    suspend fun create(user: User): User = dbQuery {
        val userId = UUID.randomUUID()
        val now = Clock.System.now()

        UsersTable.insert {
            it[id] = userId
            it[firstName] = user.firstName
            it[lastName] = user.lastName
            it[matricule] = user.matricule
            it[email] = user.email
            it[mobilePhone] = user.mobilePhone
            it[officePhone] = user.officePhone
            it[passwordHash] = user.passwordHash
            it[lastPasswordChange] = user.lastPasswordChange
            it[passwordExpiresAt] = user.passwordExpiresAt
            it[unitCode] = user.unitCode
            it[department] = user.department
            it[rank] = user.rank
            it[isOGP] = user.isOGP
            it[isOBP] = user.isOBP
            it[clearanceLevel] = user.clearanceLevel
            it[language] = user.language
            it[radioCallSign] = user.radioCallSign
            it[status] = user.status
            it[lockedUntil] = user.lockedUntil
            it[failedLoginAttempts] = user.failedLoginAttempts
            it[lastLoginAt] = user.lastLoginAt
            it[lastActivityAt] = user.lastActivityAt
            it[galopId] = user.galopId
            it[astridId] = user.astridId
            it[adUsername] = user.adUsername
            it[createdAt] = now
            it[updatedAt] = now
            it[verifiedBy] = user.verifiedBy
            it[lastModifiedBy] = user.lastModifiedBy
            it[deactivatedAt] = user.deactivatedAt
            it[deactivationReason] = user.deactivationReason
            it[dataRetentionExpiresAt] = user.dataRetentionExpiresAt
            it[lastPrivacyReviewAt] = user.lastPrivacyReviewAt
            it[gdprConsentAt] = user.gdprConsentAt
            it[emergencyContactName] = user.emergencyContactName
            it[emergencyContactPhone] = user.emergencyContactPhone
        }

        user.roles.forEach { role ->
            UserRolesTable.insert {
                it[UserRolesTable.userId] = userId
                it[UserRolesTable.role] = role
            }
        }

        user.permissions.forEach { permission ->
            UserPermissionsTable.insert {
                it[UserPermissionsTable.userId] = userId
                it[UserPermissionsTable.permission] = permission
            }
        }

        user.specializations.forEach { specialization ->
            UserSpecializationsTable.insert {
                it[UserSpecializationsTable.userId] = userId
                it[UserSpecializationsTable.specialization] = specialization
            }
        }

        user.copy(id = userId, createdAt = now, updatedAt = now)
    }

    suspend fun update(user: User): User = dbQuery {
        val now = Clock.System.now()

        UsersTable.update({ UsersTable.id eq user.id }) {
            it[firstName] = user.firstName
            it[lastName] = user.lastName
            it[email] = user.email
            it[mobilePhone] = user.mobilePhone
            it[officePhone] = user.officePhone
            it[passwordHash] = user.passwordHash
            it[lastPasswordChange] = user.lastPasswordChange
            it[passwordExpiresAt] = user.passwordExpiresAt
            it[unitCode] = user.unitCode
            it[department] = user.department
            it[rank] = user.rank
            it[isOGP] = user.isOGP
            it[isOBP] = user.isOBP
            it[clearanceLevel] = user.clearanceLevel
            it[language] = user.language
            it[radioCallSign] = user.radioCallSign
            it[status] = user.status
            it[lockedUntil] = user.lockedUntil
            it[failedLoginAttempts] = user.failedLoginAttempts
            it[lastLoginAt] = user.lastLoginAt
            it[lastActivityAt] = user.lastActivityAt
            it[galopId] = user.galopId
            it[astridId] = user.astridId
            it[adUsername] = user.adUsername
            it[updatedAt] = now
            it[verifiedBy] = user.verifiedBy
            it[lastModifiedBy] = user.lastModifiedBy
            it[deactivatedAt] = user.deactivatedAt
            it[deactivationReason] = user.deactivationReason
            it[dataRetentionExpiresAt] = user.dataRetentionExpiresAt
            it[lastPrivacyReviewAt] = user.lastPrivacyReviewAt
            it[gdprConsentAt] = user.gdprConsentAt
            it[emergencyContactName] = user.emergencyContactName
            it[emergencyContactPhone] = user.emergencyContactPhone
        }

        UserRolesTable.deleteWhere { userId eq user.id }
        user.roles.forEach { role ->
            UserRolesTable.insert {
                it[userId] = user.id
                it[UserRolesTable.role] = role
            }
        }

        UserPermissionsTable.deleteWhere { userId eq user.id }
        user.permissions.forEach { permission ->
            UserPermissionsTable.insert {
                it[userId] = user.id
                it[UserPermissionsTable.permission] = permission
            }
        }

        UserSpecializationsTable.deleteWhere { userId eq user.id }
        user.specializations.forEach { specialization ->
            UserSpecializationsTable.insert {
                it[userId] = user.id
                it[UserSpecializationsTable.specialization] = specialization
            }
        }

        user.copy(updatedAt = now)
    }

    suspend fun updateLoginAttempts(userId: UUID, attempts: Int): Unit = dbQuery {
        UsersTable.update({ UsersTable.id eq userId }) {
            it[failedLoginAttempts] = attempts
        }
    }

    suspend fun updateLastLogin(userId: UUID): Unit = dbQuery {
        val now = Clock.System.now()
        UsersTable.update({ UsersTable.id eq userId }) {
            it[lastLoginAt] = now
            it[lastActivityAt] = now
            it[failedLoginAttempts] = 0
        }
    }

    suspend fun lockUser(userId: UUID, until: Instant): Unit = dbQuery {
        UsersTable.update({ UsersTable.id eq userId }) {
            it[lockedUntil] = until
        }
    }

    suspend fun unlockUser(userId: UUID): Unit = dbQuery {
        UsersTable.update({ UsersTable.id eq userId }) {
            it[lockedUntil] = null
            it[failedLoginAttempts] = 0
        }
    }

    suspend fun delete(id: UUID): Boolean = dbQuery {
        UsersTable.deleteWhere { UsersTable.id eq id } > 0
    }

    suspend fun search(criteria: UserSearchCriteria): List<User> = dbQuery {
        var query = UsersTable.selectAll()

        criteria.searchTerm?.let { term ->
            query = query.andWhere {
                (UsersTable.firstName.lowerCase() like "%${term.lowercase()}%") or
                        (UsersTable.lastName.lowerCase() like "%${term.lowercase()}%") or
                        (UsersTable.email.lowerCase() like "%${term.lowercase()}%") or
                        (UsersTable.matricule.lowerCase() like "%${term.lowercase()}%")
            }
        }

        criteria.unitCode?.let { unit ->
            query = query.andWhere { UsersTable.unitCode eq unit }
        }

        criteria.department?.let { dept ->
            query = query.andWhere { UsersTable.department eq dept }
        }

        criteria.status?.let { status ->
            query = query.andWhere { UsersTable.status eq UserStatus.valueOf(status) }
        }

        query
            .limit(criteria.limit ?: 50).offset((criteria.offset ?: 0).toLong())
            .mapNotNull { it.toUser() }
    }

    private fun ResultRow.toUser(): User {
        val userId = this[UsersTable.id].value

        val roles = UserRolesTable
            .selectAll().where { UserRolesTable.userId eq userId }
            .map { it[UserRolesTable.role] }
            .toSet()

        val permissions = UserPermissionsTable
            .selectAll().where { UserPermissionsTable.userId eq userId }
            .map { it[UserPermissionsTable.permission] }
            .toSet()

        val specializations = UserSpecializationsTable
            .selectAll().where { UserSpecializationsTable.userId eq userId }
            .map { it[UserSpecializationsTable.specialization] }
            .toSet()

        return User(
            id = userId,
            firstName = this[UsersTable.firstName],
            lastName = this[UsersTable.lastName],
            matricule = this[UsersTable.matricule],
            email = this[UsersTable.email],
            mobilePhone = this[UsersTable.mobilePhone],
            officePhone = this[UsersTable.officePhone],
            passwordHash = this[UsersTable.passwordHash],
            lastPasswordChange = this[UsersTable.lastPasswordChange],
            passwordExpiresAt = this[UsersTable.passwordExpiresAt],
            unitCode = this[UsersTable.unitCode],
            department = this[UsersTable.department],
            rank = this[UsersTable.rank],
            isOGP = this[UsersTable.isOGP],
            isOBP = this[UsersTable.isOBP],
            clearanceLevel = this[UsersTable.clearanceLevel],
            language = this[UsersTable.language],
            radioCallSign = this[UsersTable.radioCallSign],
            roles = roles,
            permissions = permissions,
            specializations = specializations,
            status = this[UsersTable.status],
            lockedUntil = this[UsersTable.lockedUntil],
            failedLoginAttempts = this[UsersTable.failedLoginAttempts],
            lastLoginAt = this[UsersTable.lastLoginAt],
            lastActivityAt = this[UsersTable.lastActivityAt],
            galopId = this[UsersTable.galopId],
            astridId = this[UsersTable.astridId],
            adUsername = this[UsersTable.adUsername],
            createdAt = this[UsersTable.createdAt],
            updatedAt = this[UsersTable.updatedAt],
            verifiedBy = this[UsersTable.verifiedBy],
            lastModifiedBy = this[UsersTable.lastModifiedBy],
            deactivatedAt = this[UsersTable.deactivatedAt],
            deactivationReason = this[UsersTable.deactivationReason],
            dataRetentionExpiresAt = this[UsersTable.dataRetentionExpiresAt],
            lastPrivacyReviewAt = this[UsersTable.lastPrivacyReviewAt],
            gdprConsentAt = this[UsersTable.gdprConsentAt],
            emergencyContactName = this[UsersTable.emergencyContactName],
            emergencyContactPhone = this[UsersTable.emergencyContactPhone]
        )
    }
}