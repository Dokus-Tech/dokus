package tech.dokus.domain.routes

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.SessionId

/**
 * Type-safe route definitions for Account API (authenticated endpoints).
 * Base path: /api/v1/account
 *
 * SECURITY: All operations require authentication.
 */
@Serializable
@Resource("/api/v1/account")
class Account {
    /**
     * GET /api/v1/account/me
     * GET - Get current user info
     */
    @Serializable
    @Resource("me")
    class Me(val parent: Account = Account())

    /**
     * PATCH /api/v1/account/profile
     * PATCH - Update user profile (first name, last name)
     */
    @Serializable
    @Resource("profile")
    class Profile(val parent: Account = Account())

    /**
     * POST /api/v1/account/deactivate
     * POST - Deactivate user account
     */
    @Serializable
    @Resource("deactivate")
    class Deactivate(val parent: Account = Account())

    /**
     * GET/PUT /api/v1/account/active-tenant
     * GET - Get currently active tenant
     * PUT - Set active tenant
     */
    @Serializable
    @Resource("active-tenant")
    class ActiveTenant(val parent: Account = Account())

    /**
     * POST /api/v1/account/logout - Logout user
     * Note: Verb-based naming is industry standard for auth operations
     */
    @Serializable
    @Resource("logout")
    class Logout(val parent: Account = Account())

    /**
     * POST /api/v1/account/email-verifications
     * Resend verification email for the current authenticated user.
     */
    @Serializable
    @Resource("email-verifications")
    class EmailVerifications(val parent: Account = Account())

    /**
     * POST /api/v1/account/password
     * Change account password for the authenticated user.
     */
    @Serializable
    @Resource("password")
    class Password(val parent: Account = Account())

    /**
     * Session management endpoints.
     * GET /api/v1/account/sessions - List active sessions.
     */
    @Serializable
    @Resource("sessions")
    class Sessions(val parent: Account = Account()) {
        /**
         * DELETE /api/v1/account/sessions/{sessionId}
         * Revoke a specific session.
         */
        @Serializable
        @Resource("{sessionId}")
        class ById(
            val parent: Sessions,
            val sessionId: SessionId
        )

        /**
         * POST /api/v1/account/sessions/revoke-others
         * Revoke all sessions except the current one.
         */
        @Serializable
        @Resource("revoke-others")
        class RevokeOthers(val parent: Sessions = Sessions())
    }
}
