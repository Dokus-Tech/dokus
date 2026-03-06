package tech.dokus.backend.routes.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.resources.Resources
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.builtins.ListSerializer
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import tech.dokus.backend.services.auth.FirmInviteTokenService
import tech.dokus.backend.security.FirmHeaderName
import tech.dokus.backend.security.TenantHeaderName
import tech.dokus.database.repository.auth.FirmRepository
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.FirmAccessStatus
import tech.dokus.domain.enums.FirmRole
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Firm
import tech.dokus.domain.model.FirmAccess
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.auth.AcceptFirmInviteRequest
import tech.dokus.domain.model.auth.AcceptFirmInviteResponse
import tech.dokus.domain.model.auth.ConsoleClientSummary
import tech.dokus.domain.model.auth.JwtClaims
import tech.dokus.domain.model.auth.JwtFirmMembershipClaim
import tech.dokus.domain.model.auth.JwtTenantMembershipClaim
import tech.dokus.domain.model.auth.ResolveFirmInviteResponse
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.config.JwtConfig
import tech.dokus.foundation.backend.configure.configureErrorHandling
import tech.dokus.foundation.backend.security.DokusPrincipal
import tech.dokus.foundation.backend.security.JwtValidator
import tech.dokus.foundation.backend.storage.DocumentStorageService
import java.time.Instant
import java.util.Date
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalUuidApi::class)
class ConsoleRoutesTest {

    @Test
    fun `clients endpoint returns active firm client access`() = consoleRoutesTestApplication(
        firmRepository = mockk(),
        tenantRepository = mockk(),
    ) { firmRepository, tenantRepository, _ ->
        val firmId = FirmId.generate()
        val clientTenant = TenantId.generate()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        coEvery { firmRepository.listActiveAccessByFirm(firmId) } returns listOf(
            FirmAccess(
                firmId = firmId,
                tenantId = clientTenant,
                status = FirmAccessStatus.Active,
                grantedByUserId = TEST_USER_ID,
                createdAt = now,
                updatedAt = now,
            )
        )

        coEvery { tenantRepository.findByIds(listOf(clientTenant)) } returns listOf(
            tenant(
                id = clientTenant,
                displayName = "ACME BV",
                vatNumber = "BE0123456789",
            )
        )

        val response = authenticatedGet(
            path = "/api/v1/console/clients",
            firmId = firmId,
            firmMemberships = listOf(
                JwtFirmMembershipClaim(
                    firmId = firmId,
                    role = FirmRole.Owner,
                )
            ),
        )

        assertEquals(HttpStatusCode.OK, response.status)
        val payload = json.decodeFromString(
            ListSerializer(ConsoleClientSummary.serializer()),
            response.bodyAsText()
        )
        assertEquals(1, payload.size)
        assertEquals(clientTenant, payload.first().tenantId)
        assertEquals(DisplayName("ACME BV"), payload.first().companyName)
        assertEquals(VatNumber("BE0123456789"), payload.first().vatNumber)

        coVerify(exactly = 1) { firmRepository.listActiveAccessByFirm(firmId) }
        coVerify(exactly = 1) { tenantRepository.findByIds(listOf(clientTenant)) }
    }

    @Test
    fun `clients endpoint returns 401 without authentication`() = consoleRoutesTestApplication(
        firmRepository = mockk(),
        tenantRepository = mockk(),
    ) { _, _, _ ->
        val response = client.get("/api/v1/console/clients")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `resolve invite endpoint returns firm metadata`() = consoleRoutesTestApplication(
        firmRepository = mockk(),
        tenantRepository = mockk(),
        inviteTokenService = mockk(),
    ) { firmRepository, _, inviteTokenService ->
        val firmId = FirmId.generate()
        val token = "invite-token"
        val expiresAt = Clock.System.now() + 24.hours

        every { inviteTokenService.parse(token) } returns
            tech.dokus.backend.services.auth.FirmInviteTokenPayload(
                firmId = firmId,
                expiresAt = expiresAt,
            )
        coEvery { firmRepository.findById(firmId) } returns firm(
            id = firmId,
            name = "Kantoor Boonen",
            vat = "BE0123456789",
        )

        val response = authenticatedGet(
            path = "/api/v1/console/invite-links/resolve?token=$token",
            firmId = firmId,
            firmMemberships = emptyList(),
        )

        assertEquals(HttpStatusCode.OK, response.status)
        val payload = json.decodeFromString(
            ResolveFirmInviteResponse.serializer(),
            response.bodyAsText()
        )
        assertEquals(firmId, payload.firmId)
        assertEquals(DisplayName("Kantoor Boonen"), payload.firmName)
        assertEquals(VatNumber("BE0123456789"), payload.firmVatNumber)

        verify(exactly = 1) { inviteTokenService.parse(token) }
        coVerify(exactly = 1) { firmRepository.findById(firmId) }
    }

    @Test
    fun `accept invite endpoint activates firm access for tenant header`() = consoleRoutesTestApplication(
        firmRepository = mockk(),
        tenantRepository = mockk(),
        inviteTokenService = mockk(),
    ) { firmRepository, _, inviteTokenService ->
        val firmId = FirmId.generate()
        val tenantId = TenantId.generate()
        val token = "accept-token"

        every { inviteTokenService.parse(token) } returns
            tech.dokus.backend.services.auth.FirmInviteTokenPayload(
                firmId = firmId,
                expiresAt = Clock.System.now() + 24.hours,
            )
        coEvery {
            firmRepository.activateAccess(
                firmId = firmId,
                tenantId = tenantId,
                grantedByUserId = TEST_USER_ID,
            )
        } returns false

        val response = authenticatedPost(
            path = "/api/v1/console/invite-links/accept",
            firmId = firmId,
            tenantId = tenantId,
            body = AcceptFirmInviteRequest(token = token),
            firmMemberships = emptyList(),
            tenantMemberships = listOf(
                JwtTenantMembershipClaim(
                    tenantId = tenantId,
                    role = UserRole.Owner,
                )
            ),
        )

        assertEquals(HttpStatusCode.OK, response.status)
        val payload = json.decodeFromString(
            AcceptFirmInviteResponse.serializer(),
            response.bodyAsText()
        )
        assertEquals(firmId, payload.firmId)
        assertEquals(tenantId, payload.tenantId)
        assertEquals(false, payload.activated)

        verify(exactly = 1) { inviteTokenService.parse(token) }
        coVerify(exactly = 1) {
            firmRepository.activateAccess(
                firmId = firmId,
                tenantId = tenantId,
                grantedByUserId = TEST_USER_ID,
            )
        }
    }

    private fun consoleRoutesTestApplication(
        firmRepository: FirmRepository,
        tenantRepository: TenantRepository,
        inviteTokenService: FirmInviteTokenService = mockk(relaxed = true),
        testBlock: suspend ApplicationTestBuilder.(
            FirmRepository,
            TenantRepository,
            FirmInviteTokenService,
        ) -> Unit
    ) = testApplication {
        application {
            configureConsoleRoutesTestApp(firmRepository, tenantRepository, inviteTokenService)
        }
        testBlock(firmRepository, tenantRepository, inviteTokenService)
    }

    private fun Application.configureConsoleRoutesTestApp(
        firmRepository: FirmRepository,
        tenantRepository: TenantRepository,
        inviteTokenService: FirmInviteTokenService,
    ) {
        val jwtValidator = JwtValidator(testJwtConfig())

        install(Koin) {
            modules(
                module {
                    single<FirmRepository> { firmRepository }
                    single<TenantRepository> { tenantRepository }
                    single<DocumentRepository> { mockk(relaxed = true) }
                    single<DocumentDraftRepository> { mockk(relaxed = true) }
                    single<DocumentIngestionRunRepository> { mockk(relaxed = true) }
                    single<DocumentStorageService> { mockk(relaxed = true) }
                    single<FirmInviteTokenService> { inviteTokenService }
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

    private suspend fun ApplicationTestBuilder.authenticatedGet(
        path: String,
        firmId: FirmId,
        firmMemberships: List<JwtFirmMembershipClaim>,
        tenantMemberships: List<JwtTenantMembershipClaim> = emptyList(),
    ) = client.get(path) {
        header(
            HttpHeaders.Authorization,
            "Bearer ${testAccessToken(firmMemberships, tenantMemberships)}"
        )
        header(FirmHeaderName, firmId.toString())
    }

    private suspend fun ApplicationTestBuilder.authenticatedPost(
        path: String,
        firmId: FirmId,
        tenantId: TenantId,
        body: AcceptFirmInviteRequest,
        firmMemberships: List<JwtFirmMembershipClaim>,
        tenantMemberships: List<JwtTenantMembershipClaim> = emptyList(),
    ) = client.post(path) {
        header(
            HttpHeaders.Authorization,
            "Bearer ${testAccessToken(firmMemberships, tenantMemberships)}"
        )
        header(FirmHeaderName, firmId.toString())
        header(TenantHeaderName, tenantId.toString())
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(json.encodeToString(AcceptFirmInviteRequest.serializer(), body))
    }

    private fun testAccessToken(
        firmMemberships: List<JwtFirmMembershipClaim>,
        tenantMemberships: List<JwtTenantMembershipClaim> = emptyList(),
    ): String =
        JWT.create()
            .withIssuer(TEST_JWT_ISSUER)
            .withAudience(TEST_JWT_AUDIENCE)
            .withSubject(TEST_USER_ID.toString())
            .withJWTId("test-session-jti")
            .withClaim(JwtClaims.CLAIM_EMAIL, "console@test.dokus")
            .withClaim(
                JwtClaims.CLAIM_TENANTS,
                json.encodeToString(ListSerializer(JwtTenantMembershipClaim.serializer()), tenantMemberships)
            )
            .withClaim(
                JwtClaims.CLAIM_FIRMS,
                json.encodeToString(ListSerializer(JwtFirmMembershipClaim.serializer()), firmMemberships)
            )
            .withIssuedAt(Date.from(Instant.now()))
            .withExpiresAt(Date.from(Instant.now().plusSeconds(600)))
            .sign(Algorithm.HMAC256(TEST_JWT_SECRET))

    private fun testJwtConfig(): JwtConfig = JwtConfig(
        issuer = TEST_JWT_ISSUER,
        audience = TEST_JWT_AUDIENCE,
        realm = "test-realm",
        secret = TEST_JWT_SECRET,
        publicKeyPath = null,
        privateKeyPath = null,
        algorithm = "HS256"
    )

    private fun tenant(
        id: TenantId,
        displayName: String,
        vatNumber: String,
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

    private fun firm(
        id: FirmId,
        name: String,
        vat: String,
    ): Firm = Firm(
        id = id,
        name = DisplayName(name),
        vatNumber = VatNumber(vat),
        createdAt = LocalDateTime(2026, 1, 1, 12, 0),
        updatedAt = LocalDateTime(2026, 1, 1, 12, 0),
    )

    companion object {
        private val TEST_USER_ID = UserId("00000000-0000-0000-0000-000000000111")
        private const val TEST_JWT_SECRET = "console-routes-test-secret"
        private const val TEST_JWT_ISSUER = "console-routes-test"
        private const val TEST_JWT_AUDIENCE = "console-routes-test-audience"
    }
}
