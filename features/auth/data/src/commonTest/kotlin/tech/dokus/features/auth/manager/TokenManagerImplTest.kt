package tech.dokus.features.auth.manager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import tech.dokus.features.auth.storage.TokenStorage
import tech.dokus.foundation.sstorage.SecureStorage
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class TokenManagerImplTest {

    @Test
    fun `initialize with refresh needed token stays authenticated without refresh call`() = runTest {
        val tokenStorage = TokenStorage(InMemorySecureStorage())
        val manager = TokenManagerImpl(tokenStorage)

        val now = Clock.System.now().epochSeconds
        tokenStorage.saveAccessToken(jwtToken(exp = now + 60))
        tokenStorage.saveRefreshToken("refresh-token")

        var refreshCalls = 0
        manager.onTokenRefreshNeeded = { _, _ ->
            refreshCalls += 1
            null
        }

        manager.initialize()

        assertTrue(manager.isAuthenticated.value)
        assertEquals(0, refreshCalls)
    }

    @Test
    fun `initialize with expired token and refresh token stays authenticated offline`() = runTest {
        val tokenStorage = TokenStorage(InMemorySecureStorage())
        val manager = TokenManagerImpl(tokenStorage)

        val now = Clock.System.now().epochSeconds
        tokenStorage.saveAccessToken(jwtToken(exp = now - 60))
        tokenStorage.saveRefreshToken("refresh-token")

        var refreshCalls = 0
        manager.onTokenRefreshNeeded = { _, _ ->
            refreshCalls += 1
            error("initialize must not refresh")
        }

        manager.initialize()

        assertTrue(manager.isAuthenticated.value)
        assertEquals(0, refreshCalls)
    }

    @Test
    fun `initialize with missing or invalid local session is unauthenticated`() = runTest {
        val missingStorage = TokenStorage(InMemorySecureStorage())
        val missingManager = TokenManagerImpl(missingStorage)

        missingManager.initialize()
        assertFalse(missingManager.isAuthenticated.value)

        val invalidStorage = TokenStorage(InMemorySecureStorage())
        val invalidManager = TokenManagerImpl(invalidStorage)
        invalidStorage.saveAccessToken("invalid-token")
        invalidStorage.saveRefreshToken("refresh-token")

        invalidManager.initialize()
        assertFalse(invalidManager.isAuthenticated.value)
    }

    @Test
    fun `getValidAccessToken returns current token when refresh needed and network error occurs`() = runTest {
        val tokenStorage = TokenStorage(InMemorySecureStorage())
        val manager = TokenManagerImpl(tokenStorage)

        val now = Clock.System.now().epochSeconds
        val refreshNeededToken = jwtToken(exp = now + 60)
        tokenStorage.saveAccessToken(refreshNeededToken)
        tokenStorage.saveRefreshToken("refresh-token")

        manager.onTokenRefreshNeeded = { _, _ ->
            throw IllegalStateException("Connect timeout has expired")
        }

        val token = manager.getValidAccessToken()

        assertEquals(refreshNeededToken, token)
        assertEquals(refreshNeededToken, tokenStorage.getAccessToken())
    }

    @Test
    fun `refresh auth failure clears tokens and updates state`() = runTest {
        val tokenStorage = TokenStorage(InMemorySecureStorage())
        val manager = TokenManagerImpl(tokenStorage)

        val now = Clock.System.now().epochSeconds
        tokenStorage.saveAccessToken(jwtToken(exp = now + 60))
        tokenStorage.saveRefreshToken("refresh-token")

        manager.initialize()
        assertTrue(manager.isAuthenticated.value)

        manager.onTokenRefreshNeeded = { _, _ -> null }

        val refreshed = manager.refreshToken(force = false)

        assertNull(refreshed)
        assertFalse(manager.isAuthenticated.value)
        assertNull(tokenStorage.getAccessToken())
        assertNull(tokenStorage.getRefreshToken())
    }
}

private class InMemorySecureStorage : SecureStorage() {
    private val values = mutableMapOf<String, String>()
    private val streams = mutableMapOf<String, MutableStateFlow<String?>>()

    override suspend fun putString(key: String, value: String) {
        values[key] = value
        streamFor(key).value = value
    }

    override suspend fun getString(key: String): String? = values[key]

    override fun observeString(key: String): Flow<String?> = streamFor(key)

    override suspend fun remove(key: String) {
        values.remove(key)
        streamFor(key).value = null
    }

    override suspend fun clear() {
        values.clear()
        streams.values.forEach { it.value = null }
    }

    override suspend fun contains(key: String): Boolean = values.containsKey(key)

    override suspend fun getAllKeys(): Set<String> = values.keys.toSet()

    private fun streamFor(key: String): MutableStateFlow<String?> {
        return streams.getOrPut(key) { MutableStateFlow(values[key]) }
    }
}

@OptIn(ExperimentalEncodingApi::class)
private fun jwtToken(exp: Long): String {
    val header = """{"alg":"HS256","typ":"JWT"}"""
    val payload =
        """{"sub":"00000000-0000-0000-0000-000000000111","email":"test@dokus.app","iat":${exp - 3600},"exp":$exp,"jti":"jti-1"}"""

    return "${header.toBase64UrlNoPadding()}.${payload.toBase64UrlNoPadding()}.signature"
}

@OptIn(ExperimentalEncodingApi::class)
private fun String.toBase64UrlNoPadding(): String {
    return Base64.encode(encodeToByteArray())
        .trimEnd('=')
        .replace('+', '-')
        .replace('/', '_')
}
