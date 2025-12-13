package ai.dokus.foundation.domain.routes

import io.ktor.resources.*
import kotlinx.serialization.Serializable

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
     * GET/PATCH /api/v1/account/me
     * GET - Get current user info
     * PATCH - Update user info (including deactivation via status field)
     */
    @Serializable
    @Resource("me")
    class Me(val parent: Account = Account())

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
     * Creates a new email verification request (resends verification email)
     */
    @Serializable
    @Resource("email-verifications")
    class EmailVerifications(val parent: Account = Account())
}
