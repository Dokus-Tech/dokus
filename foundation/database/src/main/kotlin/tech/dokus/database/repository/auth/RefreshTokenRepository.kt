@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.database.repository.auth

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
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
import tech.dokus.foundation.backend.utils.runSuspendCatching
import java.security.MessageDigest
import java.util.UUID as JavaUuid
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * Information about an active refresh-token row.
 *
 * [sessionId] is the user-facing/session-management identity. It is stable for
 * modern rows and falls back to the row UUID for legacy rows so revocation stays precise.
 */
data class RefreshTokenInfo(
    val tokenId: String,
    val storedSessionId: SessionId?,
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
 * Successful refresh-token validation result.
 */
data class ValidatedRefreshToken(
    val userId: UserId,
    val sessionId: SessionId,
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
 * Result of a session revocation operation.
 */
sealed class SessionRevocationResult {
    data class Revoked(val sessions: List<RevokedSessionInfo>) : SessionRevocationResult()
    data object NotFound : SessionRevocationResult()
}

private data class ActiveTokenRow(
    val rowId: JavaUuid,
    val token: RefreshTokenInfo,
)

private data class SessionFingerprint(
    val deviceType: DeviceType,
    val userAgent: String?,
    val ipAddress: String?,
) {
    val isStrongEnough: Boolean
        get() = !userAgent.isNullOrBlank() || !ipAddress.isNullOrBlank()
}

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
        sessionId: SessionId? = null,
        accessTokenJti: String? = null,
        accessTokenExpiresAt: Instant? = null,
        deviceType: DeviceType = DeviceType.Desktop,
        ipAddress: String? = null,
        userAgent: String? = null,
        replaceExistingSessionIdentity: SessionId? = null,
    ): Result<Unit> = runSuspendCatching {
        dbQuery {
            val userUuid = userId.uuid.toJavaUuid()
            val tokenHash = tokenHash(token)

            replaceExistingSessionIdentity?.let { currentSessionIdentity ->
                val replaced = RefreshTokensTable.update(
                    {
                        (RefreshTokensTable.userId eq userUuid) and
                            (RefreshTokensTable.isRevoked eq false) and
                            currentSessionPredicate(currentSessionIdentity)
                    }
                ) {
                    it[RefreshTokensTable.isRevoked] = true
                }
                if (replaced > 0) {
                    logger.info(
                        "Replaced {} active refresh-token rows for session {} and user {}",
                        replaced,
                        currentSessionIdentity,
                        userId.value
                    )
                }
            }

            RefreshTokensTable.insert {
                it[RefreshTokensTable.userId] = userUuid
                it[RefreshTokensTable.sessionId] = sessionId?.uuid?.toJavaUuid()
                it[RefreshTokensTable.tokenHash] = tokenHash
                it[RefreshTokensTable.expiresAt] = expiresAt.toLocalDateTime(TimeZone.UTC)
                it[RefreshTokensTable.isRevoked] = false
                it[RefreshTokensTable.accessTokenJti] = accessTokenJti
                it[RefreshTokensTable.accessTokenExpiresAt] =
                    accessTokenExpiresAt?.toLocalDateTime(TimeZone.UTC)
                it[RefreshTokensTable.deviceType] = deviceType
                it[RefreshTokensTable.ipAddress] = ipAddress
                it[RefreshTokensTable.userAgent] = userAgent?.take(512)
            }

            logger.debug(
                "Saved refresh token for user: {}, session: {}, token hash: {}, expires: {}",
                userId.value,
                sessionId ?: "legacy",
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
    suspend fun validateAndRotate(oldToken: String): Result<ValidatedRefreshToken> = runSuspendCatching {
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

            val storedSessionId = tokenRow[RefreshTokensTable.sessionId]?.let { SessionId(it.toString()) }
            ValidatedRefreshToken(
                userId = UserId(userId.toString()),
                sessionId = resolveStableSessionId(
                    storedSessionId = storedSessionId,
                    accessTokenJti = tokenRow[RefreshTokensTable.accessTokenJti],
                    tokenId = tokenId.toString()
                )
            )
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
    suspend fun revokeToken(token: String): Result<Unit> = runSuspendCatching {
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
    suspend fun revokeAllUserTokens(userId: UserId): Result<Unit> = runSuspendCatching {
        dbQuery {
            val userUuid = userId.uuid.toJavaUuid()

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
     * Revoke a specific session for a given user.
     */
    suspend fun revokeSessionById(
        userId: UserId,
        sessionId: SessionId
    ): SessionRevocationResult = try {
        dbQuery {
            val activeRows = getActiveTokenRowsInTx(userId)
            val toRevoke = activeRows.filter { matchesDisplayedSessionId(it.token, sessionId) }
            if (toRevoke.isEmpty()) {
                SessionRevocationResult.NotFound
            } else {
                SessionRevocationResult.Revoked(revokeRows(toRevoke))
            }
        }
    } catch (error: Exception) {
        logger.error(
            "Failed to revoke session $sessionId for user ${userId.value}",
            error
        )
        throw error
    }

    /**
     * Revoke all sessions except current session for a user.
     *
     * If the current session row is not identifiable, legacy rows are skipped for safety.
     */
    suspend fun revokeOtherSessions(
        userId: UserId,
        currentSessionId: SessionId
    ): SessionRevocationResult = try {
        dbQuery {
            val activeRows = getActiveTokenRowsInTx(userId)
            val hasCurrentSessionRecord = activeRows.any {
                matchesCurrentSessionIdentity(it.token, currentSessionId)
            }

            if (!hasCurrentSessionRecord) {
                logger.warn(
                    "Current session row not found for user {} and session {} - legacy rows will be skipped for safety",
                    userId.value,
                    currentSessionId
                )
            }

            val toRevoke = if (hasCurrentSessionRecord) {
                activeRows.filterNot { matchesCurrentSessionIdentity(it.token, currentSessionId) }
            } else {
                activeRows.filter { row ->
                    row.token.storedSessionId != null &&
                        !matchesCurrentSessionIdentity(row.token, currentSessionId)
                }
            }

            SessionRevocationResult.Revoked(revokeRows(toRevoke))
        }
    } catch (error: Exception) {
        logger.error(
            "Failed to revoke other sessions for user ${userId.value}",
            error
        )
        throw error
    }

    /**
     * Revoke the currently authenticated session using stable session identity with legacy fallback.
     */
    suspend fun revokeCurrentSession(
        userId: UserId,
        currentSessionId: SessionId
    ): SessionRevocationResult = try {
        dbQuery {
            val activeRows = getActiveTokenRowsInTx(userId)
            val toRevoke = activeRows.filter {
                matchesCurrentSessionIdentity(it.token, currentSessionId)
            }
            if (toRevoke.isEmpty()) {
                SessionRevocationResult.NotFound
            } else {
                SessionRevocationResult.Revoked(revokeRows(toRevoke))
            }
        }
    } catch (error: Exception) {
        logger.error(
            "Failed to revoke current session {} for user {}",
            currentSessionId,
            userId.value,
            error
        )
        throw error
    }

    /**
     * Clean up expired and revoked tokens.
     */
    suspend fun cleanupExpiredTokens(): Result<Int> = runSuspendCatching {
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
            getDistinctActiveSessionsInTx(userId).size
        }
    } catch (error: Exception) {
        logger.error("Failed to count active sessions for user: ${userId.value}", error)
        0
    }

    /**
     * Revoke the oldest active session for a user.
     */
    suspend fun revokeOldestForUser(userId: UserId): Result<Unit> = runSuspendCatching {
        dbQuery {
            val oldestSession = getDistinctActiveSessionsInTx(userId)
                .minByOrNull { it.createdAt }
                ?: run {
                    logger.debug("No active sessions to revoke for user: ${userId.value}")
                    return@dbQuery
                }

            val activeRows = getActiveTokenRowsInTx(userId)
            val revoked = revokeRows(
                activeRows.filter { matchesDisplayedSessionId(it.token, oldestSession.sessionId) }
            )
            logger.info(
                "Revoked oldest session for user: {}, session: {}, rows: {}",
                userId.value,
                oldestSession.sessionId,
                revoked.size
            )
        }
    }.onFailure { error ->
        logger.error("Failed to revoke oldest session for user: ${userId.value}", error)
    }

    /**
     * List active sessions for user with current-session marker.
     */
    suspend fun listActiveSessions(
        userId: UserId,
        currentSessionId: SessionId?
    ): List<SessionDto> {
        return try {
            dbQuery {
                getDistinctActiveSessionsInTx(userId)
                    .sortedWith(
                        compareByDescending<RefreshTokenInfo> {
                            matchesCurrentSessionIdentity(it, currentSessionId)
                        }.thenByDescending { it.createdAt }
                    )
                    .map { token ->
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
                            isCurrent = matchesCurrentSessionIdentity(token, currentSessionId),
                        )
                    }
            }
        } catch (error: Exception) {
            logger.error("Failed to list active sessions for user: ${userId.value}", error)
            emptyList()
        }
    }

    /**
     * Get all active refresh-token rows for a user.
     */
    suspend fun getUserActiveTokens(userId: UserId): List<RefreshTokenInfo> = try {
        dbQuery {
            getActiveTokenRowsInTx(userId).map { it.token }
        }
    } catch (error: Exception) {
        logger.error("Failed to get active tokens for user: ${userId.value}", error)
        emptyList()
    }

    private fun getDistinctActiveSessionsInTx(userId: UserId): List<RefreshTokenInfo> {
        return getActiveTokenRowsInTx(userId)
            .map { it.token }
            .groupBy { sessionIdentityKey(it) }
            .values
            .map { rows -> rows.maxBy { it.createdAt } }
    }

    private fun getActiveTokenRowsInTx(userId: UserId): List<ActiveTokenRow> {
        val userUuid = userId.uuid.toJavaUuid()
        val currentTime = now()
        val currentTimeLocal = currentTime.toLocalDateTime(TimeZone.UTC)
        val rows = RefreshTokensTable
            .selectAll()
            .where { activeRowsFilter(userUuid, currentTimeLocal) }
            .orderBy(RefreshTokensTable.createdAt, SortOrder.DESC)
            .map(::toActiveTokenRow)

        val supersededLegacyIds = cleanupSupersededLegacyRows(rows, currentTime)
        return rows.filterNot { it.rowId in supersededLegacyIds }
    }

    private fun cleanupSupersededLegacyRows(
        rows: List<ActiveTokenRow>,
        currentTime: Instant
    ): Set<JavaUuid> {
        val stableRowsByFingerprint = rows
            .filter { it.token.storedSessionId != null }
            .groupBy { sessionFingerprint(it.token) }

        val supersededLegacyIds = rows.asSequence()
            .filter { it.token.storedSessionId == null }
            .filter { legacy ->
                val fingerprint = sessionFingerprint(legacy.token)
                fingerprint.isStrongEnough &&
                    (legacy.token.accessTokenExpiresAt?.let { it <= currentTime } == true) &&
                    stableRowsByFingerprint[fingerprint]
                        .orEmpty()
                        .any { stable -> stable.token.createdAt > legacy.token.createdAt }
            }
            .map { it.rowId }
            .toSet()

        if (supersededLegacyIds.isNotEmpty()) {
            RefreshTokensTable.update({ RefreshTokensTable.id inList supersededLegacyIds }) {
                it[RefreshTokensTable.isRevoked] = true
            }
            logger.info(
                "Revoked {} superseded legacy refresh-token rows during active-session cleanup",
                supersededLegacyIds.size
            )
        }

        return supersededLegacyIds
    }

    private fun revokeRows(rows: List<ActiveTokenRow>): List<RevokedSessionInfo> {
        if (rows.isEmpty()) return emptyList()
        val rowIds = rows.map { it.rowId }
        RefreshTokensTable.update({ RefreshTokensTable.id inList rowIds }) {
            it[RefreshTokensTable.isRevoked] = true
        }
        return rows.map { it.token.toRevokedSessionInfo() }
    }

    private fun toActiveTokenRow(row: org.jetbrains.exposed.v1.core.ResultRow): ActiveTokenRow {
        val rowId = row[RefreshTokensTable.id].value
        val tokenId = rowId.toString()
        val storedSessionId = row[RefreshTokensTable.sessionId]?.let { SessionId(it.toString()) }
        val accessTokenJti = row[RefreshTokensTable.accessTokenJti]

        return ActiveTokenRow(
            rowId = rowId,
            token = RefreshTokenInfo(
                tokenId = tokenId,
                storedSessionId = storedSessionId,
                sessionId = displaySessionId(storedSessionId, tokenId),
                createdAt = row[RefreshTokensTable.createdAt].toKotlinxInstant(),
                expiresAt = row[RefreshTokensTable.expiresAt].toKotlinxInstant(),
                isRevoked = row[RefreshTokensTable.isRevoked],
                accessTokenJti = accessTokenJti,
                accessTokenExpiresAt = row[RefreshTokensTable.accessTokenExpiresAt]?.toKotlinxInstant(),
                deviceType = row[RefreshTokensTable.deviceType],
                ipAddress = row[RefreshTokensTable.ipAddress],
                userAgent = row[RefreshTokensTable.userAgent],
            )
        )
    }

    private fun activeRowsFilter(userUuid: JavaUuid, currentTimeLocal: kotlinx.datetime.LocalDateTime) =
        (RefreshTokensTable.userId eq userUuid) and
            (RefreshTokensTable.isRevoked eq false) and
            (RefreshTokensTable.expiresAt greater currentTimeLocal)

    private fun currentSessionPredicate(currentSessionId: SessionId) =
        (RefreshTokensTable.sessionId eq currentSessionId.uuid.toJavaUuid()) or
            (
                RefreshTokensTable.sessionId.isNull() and
                    (RefreshTokensTable.accessTokenJti eq currentSessionId.toString())
                )

    private fun sessionIdentityKey(token: RefreshTokenInfo): String =
        token.storedSessionId?.toString() ?: "legacy:${token.tokenId}"

    private fun matchesDisplayedSessionId(token: RefreshTokenInfo, sessionId: SessionId): Boolean {
        return token.sessionId == sessionId
    }

    private fun matchesCurrentSessionIdentity(
        token: RefreshTokenInfo,
        currentSessionId: SessionId?
    ): Boolean {
        currentSessionId ?: return false
        return token.storedSessionId == currentSessionId ||
            (token.storedSessionId == null && token.accessTokenJti == currentSessionId.toString())
    }

    private fun resolveStableSessionId(
        storedSessionId: SessionId?,
        accessTokenJti: String?,
        tokenId: String
    ): SessionId {
        return storedSessionId
            ?: accessTokenJti?.let { runCatching { SessionId(it) }.getOrNull() }
            ?: SessionId(tokenId)
    }

    private fun displaySessionId(
        storedSessionId: SessionId?,
        tokenId: String
    ): SessionId = storedSessionId ?: SessionId(tokenId)

    private fun sessionFingerprint(token: RefreshTokenInfo): SessionFingerprint {
        return SessionFingerprint(
            deviceType = token.deviceType,
            userAgent = token.userAgent?.trim(),
            ipAddress = token.ipAddress?.trim()
        )
    }

    private fun RefreshTokenInfo.toRevokedSessionInfo(): RevokedSessionInfo {
        return RevokedSessionInfo(
            sessionId = sessionId,
            accessTokenJti = accessTokenJti,
            accessTokenExpiresAt = accessTokenExpiresAt
        )
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
