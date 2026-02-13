package tech.dokus.domain.routes

import io.ktor.resources.Resource
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
     * Legacy endpoint retained for contract stability (no-op on server)
     */
    @Serializable
    @Resource("email-verifications")
    class EmailVerifications(val parent: Account = Account())
}
