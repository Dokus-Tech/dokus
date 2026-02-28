package tech.dokus.backend.auth

import com.auth0.jwt.JWT
import org.junit.jupiter.api.Test
import tech.dokus.domain.enums.Permission
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.auth.JwtClaims
import tech.dokus.domain.model.auth.TenantScope
import tech.dokus.foundation.backend.config.JwtConfig
import tech.dokus.foundation.backend.security.DokusPrincipal
import tech.dokus.foundation.backend.security.JwtGenerator
import tech.dokus.foundation.backend.security.JwtValidator
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JwtIdentityOnlyTest {

    @Test
    fun `access token does not include tenant scoped claims`() {
        val generator = JwtGenerator(testJwtConfig())
        val userId = UserId.generate()
        val claims = generator.generateClaims(
            userId = userId,
            email = "identity-only@test.dokus",
            tenant = TenantScope(
                tenantId = TenantId.generate(),
                permissions = setOf(Permission.InvoicesRead, Permission.InvoicesEdit),
                subscriptionTier = SubscriptionTier.One,
                role = UserRole.Owner
            )
        )

        val token = generator.generateTokens(claims).accessToken
        val decoded = JWT.decode(token)

        assertEquals(userId.toString(), decoded.subject)
        assertEquals("identity-only@test.dokus", decoded.getClaim(JwtClaims.CLAIM_EMAIL).asString())
        assertNotNull(decoded.id)
        assertNull(decoded.getClaim(JwtClaims.CLAIM_TENANT_ID).asString())
        assertNull(decoded.getClaim(JwtClaims.CLAIM_ROLE).asString())
        assertNull(decoded.getClaim(JwtClaims.CLAIM_SUBSCRIPTION_TIER).asString())
        assertNull(decoded.getClaim(JwtClaims.CLAIM_PERMISSIONS).asList(String::class.java))
    }

    @Test
    fun `validator extracts identity only auth info from token`() {
        val config = testJwtConfig()
        val generator = JwtGenerator(config)
        val validator = JwtValidator(config)
        val userId = UserId.generate()
        val claims = generator.generateClaims(
            userId = userId,
            email = "validator@test.dokus",
            tenant = null
        )

        val token = generator.generateTokens(claims).accessToken
        val authInfo = validator.validateAndExtract(token)

        assertNotNull(authInfo)
        assertEquals(userId, authInfo.userId)
        assertEquals("validator@test.dokus", authInfo.email)
        assertEquals("validator", authInfo.name)
        assertTrue(authInfo.globalRoles.isEmpty())
        assertEquals(claims.jti, authInfo.sessionJti)

        val principal = DokusPrincipal.fromAuthInfo(authInfo)
        assertEquals(userId, principal.userId)
        assertTrue(principal.globalRoles.isEmpty())
        assertEquals(claims.jti, principal.sessionJti)
    }

    private fun testJwtConfig(): JwtConfig = JwtConfig(
        issuer = "test-issuer",
        audience = "test-audience",
        realm = "test-realm",
        secret = "test-secret-key",
        publicKeyPath = null,
        privateKeyPath = null,
        algorithm = "HS256"
    )
}
