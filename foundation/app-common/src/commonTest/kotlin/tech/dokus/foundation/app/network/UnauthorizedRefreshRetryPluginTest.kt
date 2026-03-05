package tech.dokus.foundation.app.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.ids.TenantId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnauthorizedRefreshRetryPluginTest {

    @Test
    fun `retries unauthorized response with refreshed token before body parsing`() = runTest {
        val headersSeen = mutableListOf<String?>()
        var requestCount = 0
        val engine = MockEngine { request ->
            requestCount += 1
            headersSeen += request.headers[HttpHeaders.Authorization]
            if (requestCount == 1) {
                respond(
                    content = "{}",
                    status = HttpStatusCode.Unauthorized,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            } else {
                respond(
                    content = """{"ok":true}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }

        val tokenManager = FakeTokenManager(
            isAuthenticated = true,
            latestToken = "stale-token",
            refreshedToken = "fresh-token"
        )
        var forcedLogoutCalled = false
        val client = HttpClient(engine) {
            withUnauthorizedRefreshRetry(
                tokenManager = tokenManager,
                onAuthenticationFailed = { forcedLogoutCalled = true },
                maxRetries = 1
            )
        }

        val response = client.get("https://example.com/protected") {
            headers.append(HttpHeaders.Authorization, "Bearer stale-token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(2, requestCount)
        assertEquals(listOf<String?>("Bearer stale-token", "Bearer fresh-token"), headersSeen)
        assertEquals(1, tokenManager.refreshCalls)
        assertFalse(forcedLogoutCalled)

        client.close()
    }

    @Test
    fun `calls forced logout callback when refresh invalidates auth state`() = runTest {
        val engine = MockEngine {
            respond(
                content = "{}",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val tokenManager = FakeTokenManager(
            isAuthenticated = true,
            latestToken = "stale-token",
            refreshedToken = null,
            invalidateAuthOnRefreshFailure = true
        )
        var forcedLogoutCalled = false
        val client = HttpClient(engine) {
            withUnauthorizedRefreshRetry(
                tokenManager = tokenManager,
                onAuthenticationFailed = { forcedLogoutCalled = true },
                maxRetries = 1
            )
        }

        val response = client.get("https://example.com/protected") {
            headers.append(HttpHeaders.Authorization, "Bearer stale-token")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(forcedLogoutCalled)
        assertEquals(1, tokenManager.refreshCalls)

        client.close()
    }

    @Test
    fun `does not call forced logout callback when refresh fails but auth remains valid`() = runTest {
        val engine = MockEngine {
            respond(
                content = "{}",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val tokenManager = FakeTokenManager(
            isAuthenticated = true,
            latestToken = "stale-token",
            refreshedToken = null,
            invalidateAuthOnRefreshFailure = false
        )
        var forcedLogoutCalled = false
        val client = HttpClient(engine) {
            withUnauthorizedRefreshRetry(
                tokenManager = tokenManager,
                onAuthenticationFailed = { forcedLogoutCalled = true },
                maxRetries = 1
            )
        }

        val response = client.get("https://example.com/protected") {
            headers.append(HttpHeaders.Authorization, "Bearer stale-token")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertFalse(forcedLogoutCalled)
        assertEquals(1, tokenManager.refreshCalls)

        client.close()
    }
}

private class FakeTokenManager(
    isAuthenticated: Boolean,
    private val latestToken: String?,
    private val refreshedToken: String?,
    private val invalidateAuthOnRefreshFailure: Boolean = false,
) : TokenManager {
    private val isAuthenticatedState = MutableStateFlow(isAuthenticated)
    override val isAuthenticated = isAuthenticatedState

    var refreshCalls: Int = 0
        private set

    override suspend fun getValidAccessToken(): String? = latestToken

    override suspend fun getRefreshToken(): String? = "refresh-token"

    override suspend fun getSelectedTenantId(): TenantId? = null

    override suspend fun refreshToken(force: Boolean): String? {
        refreshCalls += 1
        if (refreshedToken == null && invalidateAuthOnRefreshFailure) {
            isAuthenticatedState.value = false
        }
        return refreshedToken
    }

    override suspend fun onAuthenticationFailed() {
        isAuthenticatedState.value = false
    }
}
