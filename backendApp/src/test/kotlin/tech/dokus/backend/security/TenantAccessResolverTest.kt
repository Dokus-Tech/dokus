package tech.dokus.backend.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.builtins.ListSerializer
import org.junit.jupiter.api.Test
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.auth.JwtClaims
import tech.dokus.domain.model.auth.JwtTenantMembershipClaim
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.config.JwtConfig
import tech.dokus.foundation.backend.configure.configureErrorHandling
import tech.dokus.foundation.backend.security.DokusPrincipal
import tech.dokus.foundation.backend.security.JwtValidator
import tech.dokus.foundation.backend.security.dokusPrincipal
import java.time.Instant
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TenantAccessResolverTest {

    @Test
    fun `tenant route without tenant header returns bad request`() = tenantAccessTestApplication {
        val response = authenticatedGet("/tenant-required")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `tenant route with malformed tenant header returns bad request`() = tenantAccessTestApplication {
        val response = authenticatedGet(
            "/tenant-required",
            tenantHeader = "not-a-uuid"
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `tenant route with no jwt membership returns forbidden`() = tenantAccessTestApplication {
        val tenantId = TenantId.generate()
        val response = authenticatedGet(
            "/tenant-required",
            tenantHeader = tenantId.toString(),
            tenantMemberships = emptyList(),
        )

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `active jwt membership attaches tenant access and memoizes resolver per call`() = tenantAccessTestApplication {
        val tenantId = TenantId.generate()
        val response = authenticatedGet(
            "/tenant-required-twice",
            tenantHeader = tenantId.toString(),
            tenantMemberships = listOf(
                membership(tenantId = tenantId, role = UserRole.Owner)
            )
        )

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains(tenantId.toString()))
        assertTrue(body.contains(UserRole.Owner.dbValue))
    }

    @Test
    fun `tenant id path parameter fallback is accepted when header is omitted`() = tenantAccessTestApplication {
        val tenantId = TenantId.generate()
        val response = authenticatedGet(
            "/tenant-by-path/$tenantId",
            tenantMemberships = listOf(
                membership(tenantId = tenantId, role = UserRole.Viewer)
            )
        )

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains(UserRole.Viewer.dbValue))
    }

    @Test
    fun `authenticated non tenant route succeeds without tenant context`() = tenantAccessTestApplication {
        val response = authenticatedGet("/non-tenant")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(TEST_USER_ID.toString(), response.bodyAsText())
    }

    private fun tenantAccessTestApplication(
        testBlock: suspend ApplicationTestBuilder.() -> Unit
    ) = testApplication {
        application {
            configureTenantAccessTestApp()
        }
        testBlock()
    }

    private fun Application.configureTenantAccessTestApp() {
        val jwtValidator = JwtValidator(testJwtConfig())

        install(ContentNegotiation) {
            json(json)
        }
        configureErrorHandling()

        install(Authentication) {
            jwt("test-auth") {
                verifier(
                    JWT.require(Algorithm.HMAC256(TEST_JWT_SECRET))
                        .withIssuer(TEST_JWT_ISSUER)
                        .withAudience(TEST_JWT_AUDIENCE)
                        .build()
                )
                validate { credential ->
                    jwtValidator.extractAuthInfo(credential.payload)?.let(DokusPrincipal::fromAuthInfo)
                }
            }
        }

        routing {
            authenticate("test-auth") {
                get("/tenant-required") {
                    val access = requireTenantAccess()
                    call.respondText("${access.tenantId}:${access.role.dbValue}")
                }
                get("/tenant-required-twice") {
                    val first = requireTenantAccess()
                    val second = requireTenantAccess()
                    call.respondText("${first.tenantId}:${second.role.dbValue}")
                }
                get("/tenant-by-path/{tenantId}") {
                    val access = requireTenantAccess()
                    call.respondText("${access.tenantId}:${access.role.dbValue}")
                }
                get("/non-tenant") {
                    call.respondText(dokusPrincipal.userId.toString())
                }
            }
        }
    }

    private suspend fun ApplicationTestBuilder.authenticatedGet(
        path: String,
        tenantHeader: String? = null,
        tenantMemberships: List<JwtTenantMembershipClaim> = emptyList(),
    ) = client.get(path) {
        header(
            HttpHeaders.Authorization,
            "Bearer ${testAccessToken(tenantMemberships = tenantMemberships)}"
        )
        if (tenantHeader != null) {
            header(TenantHeaderName, tenantHeader)
        }
    }

    private fun membership(
        tenantId: TenantId,
        role: UserRole,
    ): JwtTenantMembershipClaim = JwtTenantMembershipClaim(
        tenantId = tenantId,
        role = role,
    )

    private fun testJwtConfig(): JwtConfig = JwtConfig(
        issuer = TEST_JWT_ISSUER,
        audience = TEST_JWT_AUDIENCE,
        realm = "test-realm",
        secret = TEST_JWT_SECRET,
        publicKeyPath = null,
        privateKeyPath = null,
        algorithm = "HS256"
    )

    private fun testAccessToken(
        tenantMemberships: List<JwtTenantMembershipClaim>,
    ): String =
        JWT.create()
            .withIssuer(TEST_JWT_ISSUER)
            .withAudience(TEST_JWT_AUDIENCE)
            .withSubject(TEST_USER_ID.toString())
            .withJWTId("test-session-jti")
            .withClaim(JwtClaims.CLAIM_EMAIL, "tenant-resolver@test.dokus")
            .withClaim(
                JwtClaims.CLAIM_TENANTS,
                json.encodeToString(ListSerializer(JwtTenantMembershipClaim.serializer()), tenantMemberships)
            )
            .withIssuedAt(Date.from(Instant.now()))
            .withExpiresAt(Date.from(Instant.now().plusSeconds(600)))
            .sign(Algorithm.HMAC256(TEST_JWT_SECRET))

    companion object {
        private val TEST_USER_ID = UserId("00000000-0000-0000-0000-000000000111")
        private const val TEST_JWT_SECRET = "tenant-access-resolver-secret"
        private const val TEST_JWT_ISSUER = "tenant-resolver-test"
        private const val TEST_JWT_AUDIENCE = "tenant-resolver-audience"
    }
}
