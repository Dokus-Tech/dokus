package tech.dokus.backend.routes.auth

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
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.resources.Resources
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.builtins.ListSerializer
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import io.ktor.serialization.kotlinx.json.json
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.TenantMembership
import tech.dokus.domain.model.auth.ConsoleClientSummary
import tech.dokus.domain.model.auth.JwtClaims
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.config.JwtConfig
import tech.dokus.foundation.backend.configure.configureErrorHandling
import tech.dokus.foundation.backend.security.DokusPrincipal
import tech.dokus.foundation.backend.security.JwtValidator
import java.time.Instant
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class ConsoleRoutesTest {

    @Test
    fun `clients endpoint returns only active accountant tenants`() = consoleRoutesTestApplication(
        userRepository = mockk(),
        tenantRepository = mockk()
    ) { userRepository, tenantRepository ->
        val accountantTenant = TenantId.generate()
        val nonAccountantTenant = TenantId.generate()
        val inactiveAccountantTenant = TenantId.generate()
        coEvery { userRepository.getUserTenants(TEST_USER_ID) } returns listOf(
            membership(accountantTenant, UserRole.Accountant, isActive = true),
            membership(nonAccountantTenant, UserRole.Editor, isActive = true),
            membership(inactiveAccountantTenant, UserRole.Accountant, isActive = false)
        )

        coEvery { tenantRepository.findByIds(listOf(accountantTenant)) } returns listOf(
            tenant(
                id = accountantTenant,
                displayName = "ACME Accounting",
                vatNumber = "BE0123456789"
            )
        )

        val response = authenticatedGet("/api/v1/console/clients")

        assertEquals(HttpStatusCode.OK, response.status)
        val payload = json.decodeFromString(
            ListSerializer(ConsoleClientSummary.serializer()),
            response.bodyAsText()
        )
        assertEquals(1, payload.size)
        assertEquals(accountantTenant, payload.first().tenantId)
        assertEquals(DisplayName("ACME Accounting"), payload.first().companyName)
        assertEquals(VatNumber("BE0123456789"), payload.first().vatNumber)

        coVerify(exactly = 1) { userRepository.getUserTenants(TEST_USER_ID) }
        coVerify(exactly = 1) { tenantRepository.findByIds(listOf(accountantTenant)) }
    }

    @Test
    fun `clients endpoint returns exact payload without tenant header`() = consoleRoutesTestApplication(
        userRepository = mockk(),
        tenantRepository = mockk()
    ) { userRepository, tenantRepository ->
        val accountantTenant = TenantId.generate()
        coEvery { userRepository.getUserTenants(TEST_USER_ID) } returns listOf(
            membership(accountantTenant, UserRole.Accountant, isActive = true)
        )
        coEvery { tenantRepository.findByIds(listOf(accountantTenant)) } returns listOf(
            tenant(
                id = accountantTenant,
                displayName = "No Header LLC",
                vatNumber = "BE1111222233"
            )
        )

        val response = authenticatedGet("/api/v1/console/clients")

        assertEquals(HttpStatusCode.OK, response.status)
        val payload = json.decodeFromString(
            ListSerializer(ConsoleClientSummary.serializer()),
            response.bodyAsText()
        )
        assertEquals(1, payload.size)
        assertEquals(accountantTenant, payload.first().tenantId)
        assertEquals(DisplayName("No Header LLC"), payload.first().companyName)
        assertEquals(VatNumber("BE1111222233"), payload.first().vatNumber)
    }

    @Test
    fun `clients endpoint returns 401 without authentication`() = consoleRoutesTestApplication(
        userRepository = mockk(),
        tenantRepository = mockk()
    ) { _, _ ->
        val response = client.get("/api/v1/console/clients")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    private fun consoleRoutesTestApplication(
        userRepository: UserRepository,
        tenantRepository: TenantRepository,
        testBlock: suspend ApplicationTestBuilder.(UserRepository, TenantRepository) -> Unit
    ) = testApplication {
        application {
            configureConsoleRoutesTestApp(userRepository, tenantRepository)
        }
        testBlock(userRepository, tenantRepository)
    }

    private fun Application.configureConsoleRoutesTestApp(
        userRepository: UserRepository,
        tenantRepository: TenantRepository
    ) {
        val jwtValidator = JwtValidator(testJwtConfig())

        install(Koin) {
            modules(
                module {
                    single<UserRepository> { userRepository }
                    single<TenantRepository> { tenantRepository }
                }
            )
        }

        install(Resources)
        install(ContentNegotiation) {
            json(json)
        }
        configureErrorHandling()

        install(Authentication) {
            jwt("auth-jwt") {
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
            consoleRoutes()
        }
    }

    private suspend fun ApplicationTestBuilder.authenticatedGet(path: String) = client.get(path) {
        header(HttpHeaders.Authorization, "Bearer ${testAccessToken()}")
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

    private fun tenant(
        id: TenantId,
        displayName: String,
        vatNumber: String
    ): Tenant = Tenant(
        id = id,
        type = TenantType.Company,
        legalName = LegalName(displayName),
        displayName = DisplayName(displayName),
        subscription = SubscriptionTier.Core,
        status = TenantStatus.Active,
        language = Language.En,
        vatNumber = VatNumber(vatNumber),
        trialEndsAt = null,
        subscriptionStartedAt = null,
        createdAt = LocalDateTime(2026, 1, 1, 12, 0),
        updatedAt = LocalDateTime(2026, 1, 1, 12, 0),
        avatar = null
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

    private fun testAccessToken(): String =
        JWT.create()
            .withIssuer(TEST_JWT_ISSUER)
            .withAudience(TEST_JWT_AUDIENCE)
            .withSubject(TEST_USER_ID.toString())
            .withJWTId("test-session-jti")
            .withClaim(JwtClaims.CLAIM_EMAIL, "console@test.dokus")
            .withIssuedAt(Date.from(Instant.now()))
            .withExpiresAt(Date.from(Instant.now().plusSeconds(600)))
            .sign(Algorithm.HMAC256(TEST_JWT_SECRET))

    companion object {
        private val TEST_USER_ID = UserId("00000000-0000-0000-0000-000000000111")
        private const val TEST_JWT_SECRET = "console-routes-test-secret"
        private const val TEST_JWT_ISSUER = "console-routes-test"
        private const val TEST_JWT_AUDIENCE = "console-routes-test-audience"
    }
}
