package ai.dokus.foundation.domain.routes

import io.ktor.resources.*
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Identity API (unauthenticated endpoints).
 * Base path: /api/v1/identity
 *
 * Note: Authentication endpoints (login, register, refresh) use verb-based naming
 * as this is industry-standard convention for auth endpoints.
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
     * GET/POST /api/v1/identity/password-resets
     * POST - Request a password reset (creates reset token)
     */
    @Serializable
    @Resource("password-resets")
    class PasswordResets(val parent: Identity = Identity()) {
        /**
         * PATCH /api/v1/identity/password-resets/{token}
         * Completes the password reset with new password
         */
        @Serializable
        @Resource("{token}")
        class ByToken(val parent: PasswordResets, val token: String)
    }

    /**
     * /api/v1/identity/email-verifications
     */
    @Serializable
    @Resource("email-verifications")
    class EmailVerifications(val parent: Identity = Identity()) {
        /**
         * PATCH /api/v1/identity/email-verifications/{token}
         * Verifies the email address using the token
         */
        @Serializable
        @Resource("{token}")
        class ByToken(val parent: EmailVerifications, val token: String)
    }
}
