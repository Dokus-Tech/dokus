package ai.dokus.foundation.domain.routes

import ai.dokus.foundation.domain.enums.InvitationStatus
import io.ktor.resources.*
import kotlinx.serialization.Serializable

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
}
