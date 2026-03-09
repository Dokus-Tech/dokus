package tech.dokus.app.viewmodel

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.TeamMember
import tech.dokus.domain.model.TenantInvitation
import tech.dokus.domain.model.auth.BookkeeperFirmSearchItem
import tech.dokus.domain.model.auth.TenantBookkeeperAccessItem
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for Team Settings screen.
 *
 * Manages team members, invitations, role changes, and ownership transfers.
 *
 * Network-loaded data is wrapped in [DokusState] so the screen chrome
 * (form fields, dialogs) stays visible during loading/error states.
 */

// ============================================================================
// STATE
// ============================================================================

/**
 * Data loaded from the network for the team settings screen.
 */
@Immutable
data class TeamData(
    val members: List<TeamMember> = emptyList(),
    val invitations: List<TenantInvitation> = emptyList(),
    val bookkeeperAccess: List<TenantBookkeeperAccessItem> = emptyList(),
    val isCurrentUserOwner: Boolean = false,
    val currentUserId: UserId? = null,
    val maxSeats: Int = 3,
) {
    val availableSeats: Int get() = (maxSeats - members.size - invitations.size).coerceAtLeast(0)
}

@Immutable
data class TeamSettingsState(
    val teamData: DokusState<TeamData> = DokusState.loading(),
    val inviteEmail: String = "",
    val inviteRole: UserRole = UserRole.Editor,
    val bookkeeperSearchQuery: String = "",
    val bookkeeperSearchResults: List<BookkeeperFirmSearchItem> = emptyList(),
    val bookkeeperSearchLoading: Boolean = false,
    val selectedBookkeeperFirmId: FirmId? = null,
    val actionState: TeamSettingsActionState = TeamSettingsActionState.Idle,
) : MVIState

/**
 * State for team action operations.
 */
@Immutable
sealed interface TeamSettingsActionState {
    data object Idle : TeamSettingsActionState
    data object Processing : TeamSettingsActionState
    data object Inviting : TeamSettingsActionState
    data class Success(val success: TeamSettingsSuccess) : TeamSettingsActionState
    data class Error(val error: DokusException) : TeamSettingsActionState
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

    /** Update bookkeeper firm search input */
    data class UpdateBookkeeperSearchQuery(val query: String) : TeamSettingsIntent

    /** Execute bookkeeper firm search */
    data object SearchBookkeeperFirms : TeamSettingsIntent

    /** Select one firm result for grant action */
    data class SelectBookkeeperFirm(val firmId: FirmId?) : TeamSettingsIntent

    /** Grant current tenant access to selected bookkeeper firm */
    data object GrantBookkeeperAccess : TeamSettingsIntent

    /** Revoke existing firm access for current tenant */
    data class RevokeBookkeeperAccess(val firmId: FirmId) : TeamSettingsIntent

    /** Reset bookkeeper search and selection fields */
    data object ResetBookkeeperAccessForm : TeamSettingsIntent

    /** Reset action state to idle */
    data object ResetActionState : TeamSettingsIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface TeamSettingsAction : MVIAction {

    /** Show a success message */
    data class ShowSuccess(val success: TeamSettingsSuccess) : TeamSettingsAction

    /** Show an error message */
    data class ShowError(val error: DokusException) : TeamSettingsAction

    /** Dismiss the invite dialog */
    data object DismissInviteDialog : TeamSettingsAction

    /** Dismiss the grant-bookkeeper dialog */
    data object DismissBookkeeperDialog : TeamSettingsAction
}

enum class TeamSettingsSuccess {
    InviteSent,
    InviteCancelled,
    RoleUpdated,
    MemberRemoved,
    OwnershipTransferred,
    BookkeeperAccessGranted,
    BookkeeperAccessRevoked,
}
