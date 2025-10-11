package ai.dokus.features.auth.backend.database.repository

import ai.dokus.features.auth.backend.database.entity.LoginResult
import ai.dokus.features.auth.backend.database.entity.UserLoginAttempt
import ai.dokus.features.auth.backend.database.tables.UserLoginAttemptsTable
import ai.dokus.foundation.ktor.db.dbQuery
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

class LoginAttemptRepository {

    suspend fun create(attempt: UserLoginAttempt): UserLoginAttempt = dbQuery {
        val attemptId = UUID.randomUUID()
        val now = Clock.System.now()

        UserLoginAttemptsTable.insert {
            it[id] = attemptId
            it[matricule] = attempt.matricule
            it[email] = attempt.email
            it[userId] = attempt.userId
            it[ipAddress] = attempt.ipAddress
            it[userAgent] = attempt.userAgent
            it[result] = attempt.result
            it[failureReason] = attempt.failureReason
            it[attemptedAt] = now
        }

        attempt.copy(id = attemptId, attemptedAt = now)
    }

    suspend fun findRecentByUsername(
        username: String,
        since: Instant
    ): List<UserLoginAttempt> = dbQuery {
        UserLoginAttemptsTable
            .selectAll()
            .where {
                (UserLoginAttemptsTable.matricule eq username) and
                        (UserLoginAttemptsTable.attemptedAt greater since)
            }
            .orderBy(UserLoginAttemptsTable.attemptedAt, SortOrder.DESC)
            .mapNotNull { it.toLoginAttempt() }
    }

    suspend fun countFailedAttempts(
        username: String,
        since: Instant
    ): Int = dbQuery {
        UserLoginAttemptsTable
            .selectAll()
            .where {
                (UserLoginAttemptsTable.matricule eq username) and
                        (UserLoginAttemptsTable.result neq LoginResult.SUCCESS) and
                        (UserLoginAttemptsTable.attemptedAt greater since)
            }
            .count().toInt()
    }

    suspend fun findRecentByUserId(
        userId: UUID,
        limit: Int = 10
    ): List<UserLoginAttempt> = dbQuery {
        UserLoginAttemptsTable
            .selectAll()
            .where { UserLoginAttemptsTable.userId eq userId }
            .orderBy(UserLoginAttemptsTable.attemptedAt, SortOrder.DESC)
            .limit(limit)
            .mapNotNull { it.toLoginAttempt() }
    }

    suspend fun deleteOldAttempts(before: Instant): Int = dbQuery {
        UserLoginAttemptsTable.deleteWhere {
            attemptedAt less before
        }
    }

    private fun ResultRow.toLoginAttempt(): UserLoginAttempt = UserLoginAttempt(
        id = this[UserLoginAttemptsTable.id].value,
        matricule = this[UserLoginAttemptsTable.matricule],
        userId = this[UserLoginAttemptsTable.userId]?.value,
        ipAddress = this[UserLoginAttemptsTable.ipAddress],
        userAgent = this[UserLoginAttemptsTable.userAgent],
        result = this[UserLoginAttemptsTable.result],
        failureReason = this[UserLoginAttemptsTable.failureReason],
        attemptedAt = this[UserLoginAttemptsTable.attemptedAt]
    )
}