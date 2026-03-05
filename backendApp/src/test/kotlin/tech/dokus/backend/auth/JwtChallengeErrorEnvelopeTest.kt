package tech.dokus.backend.auth

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import tech.dokus.domain.ids.UserId
import tech.dokus.foundation.backend.config.JwtConfig
import tech.dokus.foundation.backend.configure.configureErrorHandling
import tech.dokus.foundation.backend.configure.configureJwtAuthentication
import tech.dokus.foundation.backend.configure.configureSerialization
import tech.dokus.foundation.backend.security.TokenBlacklistService
import tech.dokus.foundation.backend.security.authenticateJwt
import java.time.Instant
import kotlin.test.assertEquals

class JwtChallengeErrorEnvelopeTest {

    @Test
    fun `jwt challenge returns polymorphic dokus exception envelope`() = testApplication {
        application {
            configureJwtChallengeTestApp()
        }

        val response = client.get("/protected")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val payload = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals("DokusException.NotAuthenticated", payload["type"]?.jsonPrimitive?.content)
        assertEquals("NOT_AUTHENTICATED", payload["errorCode"]?.jsonPrimitive?.content)
    }

    private fun Application.configureJwtChallengeTestApp() {
        install(Koin) {
            modules(
                module {
                    single { tech.dokus.foundation.backend.security.JwtValidator(testJwtConfig()) }
                    single<TokenBlacklistService> { NoOpTokenBlacklistService() }
                }
            )
        }

        configureSerialization()
        configureErrorHandling()
        configureJwtAuthentication()

        routing {
            authenticateJwt {
                get("/protected") {
                    call.respondText("ok")
                }
            }
        }
    }

    private fun testJwtConfig(): JwtConfig = JwtConfig(
        issuer = "test-issuer",
        audience = "test-audience",
        realm = "test-realm",
        secret = "test-secret",
        publicKeyPath = null,
        privateKeyPath = null,
        algorithm = "HS256"
    )
}

private class NoOpTokenBlacklistService : TokenBlacklistService {
    override suspend fun blacklistToken(jti: String, expiresAt: Instant) = Unit

    override suspend fun isBlacklisted(jti: String): Boolean = false

    override suspend fun blacklistAllUserTokens(userId: UserId) = Unit

    override suspend fun trackUserToken(userId: UserId, jti: String, expiresAt: Instant) = Unit
}
