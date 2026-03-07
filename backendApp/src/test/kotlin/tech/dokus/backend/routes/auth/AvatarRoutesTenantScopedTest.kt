package tech.dokus.backend.routes.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import tech.dokus.backend.services.business.BusinessProfileService
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.common.Thumbnail
import tech.dokus.domain.model.TenantMembership
import tech.dokus.domain.model.auth.JwtClaims
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.config.JwtConfig
import tech.dokus.foundation.backend.configure.configureErrorHandling
import tech.dokus.foundation.backend.security.DokusPrincipal
import tech.dokus.foundation.backend.security.JwtValidator
import tech.dokus.foundation.backend.storage.AvatarStorageService
import java.time.Instant
import java.util.Date
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class AvatarRoutesTenantScopedTest {

    @Test
    fun `tenant-scoped avatar endpoint succeeds for active member without tenant header`() = avatarRoutesTestApplication(
        userRepository = mockk(),
        tenantRepository = mockk(),
        avatarStorageService = mockk(),
        businessProfileService = mockk(relaxed = true)
    ) { userRepository, tenantRepository, avatarStorageService, _ ->
        val tenantId = TenantId.generate()
        val imageBytes = byteArrayOf(1, 2, 3, 4)
        coEvery { userRepository.getMembership(TEST_USER_ID, tenantId) } returns membership(tenantId)
        every { avatarStorageService.normalizeSize("small") } returns "small"
        coEvery { tenantRepository.getAvatarStorageKey(tenantId) } returns "avatars/test"
        coEvery { avatarStorageService.getAvatarBytes("avatars/test", "small") } returns imageBytes

        val response = authenticatedGet("/api/v1/tenants/$tenantId/avatar/small.webp")

        assertEquals(HttpStatusCode.OK, response.status)
        assertContentEquals(imageBytes, response.bodyAsBytes())
        coVerify(exactly = 1) { userRepository.getMembership(TEST_USER_ID, tenantId) }
        coVerify(exactly = 1) { tenantRepository.getAvatarStorageKey(tenantId) }
    }

    @Test
    fun `tenant avatar metadata endpoint returns canonical thumbnail for active member`() = avatarRoutesTestApplication(
        userRepository = mockk(),
        tenantRepository = mockk(),
        avatarStorageService = mockk(),
        businessProfileService = mockk(relaxed = true)
    ) { userRepository, tenantRepository, avatarStorageService, businessProfileService ->
        val tenantId = TenantId.generate()
        coEvery { userRepository.getMembership(TEST_USER_ID, tenantId) } returns membership(tenantId)
        coEvery { tenantRepository.getAvatarStorageKey(tenantId) } returns "avatars/tenants/$tenantId/test"
        coEvery { avatarStorageService.getAvatarUrls("avatars/tenants/$tenantId/test") } returns Thumbnail(
            small = "https://cdn.example/tenant-small.webp",
            medium = "https://cdn.example/tenant-medium.webp",
            large = "https://cdn.example/tenant-large.webp"
        )
        coEvery {
            businessProfileService.buildTenantAvatarThumbnail(tenantId)
        } returns Thumbnail(
            small = "https://cdn.example/tenant-small.webp",
            medium = "https://cdn.example/tenant-medium.webp",
            large = "https://cdn.example/tenant-large.webp"
        )

        val response = authenticatedGet("/api/v1/tenants/$tenantId/avatar")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("https://cdn.example/tenant-small.webp"))
        coVerify(exactly = 1) { userRepository.getMembership(TEST_USER_ID, tenantId) }
        coVerify(exactly = 1) { tenantRepository.getAvatarStorageKey(tenantId) }
    }

    @Test
    fun `tenant-scoped avatar endpoint returns forbidden for non-member`() = avatarRoutesTestApplication(
        userRepository = mockk(),
        tenantRepository = mockk(),
        avatarStorageService = mockk(),
        businessProfileService = mockk(relaxed = true)
    ) { userRepository, tenantRepository, _, _ ->
        val tenantId = TenantId.generate()
        coEvery { userRepository.getMembership(TEST_USER_ID, tenantId) } returns null

        val response = authenticatedGet("/api/v1/tenants/$tenantId/avatar/small.webp")

        assertEquals(HttpStatusCode.Forbidden, response.status)
        coVerify(exactly = 0) { tenantRepository.getAvatarStorageKey(any()) }
    }

    @Test
    fun `tenant-scoped avatar endpoint returns not found when avatar is missing`() = avatarRoutesTestApplication(
        userRepository = mockk(),
        tenantRepository = mockk(),
        avatarStorageService = mockk(),
        businessProfileService = mockk(relaxed = true)
    ) { userRepository, tenantRepository, avatarStorageService, _ ->
        val tenantId = TenantId.generate()
        coEvery { userRepository.getMembership(TEST_USER_ID, tenantId) } returns membership(tenantId)
        every { avatarStorageService.normalizeSize("small") } returns "small"
        coEvery { tenantRepository.getAvatarStorageKey(tenantId) } returns null

        val response = authenticatedGet("/api/v1/tenants/$tenantId/avatar/small.webp")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    private fun avatarRoutesTestApplication(
        userRepository: UserRepository,
        tenantRepository: TenantRepository,
        avatarStorageService: AvatarStorageService,
        businessProfileService: BusinessProfileService,
        testBlock: suspend ApplicationTestBuilder.(
            UserRepository,
            TenantRepository,
            AvatarStorageService,
            BusinessProfileService
        ) -> Unit
    ) = testApplication {
        application {
            configureAvatarRoutesTestApp(
                userRepository = userRepository,
                tenantRepository = tenantRepository,
                avatarStorageService = avatarStorageService,
                businessProfileService = businessProfileService
            )
        }
        testBlock(userRepository, tenantRepository, avatarStorageService, businessProfileService)
    }

    private fun Application.configureAvatarRoutesTestApp(
        userRepository: UserRepository,
        tenantRepository: TenantRepository,
        avatarStorageService: AvatarStorageService,
        businessProfileService: BusinessProfileService
    ) {
        val jwtValidator = JwtValidator(testJwtConfig())

        install(Koin) {
            modules(
                module {
                    single<UserRepository> { userRepository }
                    single<TenantRepository> { tenantRepository }
                    single<AvatarStorageService> { avatarStorageService }
                    single<BusinessProfileService> { businessProfileService }
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
            avatarRoutes()
        }
    }

    private suspend fun ApplicationTestBuilder.authenticatedGet(path: String) = client.get(path) {
        header(HttpHeaders.Authorization, "Bearer ${testAccessToken()}")
    }

    private fun membership(
        tenantId: TenantId,
        role: UserRole = UserRole.Owner,
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
            .withClaim(JwtClaims.CLAIM_EMAIL, "avatar@test.dokus")
            .withIssuedAt(Date.from(Instant.now()))
            .withExpiresAt(Date.from(Instant.now().plusSeconds(600)))
            .sign(Algorithm.HMAC256(TEST_JWT_SECRET))

    companion object {
        private val TEST_USER_ID = UserId("00000000-0000-0000-0000-000000000111")
        private const val TEST_JWT_SECRET = "avatar-routes-test-secret"
        private const val TEST_JWT_ISSUER = "avatar-routes-test"
        private const val TEST_JWT_AUDIENCE = "avatar-routes-test-audience"
    }
}
