
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
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.ids.UserId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun `revokeOtherSessions revokes legacy sessions when current row is identifiable`() = runBlocking {
        val userUuid = Uuid.random()
        val userId = UserId(userUuid)

        val currentTokenId = Uuid.random()
        val legacyTokenId = Uuid.random()
        val otherTokenId = Uuid.random()
        val currentSessionJti = "11111111-1111-1111-1111-111111111111"
        val otherSessionJti = "22222222-2222-2222-2222-222222222222"

        insertUser(userUuid)
        insertRefreshToken(
            tokenId = currentTokenId,
            userId = userUuid,
            tokenHash = "a".repeat(64),
            accessTokenJti = currentSessionJti
        )
        insertRefreshToken(
            tokenId = legacyTokenId,
            userId = userUuid,
            tokenHash = "b".repeat(64),
            accessTokenJti = null
        )
        insertRefreshToken(
            tokenId = otherTokenId,
            userId = userUuid,
            tokenHash = "c".repeat(64),
            accessTokenJti = otherSessionJti
        )

        val revoked = repository.revokeOtherSessions(userId, currentSessionJti).getOrThrow()

        assertEquals(2, revoked.size)
        assertTrue(revoked.any { it.sessionId == SessionId(legacyTokenId.toString()) })
        assertTrue(revoked.any { it.sessionId == SessionId(otherSessionJti) })

        assertFalse(isRevoked(currentTokenId))
        assertTrue(isRevoked(legacyTokenId))
        assertTrue(isRevoked(otherTokenId))
    }

    @Test
    fun `revokeOtherSessions keeps legacy rows when current session row is not identifiable`() = runBlocking {
        val userUuid = Uuid.random()
        val userId = UserId(userUuid)

        val legacyTokenId = Uuid.random()
        val otherTokenId = Uuid.random()
        val currentSessionJti = "33333333-3333-3333-3333-333333333333"
        val otherSessionJti = "44444444-4444-4444-4444-444444444444"

        insertUser(userUuid)
        insertRefreshToken(
            tokenId = legacyTokenId,
            userId = userUuid,
            tokenHash = "d".repeat(64),
            accessTokenJti = null
        )
        insertRefreshToken(
            tokenId = otherTokenId,
            userId = userUuid,
            tokenHash = "e".repeat(64),
            accessTokenJti = otherSessionJti
        )

        val revoked = repository.revokeOtherSessions(userId, currentSessionJti).getOrThrow()

        assertEquals(1, revoked.size)
        assertEquals(SessionId(otherSessionJti), revoked.single().sessionId)

        assertFalse(isRevoked(legacyTokenId))
        assertTrue(isRevoked(otherTokenId))
    }

    private fun insertUser(userId: UUID) {
        transaction(database) {
            UsersTable.insert {
                it[id] = userId
                it[email] = "test-${userId}@example.com"
                it[passwordHash] = "hash"
                it[firstName] = "Test"
                it[lastName] = "User"
                it[emailVerified] = true
                it[isActive] = true
            }
        }
    }

    private fun insertRefreshToken(
        tokenId: UUID,
        userId: UUID,
        tokenHash: String,
        accessTokenJti: String?
    ) {
        val future = Instant.fromEpochSeconds(2_000_000_000).toLocalDateTime(TimeZone.UTC)
        transaction(database) {
            RefreshTokensTable.insert {
                it[id] = tokenId
                it[RefreshTokensTable.userId] = userId
                it[RefreshTokensTable.tokenHash] = tokenHash
                it[expiresAt] = future
                it[isRevoked] = false
                it[RefreshTokensTable.accessTokenJti] = accessTokenJti
                it[accessTokenExpiresAt] = future
            }
        }
    }

    private fun isRevoked(tokenId: UUID): Boolean = transaction(database) {
        RefreshTokensTable
            .selectAll()
            .where { RefreshTokensTable.id eq tokenId }
            .single()[RefreshTokensTable.isRevoked]
    }
}
