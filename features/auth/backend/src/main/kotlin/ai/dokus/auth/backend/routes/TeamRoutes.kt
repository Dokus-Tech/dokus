package ai.dokus.auth.backend.routes

import ai.dokus.auth.backend.services.TeamService
import ai.dokus.foundation.domain.enums.InvitationStatus
import ai.dokus.foundation.domain.ids.InvitationId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.CreateInvitationRequest
import ai.dokus.foundation.domain.model.TransferOwnershipRequest
import ai.dokus.foundation.domain.model.UpdateMemberRoleRequest
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.dokusPrincipal
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Team management routes:
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
fun Route.teamRoutes() {
    val teamService by inject<TeamService>()

    route("/api/v1/team") {
        authenticateJwt {

            // ================================================================
            // TEAM MEMBERS
            // ================================================================

            /**
             * GET /api/v1/team/members
             * List all active team members in current tenant.
             */
            get("/members") {
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
            put("/members/{userId}/role") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()

                // Verify owner role
                if (!teamService.verifyOwnerRole(principal.userId, tenantId)) {
                    call.respond(HttpStatusCode.Forbidden, "Only the workspace Owner can manage team members")
                    return@put
                }

                val userIdParam = call.parameters["userId"]
                    ?: return@put call.respond(HttpStatusCode.BadRequest, "User ID is required")

                val targetUserId = UserId(Uuid.parse(userIdParam))
                val request = call.receive<UpdateMemberRoleRequest>()

                teamService.updateMemberRole(tenantId, targetUserId, request.role, principal.userId)
                    .onSuccess {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Role updated successfully"))
                    }
                    .onFailure { error ->
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (error.message ?: "Failed to update role")))
                    }
            }

            /**
             * DELETE /api/v1/team/members/{userId}
             * Remove a team member from the workspace.
             * Requires Owner role.
             */
            delete("/members/{userId}") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()

                // Verify owner role
                if (!teamService.verifyOwnerRole(principal.userId, tenantId)) {
                    call.respond(HttpStatusCode.Forbidden, "Only the workspace Owner can manage team members")
                    return@delete
                }

                val userIdParam = call.parameters["userId"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "User ID is required")

                val targetUserId = UserId(Uuid.parse(userIdParam))

                teamService.removeMember(tenantId, targetUserId, principal.userId)
                    .onSuccess {
                        call.respond(HttpStatusCode.NoContent)
                    }
                    .onFailure { error ->
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (error.message ?: "Failed to remove member")))
                    }
            }

            // ================================================================
            // OWNERSHIP
            // ================================================================

            /**
             * POST /api/v1/team/transfer-ownership
             * Transfer workspace ownership to another member.
             * Requires Owner role.
             */
            post("/transfer-ownership") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()

                // Verify owner role
                if (!teamService.verifyOwnerRole(principal.userId, tenantId)) {
                    call.respond(HttpStatusCode.Forbidden, "Only the workspace Owner can transfer ownership")
                    return@post
                }

                val request = call.receive<TransferOwnershipRequest>()

                teamService.transferOwnership(tenantId, request.newOwnerId, principal.userId)
                    .onSuccess {
                        call.respond(HttpStatusCode.OK, mapOf("message" to "Ownership transferred successfully"))
                    }
                    .onFailure { error ->
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (error.message ?: "Failed to transfer ownership")))
                    }
            }

            // ================================================================
            // INVITATIONS
            // ================================================================

            /**
             * POST /api/v1/team/invitations
             * Create a new team invitation.
             * Requires Owner role.
             */
            post("/invitations") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()

                // Verify owner role
                if (!teamService.verifyOwnerRole(principal.userId, tenantId)) {
                    call.respond(HttpStatusCode.Forbidden, "Only the workspace Owner can invite team members")
                    return@post
                }

                val request = call.receive<CreateInvitationRequest>()

                teamService.createInvitation(tenantId, principal.userId, request)
                    .onSuccess { invitation ->
                        call.respond(HttpStatusCode.Created, invitation)
                    }
                    .onFailure { error ->
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (error.message ?: "Failed to create invitation")))
                    }
            }

            /**
             * GET /api/v1/team/invitations
             * List all invitations for current tenant.
             * Optional query param: status (PENDING, ACCEPTED, EXPIRED, CANCELLED)
             */
            get("/invitations") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()

                val statusParam = call.request.queryParameters["status"]
                val status = statusParam?.let {
                    try {
                        InvitationStatus.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }

                val invitations = if (status == InvitationStatus.Pending) {
                    teamService.listPendingInvitations(tenantId)
                } else {
                    // For now, only pending is supported via service
                    teamService.listPendingInvitations(tenantId)
                }

                call.respond(HttpStatusCode.OK, invitations)
            }

            /**
             * DELETE /api/v1/team/invitations/{id}
             * Cancel/revoke a pending invitation.
             * Requires Owner role.
             */
            delete("/invitations/{id}") {
                val principal = dokusPrincipal
                val tenantId = principal.requireTenantId()

                // Verify owner role
                if (!teamService.verifyOwnerRole(principal.userId, tenantId)) {
                    call.respond(HttpStatusCode.Forbidden, "Only the workspace Owner can manage invitations")
                    return@delete
                }

                val idParam = call.parameters["id"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invitation ID is required")

                val invitationId = InvitationId(Uuid.parse(idParam))

                teamService.cancelInvitation(invitationId, tenantId)
                    .onSuccess {
                        call.respond(HttpStatusCode.NoContent)
                    }
                    .onFailure { error ->
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to (error.message ?: "Failed to cancel invitation")))
                    }
            }
        }
    }
}
