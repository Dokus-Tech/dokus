package tech.dokus.domain.routes

import io.ktor.resources.*
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.InvitationStatus
import tech.dokus.domain.ids.FirmId

/**
 * Type-safe route definitions for Team Management API.
 * Base path: /api/v1/team
 *
 * SECURITY: All operations are scoped to the authenticated user's tenant via JWT.
 */
@Serializable
@Resource("/api/v1/team")
class Team {
    /**
     * GET /api/v1/team/members - List team members
     */
    @Serializable
    @Resource("members")
    class Members(val parent: Team = Team()) {
        /**
         * GET/DELETE /api/v1/team/members/{userId}
         * GET - Get member details
         * DELETE - Remove member from team
         */
        @Serializable
        @Resource("{userId}")
        class Id(val parent: Members, val userId: String) {
            /**
             * GET/PUT /api/v1/team/members/{userId}/role
             * GET - Get member's role
             * PUT - Update member's role
             */
            @Serializable
            @Resource("role")
            class Role(val parent: Id)
        }
    }

    /**
     * GET/PUT /api/v1/team/owner
     * GET - Get current owner
     * PUT - Transfer ownership to another member
     */
    @Serializable
    @Resource("owner")
    class Owner(val parent: Team = Team())

    /**
     * GET/POST /api/v1/team/invitations
     * GET - List invitations (filter by status)
     * POST - Create new invitation
     */
    @Serializable
    @Resource("invitations")
    class Invitations(
        val parent: Team = Team(),
        val status: InvitationStatus? = null
    ) {
        /**
         * GET/DELETE /api/v1/team/invitations/{id}
         * GET - Get invitation details
         * DELETE - Cancel/revoke invitation
         */
        @Serializable
        @Resource("{id}")
        class Id(val parent: Invitations, val id: String)
    }

    /**
     * Company-manager firm access management for bookkeepers.
     */
    @Serializable
    @Resource("bookkeepers")
    class Bookkeepers(val parent: Team = Team()) {
        /**
         * GET /api/v1/team/bookkeepers/search?query=...&limit=...
         * Search existing firms by name/VAT.
         */
        @Serializable
        @Resource("search")
        class Search(
            val parent: Bookkeepers = Bookkeepers(),
            val query: String = "",
            val limit: Int = 20,
        )

        /**
         * GET/POST /api/v1/team/bookkeepers/access
         * List current active firm access relations for the tenant.
         * Grant firm access to the tenant.
         */
        @Serializable
        @Resource("access")
        class Access(val parent: Bookkeepers = Bookkeepers()) {
            /**
             * DELETE /api/v1/team/bookkeepers/access/{firmId}
             * Revoke active firm access.
             */
            @Serializable
            @Resource("{firmId}")
            class ByFirm(
                val parent: Access = Access(),
                val firmId: FirmId,
            )
        }
    }
}
