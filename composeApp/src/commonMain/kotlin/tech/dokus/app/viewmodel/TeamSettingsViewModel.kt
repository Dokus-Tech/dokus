package tech.dokus.app.viewmodel

import ai.dokus.app.auth.datasource.TeamRemoteDataSource
import tech.dokus.foundation.app.state.DokusState
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.ids.InvitationId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.CreateInvitationRequest
import ai.dokus.foundation.domain.model.TeamMember
import ai.dokus.foundation.domain.model.TenantInvitation
import ai.dokus.foundation.platform.Logger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for team management screen.
 * Manages team members, invitations, role changes, and ownership transfers.
 */
class TeamSettingsViewModel(
    private val teamDataSource: TeamRemoteDataSource
) : ViewModel() {

    private val logger = Logger.forClass<TeamSettingsViewModel>()

    // State for team members
    private val _membersState = MutableStateFlow<DokusState<List<TeamMember>>>(DokusState.idle())
    val membersState: StateFlow<DokusState<List<TeamMember>>> = _membersState.asStateFlow()

    // State for pending invitations
    private val _invitationsState = MutableStateFlow<DokusState<List<TenantInvitation>>>(DokusState.idle())
    val invitationsState: StateFlow<DokusState<List<TenantInvitation>>> = _invitationsState.asStateFlow()

    // State for invite form
    private val _inviteFormState = MutableStateFlow(InviteFormState())
    val inviteFormState: StateFlow<InviteFormState> = _inviteFormState.asStateFlow()

    // State for action operations (invite, remove, etc.)
    private val _actionState = MutableStateFlow<TeamActionState>(TeamActionState.Idle)
    val actionState: StateFlow<TeamActionState> = _actionState.asStateFlow()

    /**
     * Load team members from backend.
     */
    fun loadTeamMembers() {
        viewModelScope.launch {
            logger.d { "Loading team members" }
            _membersState.value = DokusState.loading()

            teamDataSource.listTeamMembers().fold(
                onSuccess = { members ->
                    logger.i { "Loaded ${members.size} team members" }
                    _membersState.value = DokusState.success(members)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load team members" }
                    _membersState.value = DokusState.error(error) { loadTeamMembers() }
                }
            )
        }
    }

    /**
     * Load pending invitations from backend.
     */
    fun loadInvitations() {
        viewModelScope.launch {
            logger.d { "Loading pending invitations" }
            _invitationsState.value = DokusState.loading()

            teamDataSource.listPendingInvitations().fold(
                onSuccess = { invitations ->
                    logger.i { "Loaded ${invitations.size} pending invitations" }
                    _invitationsState.value = DokusState.success(invitations)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load invitations" }
                    _invitationsState.value = DokusState.error(error) { loadInvitations() }
                }
            )
        }
    }

    /**
     * Load both team members and invitations.
     */
    fun loadAll() {
        loadTeamMembers()
        loadInvitations()
    }

    /**
     * Send an invitation to join the team.
     */
    fun sendInvitation(onSuccess: () -> Unit = {}) {
        val form = _inviteFormState.value

        // Validate email
        if (form.email.isBlank()) {
            _actionState.value = TeamActionState.Error("Email is required")
            return
        }

        // Basic email validation
        if (!form.email.contains("@") || !form.email.contains(".")) {
            _actionState.value = TeamActionState.Error("Please enter a valid email address")
            return
        }

        viewModelScope.launch {
            logger.d { "Sending invitation to ${form.email}" }
            _actionState.value = TeamActionState.Inviting

            val request = CreateInvitationRequest(
                email = Email(form.email.trim()),
                role = form.role
            )

            teamDataSource.createInvitation(request).fold(
                onSuccess = { invitation ->
                    logger.i { "Invitation sent to ${form.email}" }
                    _actionState.value = TeamActionState.Success("Invitation sent successfully")

                    // Refresh data
                    loadAll()

                    // Reset form
                    _inviteFormState.value = InviteFormState()

                    onSuccess()
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to send invitation" }
                    _actionState.value = TeamActionState.Error(
                        error.message ?: "Failed to send invitation"
                    )
                }
            )
        }
    }

    /**
     * Cancel a pending invitation.
     */
    fun cancelInvitation(invitationId: InvitationId) {
        viewModelScope.launch {
            logger.d { "Cancelling invitation $invitationId" }
            _actionState.value = TeamActionState.Processing

            teamDataSource.cancelInvitation(invitationId).fold(
                onSuccess = {
                    logger.i { "Invitation cancelled: $invitationId" }
                    _actionState.value = TeamActionState.Success("Invitation cancelled")
                    loadInvitations()
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to cancel invitation" }
                    _actionState.value = TeamActionState.Error(
                        error.message ?: "Failed to cancel invitation"
                    )
                }
            )
        }
    }

    /**
     * Update a team member's role.
     */
    fun updateMemberRole(userId: UserId, newRole: UserRole) {
        viewModelScope.launch {
            logger.d { "Updating role for $userId to $newRole" }
            _actionState.value = TeamActionState.Processing

            teamDataSource.updateMemberRole(userId, newRole).fold(
                onSuccess = {
                    logger.i { "Role updated for $userId to $newRole" }
                    _actionState.value = TeamActionState.Success("Role updated successfully")
                    loadTeamMembers()
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to update role" }
                    _actionState.value = TeamActionState.Error(
                        error.message ?: "Failed to update role"
                    )
                }
            )
        }
    }

    /**
     * Remove a team member from the workspace.
     */
    fun removeMember(userId: UserId) {
        viewModelScope.launch {
            logger.d { "Removing member $userId" }
            _actionState.value = TeamActionState.Processing

            teamDataSource.removeMember(userId).fold(
                onSuccess = {
                    logger.i { "Member removed: $userId" }
                    _actionState.value = TeamActionState.Success("Member removed successfully")
                    loadTeamMembers()
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to remove member" }
                    _actionState.value = TeamActionState.Error(
                        error.message ?: "Failed to remove member"
                    )
                }
            )
        }
    }

    /**
     * Transfer workspace ownership to another member.
     */
    fun transferOwnership(newOwnerId: UserId) {
        viewModelScope.launch {
            logger.d { "Transferring ownership to $newOwnerId" }
            _actionState.value = TeamActionState.Processing

            teamDataSource.transferOwnership(newOwnerId).fold(
                onSuccess = {
                    logger.i { "Ownership transferred to $newOwnerId" }
                    _actionState.value = TeamActionState.Success("Ownership transferred successfully")
                    loadTeamMembers()
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to transfer ownership" }
                    _actionState.value = TeamActionState.Error(
                        error.message ?: "Failed to transfer ownership"
                    )
                }
            )
        }
    }

    /**
     * Reset action state to idle.
     */
    fun resetActionState() {
        _actionState.value = TeamActionState.Idle
    }

    // Form field updates
    fun updateInviteEmail(value: String) {
        _inviteFormState.value = _inviteFormState.value.copy(email = value)
    }

    fun updateInviteRole(value: UserRole) {
        _inviteFormState.value = _inviteFormState.value.copy(role = value)
    }

    fun resetInviteForm() {
        _inviteFormState.value = InviteFormState()
    }
}

/**
 * Form state for sending invitations.
 */
data class InviteFormState(
    val email: String = "",
    val role: UserRole = UserRole.Editor
)

/**
 * State for team action operations.
 */
sealed class TeamActionState {
    data object Idle : TeamActionState()
    data object Processing : TeamActionState()
    data object Inviting : TeamActionState()
    data class Success(val message: String) : TeamActionState()
    data class Error(val message: String) : TeamActionState()
}
