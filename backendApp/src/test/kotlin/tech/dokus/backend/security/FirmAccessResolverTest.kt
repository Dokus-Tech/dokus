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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.builtins.ListSerializer
import org.junit.jupiter.api.Test
import tech.dokus.database.repository.auth.FirmRepository
import tech.dokus.domain.enums.FirmRole
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.auth.JwtClaims
import tech.dokus.domain.model.auth.JwtFirmMembershipClaim
import tech.dokus.domain.model.auth.JwtTenantMembershipClaim
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.config.JwtConfig
import tech.dokus.foundation.backend.configure.configureErrorHandling
import tech.dokus.foundation.backend.security.DokusPrincipal
import tech.dokus.foundation.backend.security.JwtValidator
import java.time.Instant
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirmAccessResolverTest {

    @Test
    fun `firm route without firm header returns bad request`() = firmAccessTestApplication {
        val response = authenticatedGet("/firm-required")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `firm route with malformed firm header returns bad request`() = firmAccessTestApplication {
        val response = authenticatedGet(
            "/firm-required",
            firmHeader = "not-a-uuid",
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `firm route with no jwt membership returns forbidden`() = firmAccessTestApplication {
        val firmId = FirmId.generate()
        val response = authenticatedGet(
            "/firm-required",
            firmHeader = firmId.toString(),
            firmMemberships = emptyList(),
        )

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `active jwt firm membership attaches context and memoizes resolver`() = firmAccessTestApplication {
        val firmId = FirmId.generate()
        val response = authenticatedGet(
            "/firm-required-twice",
            firmHeader = firmId.toString(),
            firmMemberships = listOf(membership(firmId, FirmRole.Owner)),
        )

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains(firmId.toString()))
        assertTrue(body.contains(FirmRole.Owner.dbValue))
    }

    @Test
    fun `firm id path parameter fallback is accepted when header is omitted`() = firmAccessTestApplication {
        val firmId = FirmId.generate()
        val response = authenticatedGet(
            "/firm-by-path/$firmId",
            firmMemberships = listOf(membership(firmId, FirmRole.Admin)),
        )

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains(FirmRole.Admin.dbValue))
    }

    @Test
    fun `firm client route returns forbidden without active firm access row`() = firmAccessTestApplication {
        val firmId = FirmId.generate()
        val tenantId = TenantId.generate()
        val response = authenticatedGet(
            "/firm-client-required",
            firmHeader = firmId.toString(),
            tenantHeader = tenantId.toString(),
            firmMemberships = listOf(membership(firmId, FirmRole.Owner)),
        )

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `firm client route succeeds when active access exists`() = firmAccessTestApplication(
        firmRepository = mockk(),
    ) { firmRepository ->
        val firmId = FirmId.generate()
        val tenantId = TenantId.generate()
        coEvery { firmRepository.hasActiveAccess(firmId, tenantId) } returns true

        val response = authenticatedGet(
            "/firm-client-required",
            firmHeader = firmId.toString(),
            tenantHeader = tenantId.toString(),
            firmMemberships = listOf(membership(firmId, FirmRole.Staff)),
        )

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains(tenantId.toString()))
        coVerify(exactly = 1) { firmRepository.hasActiveAccess(firmId, tenantId) }
    }

    private fun firmAccessTestApplication(
        firmRepository: FirmRepository = mockk {
            coEvery { hasActiveAccess(any(), any()) } returns false
        },
        testBlock: suspend ApplicationTestBuilder.(FirmRepository) -> Unit = {},
    ) = testApplication {
        application {
            configureFirmAccessTestApp(firmRepository)
        }
        testBlock(firmRepository)
    }

    private fun Application.configureFirmAccessTestApp(
        firmRepository: FirmRepository,
    ) {
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
                get("/firm-required") {
                    val access = requireFirmAccess()
                    call.respondText("${access.firmId}:${access.role.dbValue}")
                }
                get("/firm-required-twice") {
                    val first = requireFirmAccess()
                    val second = requireFirmAccess()
                    call.respondText("${first.firmId}:${second.role.dbValue}")
                }
                get("/firm-by-path/{firmId}") {
                    val access = requireFirmAccess()
                    call.respondText("${access.firmId}:${access.role.dbValue}")
                }
                get("/firm-client-required") {
                    val access = requireFirmClientAccess(firmRepository)
                    call.respondText("${access.firmId}:${access.tenantId}:${access.role.dbValue}")
                }
            }
        }
    }

    private suspend fun ApplicationTestBuilder.authenticatedGet(
        path: String,
        firmHeader: String? = null,
        tenantHeader: String? = null,
        firmMemberships: List<JwtFirmMembershipClaim> = emptyList(),
    ) = client.get(path) {
        header(
            HttpHeaders.Authorization,
            "Bearer ${testAccessToken(firmMemberships = firmMemberships)}"
        )
        if (firmHeader != null) {
            header(FirmHeaderName, firmHeader)
        }
        if (tenantHeader != null) {
            header(TenantHeaderName, tenantHeader)
        }
    }

    private fun membership(
        firmId: FirmId,
        role: FirmRole,
    ): JwtFirmMembershipClaim = JwtFirmMembershipClaim(
        firmId = firmId,
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
        firmMemberships: List<JwtFirmMembershipClaim>,
    ): String =
        JWT.create()
            .withIssuer(TEST_JWT_ISSUER)
            .withAudience(TEST_JWT_AUDIENCE)
            .withSubject(TEST_USER_ID.toString())
            .withJWTId("test-session-jti")
            .withClaim(JwtClaims.CLAIM_EMAIL, "firm-resolver@test.dokus")
            .withClaim(
                JwtClaims.CLAIM_TENANTS,
                json.encodeToString(ListSerializer(JwtTenantMembershipClaim.serializer()), emptyList())
            )
            .withClaim(
                JwtClaims.CLAIM_FIRMS,
                json.encodeToString(ListSerializer(JwtFirmMembershipClaim.serializer()), firmMemberships)
            )
            .withIssuedAt(Date.from(Instant.now()))
            .withExpiresAt(Date.from(Instant.now().plusSeconds(600)))
            .sign(Algorithm.HMAC256(TEST_JWT_SECRET))

    companion object {
        private val TEST_USER_ID = UserId("00000000-0000-0000-0000-000000000111")
        private const val TEST_JWT_SECRET = "firm-access-resolver-secret"
        private const val TEST_JWT_ISSUER = "firm-resolver-test"
        private const val TEST_JWT_AUDIENCE = "firm-resolver-audience"
    }
}
