package ai.dokus.foundation.domain.routes

import io.ktor.resources.*
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Account API (authenticated endpoints).
 * Base path: /api/v1/account
 */
@Serializable
@Resource("/api/v1/account")
class Account {
    /**
     * GET /api/v1/account/me - Get current user info
     */
    @Serializable
    @Resource("me")
    class Me(val parent: Account = Account())

    /**
     * POST /api/v1/account/select-tenant - Select active tenant
     */
    @Serializable
    @Resource("select-tenant")
    class SelectTenant(val parent: Account = Account())

    /**
     * POST /api/v1/account/logout - Logout user
     */
    @Serializable
    @Resource("logout")
    class Logout(val parent: Account = Account())

    /**
     * POST /api/v1/account/deactivate - Deactivate user account
     */
    @Serializable
    @Resource("deactivate")
    class Deactivate(val parent: Account = Account())

    /**
     * POST /api/v1/account/resend-verification - Resend verification email
     */
    @Serializable
    @Resource("resend-verification")
    class ResendVerification(val parent: Account = Account())
}
