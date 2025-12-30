package tech.dokus.backend.services.auth

import tech.dokus.database.repository.auth.InvitationRepository
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.domain.enums.InvitationStatus
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.CreateInvitationRequest
import tech.dokus.domain.model.TeamMember
import tech.dokus.domain.model.TenantInvitation
import tech.dokus.foundation.backend.utils.loggerFor
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.days

/**
 * Service for team management operations.
 * Handles invitations, role changes, and ownership transfers.
 */
class TeamService(
    private val userRepository: UserRepository,
    private val tenantRepository: TenantRepository,
    private val invitationRepository: InvitationRepository
) {
    private val logger = loggerFor()

    companion object {
        /** Invitations expire after 30 days */
        val INVITATION_EXPIRY = 30.days
    }

    /**
     * List all team members in a tenant.
     */
    suspend fun listTeamMembers(tenantId: TenantId): List<TeamMember> {
        logger.debug("Listing team members for tenant {}", tenantId)

        val usersInTenant = userRepository.listByTenant(tenantId, activeOnly = true)

        return usersInTenant.map { userInTenant ->
            TeamMember(
                userId = userInTenant.user.id,
                email = userInTenant.user.email,
                firstName = userInTenant.user.firstName,
                lastName = userInTenant.user.lastName,
                role = userInTenant.role,
                joinedAt = userInTenant.user.createdAt, // TODO: Use membership createdAt
                lastActiveAt = userInTenant.user.lastLoginAt
            )
        }
    }

    /**
     * Create an invitation for a user to join the team.
     *
     * If the user already exists in the system, they are added directly.
     * If not, an invitation is created that they can accept when registering.
     *
     * @param tenantId Target tenant
     * @param invitedBy User creating the invitation (must be Owner)
     * @param request Invitation details
     * @return Created invitation or null if user was added directly
     */
    suspend fun createInvitation(
        tenantId: TenantId,
        invitedBy: UserId,
        request: CreateInvitationRequest
    ): Result<TenantInvitation> = runCatching {
        logger.debug("Creating invitation for {} to tenant {}", request.email, tenantId)

        // Validate role - cannot invite as Owner
        if (request.role == UserRole.Owner) {
            throw IllegalArgumentException("Cannot invite users as Owner. Use transfer ownership instead.")
        }

        // Check if user already exists
        val existingUser = userRepository.findByEmail(request.email.value)

        if (existingUser != null) {
            // Check if user is already a member
            val membership = userRepository.getMembership(existingUser.id, tenantId)
            if (membership != null && membership.isActive) {
                throw IllegalArgumentException("User is already a member of this workspace")
            }

            // Add existing user directly
            if (membership != null) {
                // Reactivate existing membership
                userRepository.updateRole(existingUser.id, tenantId, request.role)
            } else {
                userRepository.addToTenant(existingUser.id, tenantId, request.role)
            }

            logger.info("Added existing user ${existingUser.id} to tenant $tenantId with role ${request.role}")

            // Return a pseudo-invitation marked as accepted
            return@runCatching TenantInvitation(
                id = InvitationId.generate(),
                tenantId = tenantId,
                email = request.email,
                role = request.role,
                invitedByName = "System",
                status = InvitationStatus.Accepted,
                expiresAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            )
        }

        // Create invitation for new user
        val expiresAt = Clock.System.now() + INVITATION_EXPIRY
        val invitationId = invitationRepository.create(
            tenantId = tenantId,
            email = request.email,
            role = request.role,
            invitedBy = invitedBy,
            expiresAt = expiresAt
        )

        logger.info("Created invitation $invitationId for ${request.email} to tenant $tenantId")

        // Fetch and return the created invitation
        invitationRepository.findByIdAndTenant(invitationId, tenantId)
            ?: throw IllegalStateException("Failed to retrieve created invitation")
    }

    /**
     * List pending invitations for a tenant.
     */
    suspend fun listPendingInvitations(tenantId: TenantId): List<TenantInvitation> {
        return invitationRepository.listByTenant(tenantId, InvitationStatus.Pending)
    }

    /**
     * Cancel an invitation.
     */
    suspend fun cancelInvitation(
        invitationId: InvitationId,
        tenantId: TenantId
    ): Result<Unit> = runCatching {
        invitationRepository.cancel(invitationId, tenantId)
        logger.info("Cancelled invitation $invitationId")
    }

    /**
     * Update a team member's role.
     *
     * @param tenantId Tenant context
     * @param targetUserId User whose role is being changed
     * @param newRole New role to assign
     * @param requestingUserId User making the request (must be Owner)
     */
    suspend fun updateMemberRole(
        tenantId: TenantId,
        targetUserId: UserId,
        newRole: UserRole,
        requestingUserId: UserId
    ): Result<Unit> = runCatching {
        // Cannot change to Owner role - use transfer ownership
        if (newRole == UserRole.Owner) {
            throw IllegalArgumentException("Cannot change role to Owner. Use transfer ownership instead.")
        }

        // Cannot change the Owner's role
        val targetMembership = userRepository.getMembership(targetUserId, tenantId)
            ?: throw IllegalArgumentException("User is not a member of this workspace")

        if (targetMembership.role == UserRole.Owner) {
            throw IllegalArgumentException("Cannot change the Owner's role. Transfer ownership first.")
        }

        userRepository.updateRole(targetUserId, tenantId, newRole)
        logger.info("Updated role for user $targetUserId to $newRole in tenant $tenantId")
    }

    /**
     * Remove a member from the team.
     *
     * @param tenantId Tenant context
     * @param targetUserId User to remove
     * @param requestingUserId User making the request (must be Owner)
     */
    suspend fun removeMember(
        tenantId: TenantId,
        targetUserId: UserId,
        requestingUserId: UserId
    ): Result<Unit> = runCatching {
        // Cannot remove self
        if (targetUserId == requestingUserId) {
            throw IllegalArgumentException("Cannot remove yourself from the workspace")
        }

        // Cannot remove the Owner
        val targetMembership = userRepository.getMembership(targetUserId, tenantId)
            ?: throw IllegalArgumentException("User is not a member of this workspace")

        if (targetMembership.role == UserRole.Owner) {
            throw IllegalArgumentException("Cannot remove the workspace Owner")
        }

        userRepository.removeFromTenant(targetUserId, tenantId)
        logger.info("Removed user $targetUserId from tenant $tenantId")
    }

    /**
     * Transfer workspace ownership to another member.
     *
     * The current Owner becomes an Admin after transfer.
     *
     * @param tenantId Tenant context
     * @param newOwnerId User who will become the new Owner
     * @param currentOwnerId Current Owner making the transfer
     */
    suspend fun transferOwnership(
        tenantId: TenantId,
        newOwnerId: UserId,
        currentOwnerId: UserId
    ): Result<Unit> = runCatching {
        // Verify the new owner is a member
        val newOwnerMembership = userRepository.getMembership(newOwnerId, tenantId)
            ?: throw IllegalArgumentException("Target user is not a member of this workspace")

        if (!newOwnerMembership.isActive) {
            throw IllegalArgumentException("Target user's membership is not active")
        }

        // Verify current user is the Owner
        val currentMembership = userRepository.getMembership(currentOwnerId, tenantId)
            ?: throw IllegalArgumentException("You are not a member of this workspace")

        if (currentMembership.role != UserRole.Owner) {
            throw IllegalArgumentException("Only the Owner can transfer ownership")
        }

        // Transfer: new owner becomes Owner, current owner becomes Admin
        userRepository.updateRole(newOwnerId, tenantId, UserRole.Owner)
        userRepository.updateRole(currentOwnerId, tenantId, UserRole.Admin)

        logger.info("Transferred ownership of tenant $tenantId from $currentOwnerId to $newOwnerId")
    }

    /**
     * Verify that a user has Owner role in a tenant.
     */
    suspend fun verifyOwnerRole(userId: UserId, tenantId: TenantId): Boolean {
        val membership = userRepository.getMembership(userId, tenantId)
        return membership?.role == UserRole.Owner && membership.isActive
    }
}

private fun kotlinx.datetime.Instant.toLocalDateTime(
    timeZone: kotlinx.datetime.TimeZone
): kotlinx.datetime.LocalDateTime {
    return this.toLocalDateTime(timeZone)
}
