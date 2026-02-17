
package tech.dokus.database.repository.auth

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.auth.RefreshTokensTable
import tech.dokus.database.utils.toKotlinxInstant
import tech.dokus.domain.DeviceType
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.auth.SessionDto
import tech.dokus.foundation.backend.database.dbQuery
import tech.dokus.foundation.backend.database.now
import tech.dokus.foundation.backend.utils.loggerFor
import java.security.MessageDigest
import kotlin.uuid.Uuid

/**
 * Information about a refresh token/session entry.
 */
data class RefreshTokenInfo(
    val tokenId: String,
    val sessionId: SessionId,
    val createdAt: Instant,
    val expiresAt: Instant,
    val isRevoked: Boolean,
    val accessTokenJti: String? = null,
    val accessTokenExpiresAt: Instant? = null,
    val deviceType: DeviceType = DeviceType.Desktop,
    val ipAddress: String? = null,
    val userAgent: String? = null,
)

/**
 * Revocation metadata for a session.
 */
data class RevokedSessionInfo(
    val sessionId: SessionId,
    val accessTokenJti: String?,
    val accessTokenExpiresAt: Instant?
)

/**
 * Repository for managing JWT refresh tokens with persistence, rotation, and revocation.
 */
class RefreshTokenRepository {
    private val logger = loggerFor()

    /**
     * Save a refresh token to the database.
     */
    @Suppress("LongParameterList")
    suspend fun saveRefreshToken(
        userId: UserId,
        token: String,
        expiresAt: Instant,
        accessTokenJti: String? = null,
        accessTokenExpiresAt: Instant? = null,
        deviceType: DeviceType = DeviceType.Desktop,
        ipAddress: String? = null,
        userAgent: String? = null
    ): Result<Unit> = runCatching {
        dbQuery {
            val userUuid = userId.uuid
            val tokenHash = tokenHash(token)

            RefreshTokensTable.insert {
                it[RefreshTokensTable.userId] = userUuid
                it[RefreshTokensTable.tokenHash] = tokenHash
                it[RefreshTokensTable.expiresAt] = expiresAt.toLocalDateTime(TimeZone.UTC)
                it[RefreshTokensTable.isRevoked] = false
                it[RefreshTokensTable.accessTokenJti] = accessTokenJti
                it[RefreshTokensTable.accessTokenExpiresAt] = accessTokenExpiresAt?.toLocalDateTime(TimeZone.UTC)
                it[RefreshTokensTable.deviceType] = deviceType
                it[RefreshTokensTable.ipAddress] = ipAddress
                it[RefreshTokensTable.userAgent] = userAgent?.take(512)
            }

            logger.debug(
                "Saved refresh token for user: {}, token hash: {}, expires: {}",
                userId.value,
                tokenHash.take(8),
                expiresAt
            )
        }
    }.onFailure { error ->
        logger.error("Failed to save refresh token for user: ${userId.value}", error)
    }

    /**
     * Validate a refresh token and rotate it to a new one.
     */
    suspend fun validateAndRotate(oldToken: String): Result<UserId> = runCatching {
        dbQuery {
            val oldTokenHash = tokenHash(oldToken)

            val tokenRow = RefreshTokensTable
                .selectAll()
                .where { RefreshTokensTable.tokenHash eq oldTokenHash }
                .singleOrNull()
                ?: throw IllegalArgumentException("Refresh token not found")

            val tokenId = tokenRow[RefreshTokensTable.id].value
            val userId = tokenRow[RefreshTokensTable.userId].value
            val expiresAt = tokenRow[RefreshTokensTable.expiresAt]
            val isRevoked = tokenRow[RefreshTokensTable.isRevoked]

            if (isRevoked) {
                logger.warn(
                    "Attempt to use revoked token (ID: $tokenId, hash: ${oldTokenHash.take(8)})"
                )
                throw SecurityException("Refresh token has been revoked")
            }

            val now = now()
            val expiresAtInstant = expiresAt.toKotlinxInstant()

            if (now > expiresAtInstant) {
                logger.warn(
                    "Attempt to use expired token (ID: $tokenId, expired: $expiresAt, hash: ${oldTokenHash.take(8)})"
                )
                throw IllegalArgumentException("Refresh token has expired")
            }

            val updated = RefreshTokensTable.update({ RefreshTokensTable.id eq tokenId }) {
                it[RefreshTokensTable.isRevoked] = true
            }

            if (updated == 0) {
                throw IllegalStateException("Failed to revoke old token during rotation")
            }

            logger.info(
                "Validated and rotated refresh token for user: $userId, token ID: $tokenId"
            )

            UserId(Uuid.parse(userId.toString()).toString())
        }
    }.onFailure { error ->
        when (error) {
            is SecurityException, is IllegalArgumentException -> {
                logger.debug("Token validation failed: ${error.message}")
            }

            else -> {
                logger.error("Unexpected error during token validation and rotation", error)
            }
        }
    }

    /**
     * Revoke a specific refresh token.
     */
    suspend fun revokeToken(token: String): Result<Unit> = runCatching {
        dbQuery {
            val tokenHash = tokenHash(token)
            val updated = RefreshTokensTable.update(
                { RefreshTokensTable.tokenHash eq tokenHash }
            ) {
                it[RefreshTokensTable.isRevoked] = true
            }

            if (updated == 0) {
                logger.warn("Attempted to revoke non-existent token (hash: ${tokenHash.take(8)})")
                throw IllegalArgumentException("Refresh token not found")
            }

            logger.info("Revoked refresh token (hash: ${tokenHash.take(8)}, count: $updated)")
        }
    }.onFailure { error ->
        if (error !is IllegalArgumentException) {
            logger.error("Failed to revoke refresh token", error)
        }
    }

    /**
     * Revoke all refresh tokens for a user.
     */
    suspend fun revokeAllUserTokens(userId: UserId): Result<Unit> = runCatching {
        dbQuery {
            val userUuid = userId.uuid

            val updated = RefreshTokensTable.update(
                {
                    (RefreshTokensTable.userId eq userUuid) and
                        (RefreshTokensTable.isRevoked eq false)
                }
            ) {
                it[RefreshTokensTable.isRevoked] = true
            }

            logger.info("Revoked all refresh tokens for user: ${userId.value}, count: $updated")
        }
    }.onFailure { error ->
        logger.error("Failed to revoke all tokens for user: ${userId.value}", error)
    }

    /**
     * Revoke a specific session by its session id for a given user.
     */
    suspend fun revokeSessionById(
        userId: UserId,
        sessionId: SessionId
    ): Result<RevokedSessionInfo?> = runCatching {
        dbQuery {
            val userUuid = userId.uuid
            val now = now().toLocalDateTime(TimeZone.UTC)
            val sessionIdString = sessionId.value.toString()

            val tokenRow = RefreshTokensTable
                .selectAll()
                .where {
                    (RefreshTokensTable.userId eq userUuid) and
                        (RefreshTokensTable.isRevoked eq false) and
                        (RefreshTokensTable.expiresAt greater now) and
                        (
                            (RefreshTokensTable.accessTokenJti eq sessionIdString) or
                                (RefreshTokensTable.id eq sessionId.uuid)
                            )
                }
                .orderBy(RefreshTokensTable.createdAt, SortOrder.DESC)
                .limit(1)
                .singleOrNull()
                ?: return@dbQuery null

            val tokenId = tokenRow[RefreshTokensTable.id].value
            val updated = RefreshTokensTable.update({ RefreshTokensTable.id eq tokenId }) {
                it[RefreshTokensTable.isRevoked] = true
            }

            if (updated == 0) {
                throw IllegalStateException("Failed to revoke session: $sessionId")
            }

            val resolvedSessionId = resolveSessionId(
                accessTokenJti = tokenRow[RefreshTokensTable.accessTokenJti],
                tokenId = tokenId.toString()
            )

            RevokedSessionInfo(
                sessionId = resolvedSessionId,
                accessTokenJti = tokenRow[RefreshTokensTable.accessTokenJti],
                accessTokenExpiresAt = tokenRow[RefreshTokensTable.accessTokenExpiresAt]?.toKotlinxInstant()
            )
        }
    }.onFailure { error ->
        logger.error(
            "Failed to revoke session $sessionId for user ${userId.value}",
            error
        )
    }

    /**
     * Revoke all sessions except current session for a user.
     *
     * If the current session row is identifiable by JTI, legacy rows without JTI are revoked too.
     * If the current session row is not identifiable (older row without JTI), we skip legacy rows
     * to avoid revoking the current session by mistake.
     */
    suspend fun revokeOtherSessions(
        userId: UserId,
        currentSessionJti: String
    ): Result<List<RevokedSessionInfo>> = runCatching {
        dbQuery {
            val userUuid = userId.uuid
            val now = now().toLocalDateTime(TimeZone.UTC)
            val activeRowsFilter = (RefreshTokensTable.userId eq userUuid) and
                (RefreshTokensTable.isRevoked eq false) and
                (RefreshTokensTable.expiresAt greater now)

            val hasCurrentSessionRecord = RefreshTokensTable
                .selectAll()
                .where {
                    activeRowsFilter and
                        (RefreshTokensTable.accessTokenJti eq currentSessionJti)
                }
                .limit(1)
                .any()

            if (!hasCurrentSessionRecord) {
                logger.warn(
                    "Current session row not found for user {} and jti {} - " +
                        "legacy rows will be skipped for safety",
                    userId.value,
                    currentSessionJti
                )
            }

            val revokePredicate = if (hasCurrentSessionRecord) {
                (RefreshTokensTable.accessTokenJti.isNull()) or
                    (RefreshTokensTable.accessTokenJti neq currentSessionJti)
            } else {
                (RefreshTokensTable.accessTokenJti.isNotNull()) and
                    (RefreshTokensTable.accessTokenJti neq currentSessionJti)
            }

            val toRevoke = RefreshTokensTable
                .selectAll()
                .where {
                    activeRowsFilter and revokePredicate
                }
                .toList()

            val revoked = mutableListOf<RevokedSessionInfo>()
            toRevoke.forEach { row ->
                val tokenId = row[RefreshTokensTable.id].value
                val updated = RefreshTokensTable.update({ RefreshTokensTable.id eq tokenId }) {
                    it[RefreshTokensTable.isRevoked] = true
                }

                if (updated > 0) {
                    revoked += RevokedSessionInfo(
                        sessionId = resolveSessionId(
                            accessTokenJti = row[RefreshTokensTable.accessTokenJti],
                            tokenId = tokenId.toString()
                        ),
                        accessTokenJti = row[RefreshTokensTable.accessTokenJti],
                        accessTokenExpiresAt = row[RefreshTokensTable.accessTokenExpiresAt]?.toKotlinxInstant()
                    )
                }
            }

            revoked
        }
    }.onFailure { error ->
        logger.error(
            "Failed to revoke other sessions for user ${userId.value}",
            error
        )
    }

    /**
     * Clean up expired and revoked tokens.
     */
    suspend fun cleanupExpiredTokens(): Result<Int> = runCatching {
        dbQuery {
            val now = now().toLocalDateTime(TimeZone.UTC)

            val deleted = RefreshTokensTable.deleteWhere {
                (expiresAt less now) or (isRevoked eq true)
            }

            logger.info("Cleaned up expired/revoked refresh tokens, count: $deleted")
            deleted
        }
    }.onFailure { error ->
        logger.error("Failed to cleanup expired tokens", error)
    }

    /**
     * Count active sessions for a user.
     */
    suspend fun countActiveForUser(userId: UserId): Int = try {
        dbQuery {
            val userUuid = userId.uuid
            val now = now().toLocalDateTime(TimeZone.UTC)

            RefreshTokensTable
                .selectAll()
                .where {
                    (RefreshTokensTable.userId eq userUuid) and
                        (RefreshTokensTable.isRevoked eq false) and
                        (RefreshTokensTable.expiresAt greater now)
                }
                .count()
                .toInt()
        }
    } catch (error: Exception) {
        logger.error("Failed to count active sessions for user: ${userId.value}", error)
        0
    }

    /**
     * Revoke the oldest active session for a user.
     */
    suspend fun revokeOldestForUser(userId: UserId): Result<Unit> = runCatching {
        dbQuery {
            val userUuid = userId.uuid
            val now = now().toLocalDateTime(TimeZone.UTC)

            val oldestToken = RefreshTokensTable
                .selectAll()
                .where {
                    (RefreshTokensTable.userId eq userUuid) and
                        (RefreshTokensTable.isRevoked eq false) and
                        (RefreshTokensTable.expiresAt greater now)
                }
                .orderBy(RefreshTokensTable.createdAt, SortOrder.ASC)
                .limit(1)
                .singleOrNull()

            if (oldestToken != null) {
                val tokenId = oldestToken[RefreshTokensTable.id].value
                RefreshTokensTable.update({ RefreshTokensTable.id eq tokenId }) {
                    it[RefreshTokensTable.isRevoked] = true
                }
                logger.info("Revoked oldest session for user: ${userId.value}, token ID: $tokenId")
            } else {
                logger.debug("No active sessions to revoke for user: ${userId.value}")
            }
        }
    }.onFailure { error ->
        logger.error("Failed to revoke oldest session for user: ${userId.value}", error)
    }

    /**
     * List active sessions for user with current-session marker.
     */
    suspend fun listActiveSessions(
        userId: UserId,
        currentSessionJti: String?
    ): List<SessionDto> {
        val active = getUserActiveTokens(userId)
        return active.map { token ->
            SessionDto(
                id = token.sessionId,
                ipAddress = token.ipAddress,
                userAgent = token.userAgent,
                deviceId = null,
                deviceType = token.deviceType,
                location = null,
                createdAt = token.createdAt.epochSeconds,
                expiresAt = token.expiresAt.epochSeconds,
                lastActivityAt = token.createdAt.epochSeconds,
                revokedAt = null,
                revokedReason = null,
                revokedBy = null,
                isCurrent = token.accessTokenJti != null && token.accessTokenJti == currentSessionJti,
            )
        }
    }

    /**
     * Get all active tokens for a user.
     */
    suspend fun getUserActiveTokens(userId: UserId): List<RefreshTokenInfo> = try {
        dbQuery {
            val userUuid = userId.uuid
            val now = now().toLocalDateTime(TimeZone.UTC)

            RefreshTokensTable
                .selectAll()
                .where {
                    (RefreshTokensTable.userId eq userUuid) and
                        (RefreshTokensTable.isRevoked eq false) and
                        (RefreshTokensTable.expiresAt greater now)
                }
                .orderBy(RefreshTokensTable.createdAt, SortOrder.DESC)
                .map { row ->
                    val tokenId = row[RefreshTokensTable.id].value.toString()
                    val accessTokenJti = row[RefreshTokensTable.accessTokenJti]
                    RefreshTokenInfo(
                        tokenId = tokenId,
                        sessionId = resolveSessionId(accessTokenJti, tokenId),
                        createdAt = row[RefreshTokensTable.createdAt].toKotlinxInstant(),
                        expiresAt = row[RefreshTokensTable.expiresAt].toKotlinxInstant(),
                        isRevoked = row[RefreshTokensTable.isRevoked],
                        accessTokenJti = accessTokenJti,
                        accessTokenExpiresAt = row[RefreshTokensTable.accessTokenExpiresAt]?.toKotlinxInstant(),
                        deviceType = row[RefreshTokensTable.deviceType],
                        ipAddress = row[RefreshTokensTable.ipAddress],
                        userAgent = row[RefreshTokensTable.userAgent],
                    )
                }
        }
    } catch (error: Exception) {
        logger.error("Failed to get active tokens for user: ${userId.value}", error)
        emptyList()
    }

    private fun resolveSessionId(accessTokenJti: String?, tokenId: String): SessionId {
        val fromJti = accessTokenJti
            ?.let { runCatching { SessionId(it) }.getOrNull() }
        return fromJti ?: SessionId(tokenId)
    }

    private fun tokenHash(token: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(token.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            logger.error("Failed to hash token", e)
            throw e
        }
    }
}
