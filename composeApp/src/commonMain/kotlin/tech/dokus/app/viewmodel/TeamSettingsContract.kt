package tech.dokus.app.viewmodel

import ai.dokus.foundation.domain.asbtractions.RetryHandler
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.InvitationId
import ai.dokus.foundation.domain.ids.UserId
import tech.dokus.domain.model.TeamMember
import tech.dokus.domain.model.TenantInvitation
import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for Team Settings screen.
 *
 * Manages team members, invitations, role changes, and ownership transfers.
 *
 * Flow:
 * 1. Loading → Initial data fetch
 * 2. Content → Members and invitations loaded
 *    - User can invite new members
 *    - User can change member roles
 *    - User can remove members
 *    - User can cancel invitations
 *    - User can transfer ownership
 * 3. Error → Failed to load with retry option
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface TeamSettingsState : MVIState, DokusState<Nothing> {

    /**
     * Initial loading state.
     */
    data object Loading : TeamSettingsState

    /**
     * Content state with team members and invitations.
     *
     * @property members List of team members
     * @property membersLoading Whether members are being refreshed
     * @property invitations List of pending invitations
     * @property invitationsLoading Whether invitations are being refreshed
     * @property inviteEmail Email for new invitation
     * @property inviteRole Role for new invitation
     * @property actionState Current action state for feedback
     */
    @Immutable
    data class Content(
        val members: List<TeamMember> = emptyList(),
        val membersLoading: Boolean = false,
        val invitations: List<TenantInvitation> = emptyList(),
        val invitationsLoading: Boolean = false,
        val inviteEmail: String = "",
        val inviteRole: UserRole = UserRole.Editor,
        val actionState: ActionState = ActionState.Idle,
    ) : TeamSettingsState {

        /**
         * State for team action operations.
         */
        @Immutable
        sealed interface ActionState {
            data object Idle : ActionState
            data object Processing : ActionState
            data object Inviting : ActionState
            data class Success(val message: String) : ActionState
            data class Error(val message: String) : ActionState
        }
    }

    /**
     * Error state with recovery option.
     *
     * @property exception The error that occurred
     * @property retryHandler Handler to retry the failed operation
     */
    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : TeamSettingsState, DokusState.Error<Nothing>
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface TeamSettingsIntent : MVIIntent {

    /** Load all team data (members and invitations) */
    data object Load : TeamSettingsIntent

    /** Refresh all team data */
    data object Refresh : TeamSettingsIntent

    /** Update invite email field */
    data class UpdateInviteEmail(val email: String) : TeamSettingsIntent

    /** Update invite role selection */
    data class UpdateInviteRole(val role: UserRole) : TeamSettingsIntent

    /** Reset the invite form */
    data object ResetInviteForm : TeamSettingsIntent

    /** Send an invitation to the entered email */
    data object SendInvitation : TeamSettingsIntent

    /** Cancel a pending invitation */
    data class CancelInvitation(val invitationId: InvitationId) : TeamSettingsIntent

    /** Update a team member's role */
    data class UpdateMemberRole(val userId: UserId, val newRole: UserRole) : TeamSettingsIntent

    /** Remove a team member */
    data class RemoveMember(val userId: UserId) : TeamSettingsIntent

    /** Transfer workspace ownership to a member */
    data class TransferOwnership(val newOwnerId: UserId) : TeamSettingsIntent

    /** Reset action state to idle */
    data object ResetActionState : TeamSettingsIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface TeamSettingsAction : MVIAction {

    /** Show a success message */
    data class ShowSuccess(val message: String) : TeamSettingsAction

    /** Show an error message */
    data class ShowError(val message: String) : TeamSettingsAction

    /** Dismiss the invite dialog */
    data object DismissInviteDialog : TeamSettingsAction
}
