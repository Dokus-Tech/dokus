package tech.dokus.backend.routes.auth

import tech.dokus.domain.enums.InvitationStatus
import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.CreateInvitationRequest
import tech.dokus.domain.model.TransferOwnershipRequest
import tech.dokus.domain.model.UpdateMemberRoleRequest
import tech.dokus.domain.routes.Team
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import tech.dokus.backend.services.auth.TeamService
import tech.dokus.foundation.ktor.security.authenticateJwt
import tech.dokus.foundation.ktor.security.dokusPrincipal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Team management routes using Ktor Type-Safe Routing:
 * - List team members
 * - Invite team members
 * - Update member roles
 * - Remove team members
 * - Transfer ownership
 * - Manage invitations
 *
 * All routes require Owner role except listing members.
 */
@OptIn(ExperimentalUuidApi::class)
internal fun Route.teamRoutes() {
    val teamService by inject<TeamService>()

    authenticateJwt {

        // ================================================================
        // TEAM MEMBERS
        // ================================================================

        /**
         * GET /api/v1/team/members
         * List all active team members in current tenant.
         */
        get<Team.Members> {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()

            val members = teamService.listTeamMembers(tenantId)
            call.respond(HttpStatusCode.OK, members)
        }

        /**
         * PUT /api/v1/team/members/{userId}/role
         * Update a team member's role.
         * Requires Owner role.
         */
        put<Team.Members.Id.Role> { route ->
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()

            // Verify owner role
            if (!teamService.verifyOwnerRole(principal.userId, tenantId)) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    "Only the workspace Owner can manage team members"
                )
                return@put
            }

            val targetUserId = UserId(Uuid.parse(route.parent.userId))
            val request = call.receive<UpdateMemberRoleRequest>()

            teamService.updateMemberRole(tenantId, targetUserId, request.role, principal.userId)
                .onSuccess {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Role updated successfully"))
                }
                .onFailure { error ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (error.message ?: "Failed to update role"))
                    )
                }
        }

        /**
         * DELETE /api/v1/team/members/{userId}
         * Remove a team member from the workspace.
         * Requires Owner role.
         */
        delete<Team.Members.Id> { route ->
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()

            // Verify owner role
            if (!teamService.verifyOwnerRole(principal.userId, tenantId)) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    "Only the workspace Owner can manage team members"
                )
                return@delete
            }

            val targetUserId = UserId(Uuid.parse(route.userId))

            teamService.removeMember(tenantId, targetUserId, principal.userId)
                .onSuccess {
                    call.respond(HttpStatusCode.NoContent)
                }
                .onFailure { error ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (error.message ?: "Failed to remove member"))
                    )
                }
        }

        // ================================================================
        // OWNERSHIP
        // ================================================================

        /**
         * PUT /api/v1/team/owner
         * Transfer workspace ownership to another member.
         * Requires Owner role.
         */
        put<Team.Owner> {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()

            // Verify owner role
            if (!teamService.verifyOwnerRole(principal.userId, tenantId)) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    "Only the workspace Owner can transfer ownership"
                )
                return@put
            }

            val request = call.receive<TransferOwnershipRequest>()

            teamService.transferOwnership(tenantId, request.newOwnerId, principal.userId)
                .onSuccess {
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf("message" to "Ownership transferred successfully")
                    )
                }
                .onFailure { error ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (error.message ?: "Failed to transfer ownership"))
                    )
                }
        }

        // ================================================================
        // INVITATIONS
        // ================================================================

        /**
         * GET /api/v1/team/invitations
         * List all invitations for current tenant.
         * Query param 'status' supported in route definition.
         */
        get<Team.Invitations> { route ->
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()

            val invitations = if (route.status == InvitationStatus.Pending) {
                teamService.listPendingInvitations(tenantId)
            } else {
                // For now, only pending is supported via service
                teamService.listPendingInvitations(tenantId)
            }

            call.respond(HttpStatusCode.OK, invitations)
        }

        /**
         * POST /api/v1/team/invitations
         * Create a new team invitation.
         * Requires Owner role.
         */
        post<Team.Invitations> {
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()

            // Verify owner role
            if (!teamService.verifyOwnerRole(principal.userId, tenantId)) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    "Only the workspace Owner can invite team members"
                )
                return@post
            }

            val request = call.receive<CreateInvitationRequest>()

            teamService.createInvitation(tenantId, principal.userId, request)
                .onSuccess { invitation ->
                    call.respond(HttpStatusCode.Created, invitation)
                }
                .onFailure { error ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (error.message ?: "Failed to create invitation"))
                    )
                }
        }

        /**
         * DELETE /api/v1/team/invitations/{id}
         * Cancel/revoke a pending invitation.
         * Requires Owner role.
         */
        delete<Team.Invitations.Id> { route ->
            val principal = dokusPrincipal
            val tenantId = principal.requireTenantId()

            // Verify owner role
            if (!teamService.verifyOwnerRole(principal.userId, tenantId)) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    "Only the workspace Owner can manage invitations"
                )
                return@delete
            }

            val invitationId = InvitationId(Uuid.parse(route.id))

            teamService.cancelInvitation(invitationId, tenantId)
                .onSuccess {
                    call.respond(HttpStatusCode.NoContent)
                }
                .onFailure { error ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (error.message ?: "Failed to cancel invitation"))
                    )
                }
        }
    }
}
