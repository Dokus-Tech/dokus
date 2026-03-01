package tech.dokus.backend.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.TenantMembership
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
    fun `tenant route without tenant header returns bad request`() = tenantAccessTestApplication(
        userRepository = mockk(relaxed = true)
    ) { userRepository ->
        val response = authenticatedGet("/tenant-required")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        coVerify(exactly = 0) { userRepository.getMembership(any(), any()) }
    }

    @Test
    fun `tenant route with malformed tenant header returns bad request`() = tenantAccessTestApplication(
        userRepository = mockk(relaxed = true)
    ) { userRepository ->
        val response = authenticatedGet(
            "/tenant-required",
            tenantHeader = "not-a-uuid"
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
        coVerify(exactly = 0) { userRepository.getMembership(any(), any()) }
    }

    @Test
    fun `tenant route with no membership returns forbidden`() = tenantAccessTestApplication(
        userRepository = mockk()
    ) { userRepository ->
        val tenantId = TenantId.generate()
        coEvery { userRepository.getMembership(TEST_USER_ID, tenantId) } returns null

        val response = authenticatedGet(
            "/tenant-required",
            tenantHeader = tenantId.toString()
        )

        assertEquals(HttpStatusCode.Forbidden, response.status)
        coVerify(exactly = 1) { userRepository.getMembership(TEST_USER_ID, tenantId) }
    }

    @Test
    fun `tenant route with inactive membership returns forbidden`() = tenantAccessTestApplication(
        userRepository = mockk()
    ) { userRepository ->
        val tenantId = TenantId.generate()
        coEvery { userRepository.getMembership(TEST_USER_ID, tenantId) } returns membership(
            tenantId = tenantId,
            role = UserRole.Admin,
            isActive = false
        )

        val response = authenticatedGet(
            "/tenant-required",
            tenantHeader = tenantId.toString()
        )

        assertEquals(HttpStatusCode.Forbidden, response.status)
        coVerify(exactly = 1) { userRepository.getMembership(TEST_USER_ID, tenantId) }
    }

    @Test
    fun `active membership attaches tenant access and memoizes resolver per call`() = tenantAccessTestApplication(
        userRepository = mockk()
    ) { userRepository ->
        val tenantId = TenantId.generate()
        coEvery { userRepository.getMembership(TEST_USER_ID, tenantId) } returns membership(
            tenantId = tenantId,
            role = UserRole.Owner
        )

        val response = authenticatedGet(
            "/tenant-required-twice",
            tenantHeader = tenantId.toString()
        )

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains(tenantId.toString()))
        assertTrue(body.contains(UserRole.Owner.dbValue))
        coVerify(exactly = 1) { userRepository.getMembership(TEST_USER_ID, tenantId) }
    }

    @Test
    fun `tenant id path parameter fallback is accepted when header is omitted`() = tenantAccessTestApplication(
        userRepository = mockk()
    ) { userRepository ->
        val tenantId = TenantId.generate()
        coEvery { userRepository.getMembership(TEST_USER_ID, tenantId) } returns membership(
            tenantId = tenantId,
            role = UserRole.Viewer
        )

        val response = authenticatedGet("/tenant-by-path/$tenantId")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains(UserRole.Viewer.dbValue))
        coVerify(exactly = 1) { userRepository.getMembership(TEST_USER_ID, tenantId) }
    }

    @Test
    fun `authenticated non tenant route succeeds without tenant context`() = tenantAccessTestApplication(
        userRepository = mockk(relaxed = true)
    ) { userRepository ->
        val response = authenticatedGet("/non-tenant")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(TEST_USER_ID.toString(), response.bodyAsText())
        coVerify(exactly = 0) { userRepository.getMembership(any(), any()) }
    }

    private fun tenantAccessTestApplication(
        userRepository: UserRepository,
        testBlock: suspend ApplicationTestBuilder.(UserRepository) -> Unit
    ) = testApplication {
        application {
            configureTenantAccessTestApp(userRepository)
        }
        testBlock(userRepository)
    }

    private fun Application.configureTenantAccessTestApp(userRepository: UserRepository) {
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
                    val access = requireTenantAccess(userRepository)
                    call.respondText("${access.tenantId}:${access.role.dbValue}")
                }
                get("/tenant-required-twice") {
                    val first = requireTenantAccess(userRepository)
                    val second = requireTenantAccess(userRepository)
                    call.respondText("${first.tenantId}:${second.role.dbValue}")
                }
                get("/tenant-by-path/{tenantId}") {
                    val access = requireTenantAccess(userRepository)
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
        tenantHeader: String? = null
    ) = client.get(path) {
        header(HttpHeaders.Authorization, "Bearer ${testAccessToken()}")
        if (tenantHeader != null) {
            header(TenantHeaderName, tenantHeader)
        }
    }

    private fun membership(
        tenantId: TenantId,
        role: UserRole,
        isActive: Boolean = true
    ): TenantMembership {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        return TenantMembership(
            userId = TEST_USER_ID,
            tenantId = tenantId,
            role = role,
            isActive = isActive,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun testJwtConfig(): JwtConfig = JwtConfig(
        issuer = TEST_JWT_ISSUER,
        audience = TEST_JWT_AUDIENCE,
        realm = "test-realm",
        secret = TEST_JWT_SECRET,
        publicKeyPath = null,
        privateKeyPath = null,
        algorithm = "HS256"
    )

    private fun testAccessToken(): String =
        JWT.create()
            .withIssuer(TEST_JWT_ISSUER)
            .withAudience(TEST_JWT_AUDIENCE)
            .withSubject(TEST_USER_ID.toString())
            .withJWTId("test-session-jti")
            .withClaim("email", "tenant-resolver@test.dokus")
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
