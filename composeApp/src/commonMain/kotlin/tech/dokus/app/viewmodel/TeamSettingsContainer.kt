package tech.dokus.app.viewmodel

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.Email
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.CreateInvitationRequest
import tech.dokus.domain.enums.maxSeats
import tech.dokus.features.auth.usecases.CancelInvitationUseCase
import tech.dokus.features.auth.usecases.CreateInvitationUseCase
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCase
import tech.dokus.features.auth.usecases.GetCurrentUserUseCase
import tech.dokus.features.auth.usecases.ListPendingInvitationsUseCase
import tech.dokus.features.auth.usecases.ListTeamMembersUseCase
import tech.dokus.features.auth.usecases.RemoveTeamMemberUseCase
import tech.dokus.features.auth.usecases.TransferWorkspaceOwnershipUseCase
import tech.dokus.features.auth.usecases.UpdateTeamMemberRoleUseCase
import tech.dokus.foundation.platform.Logger

internal typealias TeamSettingsCtx = PipelineContext<TeamSettingsState, TeamSettingsIntent, TeamSettingsAction>

internal data class TeamSettingsUseCases(
    val listTeamMembers: ListTeamMembersUseCase,
    val listPendingInvitations: ListPendingInvitationsUseCase,
    val createInvitation: CreateInvitationUseCase,
    val cancelInvitation: CancelInvitationUseCase,
    val updateTeamMemberRole: UpdateTeamMemberRoleUseCase,
    val removeTeamMember: RemoveTeamMemberUseCase,
    val transferWorkspaceOwnership: TransferWorkspaceOwnershipUseCase,
    val getCurrentUser: GetCurrentUserUseCase,
    val getCurrentTenant: GetCurrentTenantUseCase,
)

/**
 * Container for Team Settings screen using FlowMVI.
 *
 * Manages team members, invitations, role changes, and ownership transfers.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class TeamSettingsContainer(
    private val useCases: TeamSettingsUseCases,
) : Container<TeamSettingsState, TeamSettingsIntent, TeamSettingsAction> {

    private val logger = Logger.forClass<TeamSettingsContainer>()

    override val store: Store<TeamSettingsState, TeamSettingsIntent, TeamSettingsAction> =
        store(TeamSettingsState.Loading) {
            reduce { intent ->
                when (intent) {
                    is TeamSettingsIntent.Load -> handleLoad()
                    is TeamSettingsIntent.Refresh -> handleRefresh()
                    is TeamSettingsIntent.UpdateInviteEmail -> handleUpdateInviteEmail(intent.email)
                    is TeamSettingsIntent.UpdateInviteRole -> handleUpdateInviteRole(intent.role)
                    is TeamSettingsIntent.ResetInviteForm -> handleResetInviteForm()
                    is TeamSettingsIntent.SendInvitation -> handleSendInvitation()
                    is TeamSettingsIntent.CancelInvitation -> handleCancelInvitation(intent.invitationId)
                    is TeamSettingsIntent.UpdateMemberRole -> handleUpdateMemberRole(intent.userId, intent.newRole)
                    is TeamSettingsIntent.RemoveMember -> handleRemoveMember(intent.userId)
                    is TeamSettingsIntent.TransferOwnership -> handleTransferOwnership(intent.newOwnerId)
                    is TeamSettingsIntent.ResetActionState -> handleResetActionState()
                }
            }
        }

    private suspend fun TeamSettingsCtx.handleLoad() {
        logger.d { "Loading team data" }

        updateState { TeamSettingsState.Loading }

        loadTeamData()
    }

    private suspend fun TeamSettingsCtx.handleRefresh() {
        logger.d { "Refreshing team data" }

        withState<TeamSettingsState.Content, _> {
            updateState {
                copy(membersLoading = true, invitationsLoading = true)
            }
        }

        loadTeamData()
    }

    private suspend fun TeamSettingsCtx.loadTeamData() {
        // Load members
        val membersResult = useCases.listTeamMembers()
        val invitationsResult = useCases.listPendingInvitations()
        val currentUserId = useCases.getCurrentUser().getOrNull()?.id
        val maxSeats = useCases.getCurrentTenant().getOrNull()?.subscription?.maxSeats ?: 3

        membersResult.fold(
            onSuccess = { members ->
                logger.i { "Loaded ${members.size} team members" }
                invitationsResult.fold(
                    onSuccess = { invitations ->
                        logger.i { "Loaded ${invitations.size} pending invitations" }
                        updateState {
                            TeamSettingsState.Content(
                                members = members,
                                membersLoading = false,
                                invitations = invitations,
                                invitationsLoading = false,
                                currentUserId = currentUserId,
                                maxSeats = maxSeats,
                            )
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to load invitations" }
                        updateState {
                            TeamSettingsState.Error(
                                exception = error.asDokusException,
                                retryHandler = { intent(TeamSettingsIntent.Load) }
                            )
                        }
                    }
                )
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load team members" }
                updateState {
                    TeamSettingsState.Error(
                        exception = error.asDokusException,
                        retryHandler = { intent(TeamSettingsIntent.Load) }
                    )
                }
            }
        )
    }

    private suspend fun TeamSettingsCtx.handleUpdateInviteEmail(email: String) {
        withState<TeamSettingsState.Content, _> {
            updateState { copy(inviteEmail = email) }
        }
    }

    private suspend fun TeamSettingsCtx.handleUpdateInviteRole(role: UserRole) {
        withState<TeamSettingsState.Content, _> {
            updateState { copy(inviteRole = role) }
        }
    }

    private suspend fun TeamSettingsCtx.handleResetInviteForm() {
        withState<TeamSettingsState.Content, _> {
            updateState {
                copy(
                    inviteEmail = "",
                    inviteRole = UserRole.Editor
                )
            }
        }
    }

    private suspend fun TeamSettingsCtx.handleSendInvitation() {
        withState<TeamSettingsState.Content, _> {
            // Validate email
            if (inviteEmail.isBlank()) {
                val exception = DokusException.Validation.EmailRequired
                updateState {
                    copy(actionState = TeamSettingsState.Content.ActionState.Error(exception))
                }
                action(TeamSettingsAction.ShowError(exception))
                return@withState
            }

            // Basic email validation
            if (!inviteEmail.contains("@") || !inviteEmail.contains(".")) {
                val exception = DokusException.Validation.InvalidEmail
                updateState {
                    copy(actionState = TeamSettingsState.Content.ActionState.Error(exception))
                }
                action(TeamSettingsAction.ShowError(exception))
                return@withState
            }

            val currentEmail = inviteEmail.trim()
            val currentRole = inviteRole

            logger.d { "Sending invitation to $currentEmail" }
            updateState { copy(actionState = TeamSettingsState.Content.ActionState.Inviting) }

            val request = CreateInvitationRequest(
                email = Email(currentEmail),
                role = currentRole
            )

            useCases.createInvitation(request).fold(
                onSuccess = {
                    logger.i { "Invitation sent to $currentEmail" }
                    updateState {
                        copy(
                            inviteEmail = "",
                            inviteRole = UserRole.Editor,
                            actionState = TeamSettingsState.Content.ActionState.Success(TeamSettingsSuccess.InviteSent)
                        )
                    }
                    action(TeamSettingsAction.ShowSuccess(TeamSettingsSuccess.InviteSent))
                    action(TeamSettingsAction.DismissInviteDialog)

                    // Refresh invitations
                    refreshInvitations()
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to send invitation" }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.TeamInviteFailed
                    } else {
                        exception
                    }
                    updateState {
                        copy(actionState = TeamSettingsState.Content.ActionState.Error(displayException))
                    }
                    action(TeamSettingsAction.ShowError(displayException))
                }
            )
        }
    }

    private suspend fun TeamSettingsCtx.handleCancelInvitation(invitationId: InvitationId) {
        withState<TeamSettingsState.Content, _> {
            logger.d { "Cancelling invitation $invitationId" }
            updateState { copy(actionState = TeamSettingsState.Content.ActionState.Processing) }

            useCases.cancelInvitation(invitationId).fold(
                onSuccess = {
                    logger.i { "Invitation cancelled: $invitationId" }
                    updateState {
                        copy(
                            actionState = TeamSettingsState.Content.ActionState.Success(
                                TeamSettingsSuccess.InviteCancelled
                            )
                        )
                    }
                    action(TeamSettingsAction.ShowSuccess(TeamSettingsSuccess.InviteCancelled))

                    // Refresh invitations
                    refreshInvitations()
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to cancel invitation" }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.TeamInviteCancelFailed
                    } else {
                        exception
                    }
                    updateState {
                        copy(actionState = TeamSettingsState.Content.ActionState.Error(displayException))
                    }
                    action(TeamSettingsAction.ShowError(displayException))
                }
            )
        }
    }

    private suspend fun TeamSettingsCtx.handleUpdateMemberRole(userId: UserId, newRole: UserRole) {
        withState<TeamSettingsState.Content, _> {
            logger.d { "Updating role for $userId to $newRole" }
            updateState { copy(actionState = TeamSettingsState.Content.ActionState.Processing) }

            useCases.updateTeamMemberRole(userId, newRole).fold(
                onSuccess = {
                    logger.i { "Role updated for $userId to $newRole" }
                    updateState {
                        copy(
                            actionState = TeamSettingsState.Content.ActionState.Success(TeamSettingsSuccess.RoleUpdated)
                        )
                    }
                    action(TeamSettingsAction.ShowSuccess(TeamSettingsSuccess.RoleUpdated))

                    // Refresh members
                    refreshMembers()
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to update role" }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.TeamRoleUpdateFailed
                    } else {
                        exception
                    }
                    updateState {
                        copy(actionState = TeamSettingsState.Content.ActionState.Error(displayException))
                    }
                    action(TeamSettingsAction.ShowError(displayException))
                }
            )
        }
    }

    private suspend fun TeamSettingsCtx.handleRemoveMember(userId: UserId) {
        withState<TeamSettingsState.Content, _> {
            logger.d { "Removing member $userId" }
            updateState { copy(actionState = TeamSettingsState.Content.ActionState.Processing) }

            useCases.removeTeamMember(userId).fold(
                onSuccess = {
                    logger.i { "Member removed: $userId" }
                    updateState {
                        copy(
                            actionState = TeamSettingsState.Content.ActionState.Success(
                                TeamSettingsSuccess.MemberRemoved
                            )
                        )
                    }
                    action(TeamSettingsAction.ShowSuccess(TeamSettingsSuccess.MemberRemoved))

                    // Refresh members
                    refreshMembers()
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to remove member" }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.TeamMemberRemoveFailed
                    } else {
                        exception
                    }
                    updateState {
                        copy(actionState = TeamSettingsState.Content.ActionState.Error(displayException))
                    }
                    action(TeamSettingsAction.ShowError(displayException))
                }
            )
        }
    }

    private suspend fun TeamSettingsCtx.handleTransferOwnership(newOwnerId: UserId) {
        withState<TeamSettingsState.Content, _> {
            logger.d { "Transferring ownership to $newOwnerId" }
            updateState { copy(actionState = TeamSettingsState.Content.ActionState.Processing) }

            useCases.transferWorkspaceOwnership(newOwnerId).fold(
                onSuccess = {
                    logger.i { "Ownership transferred to $newOwnerId" }
                    updateState {
                        copy(
                            actionState = TeamSettingsState.Content.ActionState.Success(
                                TeamSettingsSuccess.OwnershipTransferred
                            )
                        )
                    }
                    action(TeamSettingsAction.ShowSuccess(TeamSettingsSuccess.OwnershipTransferred))

                    // Refresh members
                    refreshMembers()
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to transfer ownership" }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.TeamOwnershipTransferFailed
                    } else {
                        exception
                    }
                    updateState {
                        copy(actionState = TeamSettingsState.Content.ActionState.Error(displayException))
                    }
                    action(TeamSettingsAction.ShowError(displayException))
                }
            )
        }
    }

    private suspend fun TeamSettingsCtx.handleResetActionState() {
        withState<TeamSettingsState.Content, _> {
            updateState { copy(actionState = TeamSettingsState.Content.ActionState.Idle) }
        }
    }

    private suspend fun TeamSettingsCtx.refreshMembers() {
        withState<TeamSettingsState.Content, _> {
            updateState { copy(membersLoading = true) }

            useCases.listTeamMembers().fold(
                onSuccess = { members ->
                    logger.i { "Refreshed ${members.size} team members" }
                    updateState {
                        copy(members = members, membersLoading = false)
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to refresh members" }
                    updateState { copy(membersLoading = false) }
                }
            )
        }
    }

    private suspend fun TeamSettingsCtx.refreshInvitations() {
        withState<TeamSettingsState.Content, _> {
            updateState { copy(invitationsLoading = true) }

            useCases.listPendingInvitations().fold(
                onSuccess = { invitations ->
                    logger.i { "Refreshed ${invitations.size} pending invitations" }
                    updateState {
                        copy(invitations = invitations, invitationsLoading = false)
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to refresh invitations" }
                    updateState { copy(invitationsLoading = false) }
                }
            )
        }
    }
}
