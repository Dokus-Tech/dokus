package tech.dokus.backend.routes.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import tech.dokus.backend.security.requireRole
import tech.dokus.backend.security.requireTenantAccess
import tech.dokus.backend.security.requireTenantId
import tech.dokus.backend.services.auth.TeamService
import tech.dokus.domain.enums.InvitationStatus
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.CreateInvitationRequest
import tech.dokus.domain.model.auth.BookkeeperFirmSearchItem
import tech.dokus.domain.model.auth.GrantBookkeeperAccessRequest
import tech.dokus.domain.model.auth.GrantBookkeeperAccessResponse
import tech.dokus.domain.model.TransferOwnershipRequest
import tech.dokus.domain.model.UpdateMemberRoleRequest
import tech.dokus.domain.routes.Team
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
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
            val tenantId = requireTenantId()

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
            val tenantId = requireTenantAccess().requireRole(UserRole.Owner).tenantId

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
            val tenantId = requireTenantAccess().requireRole(UserRole.Owner).tenantId

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
            val tenantId = requireTenantAccess().requireRole(UserRole.Owner).tenantId

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
            val tenantId = requireTenantId()

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
            val tenantId = requireTenantAccess().requireRole(UserRole.Owner).tenantId

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
            val tenantId = requireTenantAccess().requireRole(UserRole.Owner).tenantId

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

        // ================================================================
        // BOOKKEEPER ACCESS (FIRM LINKS)
        // ================================================================

        /**
         * GET /api/v1/team/bookkeepers/search?query=...&limit=...
         * Search existing firms for granting access.
         * Requires Owner role.
         */
        get<Team.Bookkeepers.Search> { route ->
            val tenantId = requireTenantAccess().requireRole(UserRole.Owner).tenantId
            val query = route.query.trim()
            if (query.length < 2) {
                call.respond(HttpStatusCode.OK, emptyList<BookkeeperFirmSearchItem>())
                return@get
            }

            val results = teamService.searchBookkeeperFirms(
                tenantId = tenantId,
                query = query,
                limit = route.limit.coerceIn(1, 50),
            )
            call.respond(HttpStatusCode.OK, results)
        }

        /**
         * GET /api/v1/team/bookkeepers/access
         * List active firm access links for current tenant.
         * Requires Owner role.
         */
        get<Team.Bookkeepers.Access> {
            val tenantId = requireTenantAccess().requireRole(UserRole.Owner).tenantId
            val access = teamService.listBookkeeperAccess(tenantId)
            call.respond(HttpStatusCode.OK, access)
        }

        /**
         * POST /api/v1/team/bookkeepers/access
         * Grant tenant access to a firm.
         * Requires Owner role.
         */
        post<Team.Bookkeepers.Access> {
            val principal = dokusPrincipal
            val tenantId = requireTenantAccess().requireRole(UserRole.Owner).tenantId
            val request = call.receive<GrantBookkeeperAccessRequest>()

            teamService.grantBookkeeperAccess(
                tenantId = tenantId,
                firmId = request.firmId,
                grantedBy = principal.userId,
            ).fold(
                onSuccess = { activated ->
                    call.respond(
                        HttpStatusCode.OK,
                        GrantBookkeeperAccessResponse(
                            firmId = request.firmId,
                            tenantId = tenantId,
                            activated = activated
                        )
                    )
                },
                onFailure = { error ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (error.message ?: "Failed to grant access"))
                    )
                }
            )
        }

        /**
         * DELETE /api/v1/team/bookkeepers/access/{firmId}
         * Revoke active firm access for the tenant.
         * Requires Owner role.
         */
        delete<Team.Bookkeepers.Access.ByFirm> { route ->
            val tenantId = requireTenantAccess().requireRole(UserRole.Owner).tenantId
            teamService.revokeBookkeeperAccess(
                tenantId = tenantId,
                firmId = route.firmId,
            ).fold(
                onSuccess = { revoked ->
                    if (!revoked) {
                        throw DokusException.NotFound("Active firm access not found")
                    }
                    call.respond(HttpStatusCode.NoContent)
                },
                onFailure = { error ->
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to (error.message ?: "Failed to revoke access"))
                    )
                }
            )
        }
    }
}
