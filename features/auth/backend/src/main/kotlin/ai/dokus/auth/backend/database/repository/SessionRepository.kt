package ai.dokus.auth.backend.database.repository

import ai.dokus.auth.domain.model.SessionRevokeReason
import ai.dokus.foundation.ktor.db.dbQuery
import ai.dokus.auth.backend.database.entity.UserSession
import ai.dokus.auth.backend.database.tables.UserSessionsTable
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import java.util.*

class SessionRepository {

    suspend fun findById(sessionId: UUID): UserSession? = dbQuery {
        UserSessionsTable
            .selectAll()
            .where { UserSessionsTable.id eq sessionId }
            .mapNotNull { it.toUserSession() }
            .singleOrNull()
    }

    suspend fun findByToken(sessionToken: String): UserSession? = dbQuery {
        UserSessionsTable
            .selectAll()
            .where { UserSessionsTable.sessionToken eq sessionToken }
            .mapNotNull { it.toUserSession() }
            .singleOrNull()
    }

    suspend fun findActiveByToken(sessionToken: String): UserSession? = dbQuery {
        val now = Clock.System.now()
        UserSessionsTable
            .selectAll()
            .where {
                (UserSessionsTable.sessionToken eq sessionToken) and
                        (UserSessionsTable.revokedAt.isNull()) and
                        (UserSessionsTable.expiresAt greater now)
            }
            .mapNotNull { it.toUserSession() }
            .singleOrNull()
    }

    suspend fun findByRefreshToken(refreshToken: String): UserSession? = dbQuery {
        UserSessionsTable
            .selectAll()
            .where {
                (UserSessionsTable.refreshToken eq refreshToken) and
                        (UserSessionsTable.revokedAt.isNull())
            }
            .mapNotNull { it.toUserSession() }
            .singleOrNull()
    }

    suspend fun findActiveByUserId(userId: UUID): List<UserSession> = dbQuery {
        val now = Clock.System.now()
        UserSessionsTable
            .selectAll()
            .where {
                (UserSessionsTable.userId eq userId) and
                        (UserSessionsTable.revokedAt.isNull()) and
                        (UserSessionsTable.expiresAt greater now)
            }
            .sortedByDescending { it[UserSessionsTable.lastActivityAt] }
            .map { it.toUserSession() }
    }

    suspend fun countActiveSessions(userId: UUID): Int = dbQuery {
        val now = Clock.System.now()
        UserSessionsTable
            .selectAll()
            .where {
                (UserSessionsTable.userId eq userId) and
                        (UserSessionsTable.revokedAt.isNull()) and
                        (UserSessionsTable.expiresAt greater now)
            }
            .count().toInt()
    }

    suspend fun create(session: UserSession): UserSession = dbQuery {
        val sessionId = UUID.randomUUID()
        val now = Clock.System.now()

        UserSessionsTable.insert {
            it[id] = sessionId
            it[userId] = session.userId
            it[sessionToken] = session.sessionToken
            it[refreshToken] = session.refreshToken
            it[ipAddress] = session.ipAddress
            it[userAgent] = session.userAgent
            it[deviceId] = session.deviceId
            it[deviceType] = session.deviceType
            it[location] = session.location
            it[createdAt] = now
            it[expiresAt] = session.expiresAt
            it[lastActivityAt] = session.lastActivityAt
            it[revokedAt] = session.revokedAt
            it[revokedReason] = session.revokedReason
        }

        session.copy(id = sessionId, createdAt = now)
    }

    suspend fun revokeSession(
        sessionToken: String,
        reason: SessionRevokeReason,
        revokedByUserId: UUID? = null
    ): Unit = dbQuery {
        val now = Clock.System.now()
        UserSessionsTable.update({ UserSessionsTable.sessionToken eq sessionToken }) {
            it[revokedAt] = now
            it[revokedReason] = reason
            it[revokedBy] = revokedByUserId
        }
    }

    suspend fun revokeAllUserSessions(
        userId: UUID,
        reason: SessionRevokeReason,
        revokedByUserId: UUID? = null
    ): Int = dbQuery {
        val now = Clock.System.now()
        UserSessionsTable.update({
            (UserSessionsTable.userId eq userId) and
                    (UserSessionsTable.revokedAt.isNull())
        }) {
            it[revokedAt] = now
            it[revokedReason] = reason
            it[revokedBy] = revokedByUserId
        }
    }

    suspend fun updateLastActivity(sessionToken: String): Unit = dbQuery {
        val now = Clock.System.now()
        UserSessionsTable.update({ UserSessionsTable.sessionToken eq sessionToken }) {
            it[lastActivityAt] = now
        }
    }

    suspend fun updateRefreshToken(sessionId: UUID, refreshToken: String): Boolean = dbQuery {
        UserSessionsTable.update({ UserSessionsTable.id eq sessionId }) {
            it[UserSessionsTable.refreshToken] = refreshToken
        } > 0
    }

    suspend fun deleteExpiredSessions(): Int = dbQuery {
        val now = Clock.System.now()
        UserSessionsTable.deleteWhere { expiresAt less now }
    }

    private fun ResultRow.toUserSession(): UserSession = UserSession(
        id = this[UserSessionsTable.id].value,
        userId = this[UserSessionsTable.userId].value,
        sessionToken = this[UserSessionsTable.sessionToken],
        refreshToken = this[UserSessionsTable.refreshToken],
        ipAddress = this[UserSessionsTable.ipAddress],
        userAgent = this[UserSessionsTable.userAgent],
        deviceId = this[UserSessionsTable.deviceId],
        deviceType = this[UserSessionsTable.deviceType],
        location = this[UserSessionsTable.location],
        createdAt = this[UserSessionsTable.createdAt],
        expiresAt = this[UserSessionsTable.expiresAt],
        lastActivityAt = this[UserSessionsTable.lastActivityAt],
        revokedAt = this[UserSessionsTable.revokedAt],
        revokedReason = this[UserSessionsTable.revokedReason],
        revokedBy = this[UserSessionsTable.revokedBy]?.value
    )
}