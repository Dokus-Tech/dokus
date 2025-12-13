package ai.dokus.foundation.domain.routes

import ai.dokus.foundation.domain.enums.InvitationStatus
import io.ktor.resources.*
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for Team Management API.
 * Base path: /api/v1/team
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
         * /api/v1/team/members/{userId} - Single member operations
         */
        @Serializable
        @Resource("{userId}")
        class Id(val parent: Members, val userId: String) {
            /**
             * PUT /api/v1/team/members/{userId}/role - Update member role
             */
            @Serializable
            @Resource("role")
            class Role(val parent: Id)
        }
    }

    /**
     * POST /api/v1/team/transfer-ownership - Transfer tenant ownership
     */
    @Serializable
    @Resource("transfer-ownership")
    class TransferOwnership(val parent: Team = Team())

    /**
     * GET/POST /api/v1/team/invitations - List or create invitations
     */
    @Serializable
    @Resource("invitations")
    class Invitations(
        val parent: Team = Team(),
        val status: InvitationStatus? = null
    ) {
        /**
         * DELETE /api/v1/team/invitations/{id} - Cancel invitation
         */
        @Serializable
        @Resource("{id}")
        class Id(val parent: Invitations, val id: String)
    }
}
