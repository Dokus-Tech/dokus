@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.database.repository.auth

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import tech.dokus.database.tables.auth.RefreshTokensTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.domain.DeviceType
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.ids.UserId
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

class RefreshTokenRepositoryTest {

    private lateinit var database: Database
    private val repository = RefreshTokenRepository()

    @BeforeTest
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:test_refresh_tokens_${System.currentTimeMillis()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        transaction(database) {
            SchemaUtils.create(
                UsersTable,
                RefreshTokensTable
            )
        }
    }

    @AfterTest
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(
                RefreshTokensTable,
                UsersTable
            )
        }
    }

    @Test
    fun `validateAndRotate preserves stable session id`() = runBlocking {
        val userUuid = UUID.randomUUID()
        val userId = UserId(userUuid.toKotlinUuid())
        val sessionId = SessionId("11111111-1111-1111-1111-111111111111")

        insertUser(userUuid)
        repository.saveRefreshToken(
            userId = userId,
            token = "refresh-token",
            expiresAt = futureInstant(),
            sessionId = sessionId,
            accessTokenJti = "22222222-2222-2222-2222-222222222222",
            accessTokenExpiresAt = futureInstant()
        ).getOrThrow()

        val validated = repository.validateAndRotate("refresh-token").getOrThrow()

        assertEquals(userId, validated.userId)
        assertEquals(sessionId, validated.sessionId)
        assertTrue(
            transaction(database) {
                RefreshTokensTable
                    .selectAll()
                    .single()[RefreshTokensTable.isRevoked]
            }
        )
    }

    @Test
    fun `countActiveForUser counts distinct stable sessions only`() = runBlocking {
        val userUuid = UUID.randomUUID()
        val userId = UserId(userUuid.toKotlinUuid())
        val sharedSessionId = SessionId("11111111-1111-1111-1111-111111111111")
        val otherSessionId = SessionId("22222222-2222-2222-2222-222222222222")

        insertUser(userUuid)
        insertRefreshToken(
            tokenId = UUID.randomUUID(),
            userId = userUuid,
            tokenHash = "a".repeat(64),
            sessionId = sharedSessionId,
            accessTokenJti = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        )
        insertRefreshToken(
            tokenId = UUID.randomUUID(),
            userId = userUuid,
            tokenHash = "b".repeat(64),
            sessionId = sharedSessionId,
            accessTokenJti = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
        )
        insertRefreshToken(
            tokenId = UUID.randomUUID(),
            userId = userUuid,
            tokenHash = "c".repeat(64),
            sessionId = otherSessionId,
            accessTokenJti = "cccccccc-cccc-cccc-cccc-cccccccccccc"
        )

        assertEquals(2, repository.countActiveForUser(userId))
    }

    @Test
    fun `saveRefreshToken replaces current session when requested`() = runBlocking {
        val userUuid = UUID.randomUUID()
        val userId = UserId(userUuid.toKotlinUuid())
        val sessionId = SessionId("11111111-1111-1111-1111-111111111111")

        insertUser(userUuid)
        repository.saveRefreshToken(
            userId = userId,
            token = "refresh-token-1",
            expiresAt = futureInstant(),
            sessionId = sessionId,
            accessTokenJti = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
            accessTokenExpiresAt = futureInstant()
        ).getOrThrow()
        repository.saveRefreshToken(
            userId = userId,
            token = "refresh-token-2",
            expiresAt = futureInstant(),
            sessionId = sessionId,
            accessTokenJti = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
            accessTokenExpiresAt = futureInstant(),
            replaceExistingSessionIdentity = sessionId
        ).getOrThrow()

        val activeTokens = repository.getUserActiveTokens(userId)

        assertEquals(1, activeTokens.size)
        assertEquals(sessionId, activeTokens.single().sessionId)
        assertEquals(1, repository.listActiveSessions(userId, sessionId).size)
    }

    @Test
    fun `revokeSessionById revokes every row for a stable session`() = runBlocking {
        val userUuid = UUID.randomUUID()
        val userId = UserId(userUuid.toKotlinUuid())
        val sessionId = SessionId("11111111-1111-1111-1111-111111111111")

        insertUser(userUuid)
        val firstRowId = UUID.randomUUID()
        val secondRowId = UUID.randomUUID()
        val otherRowId = UUID.randomUUID()
        insertRefreshToken(
            tokenId = firstRowId,
            userId = userUuid,
            tokenHash = "a".repeat(64),
            sessionId = sessionId,
            accessTokenJti = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        )
        insertRefreshToken(
            tokenId = secondRowId,
            userId = userUuid,
            tokenHash = "b".repeat(64),
            sessionId = sessionId,
            accessTokenJti = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
        )
        insertRefreshToken(
            tokenId = otherRowId,
            userId = userUuid,
            tokenHash = "c".repeat(64),
            sessionId = SessionId("22222222-2222-2222-2222-222222222222"),
            accessTokenJti = "cccccccc-cccc-cccc-cccc-cccccccccccc"
        )

        val revoked = repository.revokeSessionById(userId, sessionId).getOrThrow()

        assertEquals(2, revoked.size)
        assertTrue(isRevoked(firstRowId))
        assertTrue(isRevoked(secondRowId))
        assertFalse(isRevoked(otherRowId))
    }

    @Test
    fun `revokeOtherSessions revokes legacy sessions when current row is identifiable`() = runBlocking {
        val userUuid = UUID.randomUUID()
        val userId = UserId(userUuid.toKotlinUuid())
        val currentSessionId = SessionId("11111111-1111-1111-1111-111111111111")
        val otherSessionId = SessionId("22222222-2222-2222-2222-222222222222")

        insertUser(userUuid)
        val currentTokenId = UUID.randomUUID()
        val legacyTokenId = UUID.randomUUID()
        val otherTokenId = UUID.randomUUID()
        insertRefreshToken(
            tokenId = currentTokenId,
            userId = userUuid,
            tokenHash = "a".repeat(64),
            sessionId = currentSessionId,
            accessTokenJti = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
        )
        insertRefreshToken(
            tokenId = legacyTokenId,
            userId = userUuid,
            tokenHash = "b".repeat(64),
            sessionId = null,
            accessTokenJti = null
        )
        insertRefreshToken(
            tokenId = otherTokenId,
            userId = userUuid,
            tokenHash = "c".repeat(64),
            sessionId = otherSessionId,
            accessTokenJti = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
        )

        val revoked = repository.revokeOtherSessions(userId, currentSessionId).getOrThrow()

        assertEquals(2, revoked.size)
        assertTrue(revoked.any { it.sessionId == SessionId(legacyTokenId.toString()) })
        assertTrue(revoked.any { it.sessionId == otherSessionId })
        assertFalse(isRevoked(currentTokenId))
        assertTrue(isRevoked(legacyTokenId))
        assertTrue(isRevoked(otherTokenId))
    }

    @Test
    fun `revokeOtherSessions keeps legacy rows when current session row is not identifiable`() = runBlocking {
        val userUuid = UUID.randomUUID()
        val userId = UserId(userUuid.toKotlinUuid())
        val currentSessionId = SessionId("11111111-1111-1111-1111-111111111111")
        val otherSessionId = SessionId("22222222-2222-2222-2222-222222222222")

        insertUser(userUuid)
        val legacyTokenId = UUID.randomUUID()
        val otherTokenId = UUID.randomUUID()
        insertRefreshToken(
            tokenId = legacyTokenId,
            userId = userUuid,
            tokenHash = "d".repeat(64),
            sessionId = null,
            accessTokenJti = null
        )
        insertRefreshToken(
            tokenId = otherTokenId,
            userId = userUuid,
            tokenHash = "e".repeat(64),
            sessionId = otherSessionId,
            accessTokenJti = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
        )

        val revoked = repository.revokeOtherSessions(userId, currentSessionId).getOrThrow()

        assertEquals(1, revoked.size)
        assertEquals(otherSessionId, revoked.single().sessionId)
        assertFalse(isRevoked(legacyTokenId))
        assertTrue(isRevoked(otherTokenId))
    }

    @Test
    fun `listActiveSessions cleans up superseded legacy rows and marks current session`() = runBlocking {
        val userUuid = UUID.randomUUID()
        val userId = UserId(userUuid.toKotlinUuid())
        val currentSessionId = SessionId("11111111-1111-1111-1111-111111111111")
        val otherSessionId = SessionId("22222222-2222-2222-2222-222222222222")
        val legacyTokenId = UUID.randomUUID()

        insertUser(userUuid)
        insertRefreshToken(
            tokenId = legacyTokenId,
            userId = userUuid,
            tokenHash = "a".repeat(64),
            sessionId = null,
            accessTokenJti = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
            accessTokenExpiresAt = pastInstant(),
            deviceType = DeviceType.Desktop,
            ipAddress = "10.0.0.1",
            userAgent = "Chrome on macOS",
            createdAt = pastInstant()
        )
        insertRefreshToken(
            tokenId = UUID.randomUUID(),
            userId = userUuid,
            tokenHash = "b".repeat(64),
            sessionId = currentSessionId,
            accessTokenJti = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
            deviceType = DeviceType.Desktop,
            ipAddress = "10.0.0.1",
            userAgent = "Chrome on macOS",
            createdAt = futureInstant()
        )
        insertRefreshToken(
            tokenId = UUID.randomUUID(),
            userId = userUuid,
            tokenHash = "c".repeat(64),
            sessionId = otherSessionId,
            accessTokenJti = "cccccccc-cccc-cccc-cccc-cccccccccccc"
        )
        insertRefreshToken(
            tokenId = UUID.randomUUID(),
            userId = userUuid,
            tokenHash = "d".repeat(64),
            sessionId = SessionId("33333333-3333-3333-3333-333333333333"),
            accessTokenJti = "dddddddd-dddd-dddd-dddd-dddddddddddd",
            isRevoked = true
        )
        insertRefreshToken(
            tokenId = UUID.randomUUID(),
            userId = userUuid,
            tokenHash = "e".repeat(64),
            sessionId = SessionId("44444444-4444-4444-4444-444444444444"),
            accessTokenJti = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee",
            expiresAt = pastInstant()
        )

        val sessions = repository.listActiveSessions(userId, currentSessionId)

        assertEquals(2, sessions.size)
        assertEquals(currentSessionId, sessions.first().id)
        assertTrue(sessions.first().isCurrent)
        assertEquals(otherSessionId, sessions.last().id)
        assertTrue(isRevoked(legacyTokenId))
    }

    private fun insertUser(userId: UUID) {
        transaction(database) {
            UsersTable.insert {
                it[id] = userId
                it[email] = "test-$userId@example.com"
                it[passwordHash] = "hash"
                it[firstName] = "Test"
                it[lastName] = "User"
                it[emailVerified] = true
                it[isActive] = true
            }
        }
    }

    @Suppress("LongParameterList")
    private fun insertRefreshToken(
        tokenId: UUID,
        userId: UUID,
        tokenHash: String,
        sessionId: SessionId?,
        accessTokenJti: String?,
        isRevoked: Boolean = false,
        expiresAt: Instant = futureInstant(),
        accessTokenExpiresAt: Instant = futureInstant(),
        deviceType: DeviceType = DeviceType.Desktop,
        ipAddress: String? = null,
        userAgent: String? = null,
        createdAt: Instant = Instant.fromEpochSeconds(1_700_000_000),
    ) {
        transaction(database) {
            RefreshTokensTable.insert {
                it[id] = tokenId
                it[RefreshTokensTable.userId] = userId
                it[RefreshTokensTable.sessionId] = sessionId?.uuid?.toString()?.let(UUID::fromString)
                it[RefreshTokensTable.tokenHash] = tokenHash
                it[RefreshTokensTable.expiresAt] = expiresAt.toLocalDateTime(TimeZone.UTC)
                it[RefreshTokensTable.isRevoked] = isRevoked
                it[RefreshTokensTable.accessTokenJti] = accessTokenJti
                it[RefreshTokensTable.accessTokenExpiresAt] =
                    accessTokenExpiresAt.toLocalDateTime(TimeZone.UTC)
                it[RefreshTokensTable.deviceType] = deviceType
                it[RefreshTokensTable.ipAddress] = ipAddress
                it[RefreshTokensTable.userAgent] = userAgent
                it[RefreshTokensTable.createdAt] = createdAt.toLocalDateTime(TimeZone.UTC)
            }
        }
    }

    private fun isRevoked(tokenId: UUID): Boolean = transaction(database) {
        RefreshTokensTable
            .selectAll()
            .where { RefreshTokensTable.id eq tokenId }
            .single()[RefreshTokensTable.isRevoked]
    }

    private fun futureInstant(): Instant = Instant.fromEpochSeconds(2_000_000_000)

    private fun pastInstant(): Instant = Instant.fromEpochSeconds(1_600_000_000)
}
