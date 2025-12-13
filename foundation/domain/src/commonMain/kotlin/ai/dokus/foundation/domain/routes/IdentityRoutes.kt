package ai.dokus.foundation.domain.routes

import io.ktor.resources.*
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Identity API (unauthenticated endpoints).
 * Base path: /api/v1/identity
 */
@Serializable
@Resource("/api/v1/identity")
class Identity {
    /**
     * POST /api/v1/identity/login - Authenticate user
     */
    @Serializable
    @Resource("login")
    class Login(val parent: Identity = Identity())

    /**
     * POST /api/v1/identity/register - Register new user
     */
    @Serializable
    @Resource("register")
    class Register(val parent: Identity = Identity())

    /**
     * POST /api/v1/identity/refresh - Refresh access token
     */
    @Serializable
    @Resource("refresh")
    class Refresh(val parent: Identity = Identity())

    /**
     * POST /api/v1/identity/request-password-reset - Request password reset
     */
    @Serializable
    @Resource("request-password-reset")
    class RequestPasswordReset(val parent: Identity = Identity())

    /**
     * POST /api/v1/identity/reset-password/{token} - Reset password with token
     */
    @Serializable
    @Resource("reset-password/{token}")
    class ResetPassword(val parent: Identity = Identity(), val token: String)

    /**
     * POST /api/v1/identity/verify-email/{token} - Verify email address
     */
    @Serializable
    @Resource("verify-email/{token}")
    class VerifyEmail(val parent: Identity = Identity(), val token: String)
}
