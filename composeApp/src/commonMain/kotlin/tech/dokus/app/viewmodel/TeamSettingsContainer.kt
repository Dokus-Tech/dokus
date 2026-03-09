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
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.InvitationId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.CreateInvitationRequest
import tech.dokus.domain.enums.maxSeats
import tech.dokus.features.auth.usecases.CancelInvitationUseCase
import tech.dokus.features.auth.usecases.CreateInvitationUseCase
import tech.dokus.features.auth.usecases.GrantBookkeeperAccessUseCase
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCase
import tech.dokus.features.auth.usecases.GetCurrentUserUseCase
import tech.dokus.features.auth.usecases.ListBookkeeperAccessUseCase
import tech.dokus.features.auth.usecases.ListPendingInvitationsUseCase
import tech.dokus.features.auth.usecases.ListTeamMembersUseCase
import tech.dokus.features.auth.usecases.RevokeBookkeeperAccessUseCase
import tech.dokus.features.auth.usecases.RemoveTeamMemberUseCase
import tech.dokus.features.auth.usecases.SearchBookkeeperFirmsUseCase
import tech.dokus.features.auth.usecases.TransferWorkspaceOwnershipUseCase
import tech.dokus.features.auth.usecases.UpdateTeamMemberRoleUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isSuccess
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
    val searchBookkeeperFirms: SearchBookkeeperFirmsUseCase,
    val listBookkeeperAccess: ListBookkeeperAccessUseCase,
    val grantBookkeeperAccess: GrantBookkeeperAccessUseCase,
    val revokeBookkeeperAccess: RevokeBookkeeperAccessUseCase,
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
        store(TeamSettingsState.initial) {
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
                    is TeamSettingsIntent.UpdateBookkeeperSearchQuery -> handleUpdateBookkeeperSearchQuery(intent.query)
                    TeamSettingsIntent.SearchBookkeeperFirms -> handleSearchBookkeeperFirms()
                    is TeamSettingsIntent.SelectBookkeeperFirm -> handleSelectBookkeeperFirm(intent.firmId)
                    TeamSettingsIntent.GrantBookkeeperAccess -> handleGrantBookkeeperAccess()
                    is TeamSettingsIntent.RevokeBookkeeperAccess -> handleRevokeBookkeeperAccess(intent.firmId)
                    TeamSettingsIntent.ResetBookkeeperAccessForm -> handleResetBookkeeperAccessForm()
                    is TeamSettingsIntent.ResetActionState -> handleResetActionState()
                }
            }
        }

    private suspend fun TeamSettingsCtx.handleLoad() {
        logger.d { "Loading team data" }

        updateState { copy(teamData = DokusState.loading()) }

        loadTeamData()
    }

    private suspend fun TeamSettingsCtx.handleRefresh() {
        logger.d { "Refreshing team data" }

        updateState { copy(teamData = teamData.asLoading) }

        loadTeamData()
    }

    private suspend fun TeamSettingsCtx.loadTeamData() {
        val membersResult = useCases.listTeamMembers()
        val invitationsResult = useCases.listPendingInvitations()
        val currentUserId = useCases.getCurrentUser().getOrNull()?.id
        val currentTenant = useCases.getCurrentTenant().getOrNull()
        val maxSeats = currentTenant?.subscription?.maxSeats ?: 3
        val isCurrentUserOwner = currentTenant?.role == UserRole.Owner
        val bookkeeperAccessResult = if (isCurrentUserOwner) {
            useCases.listBookkeeperAccess()
        } else {
            Result.success(emptyList())
        }

        membersResult.fold(
            onSuccess = { members ->
                logger.i { "Loaded ${members.size} team members" }
                invitationsResult.fold(
                    onSuccess = { invitations ->
                        logger.i { "Loaded ${invitations.size} pending invitations" }
                        bookkeeperAccessResult.fold(
                            onSuccess = { bookkeeperAccess ->
                                updateState {
                                    copy(
                                        teamData = DokusState.success(
                                            TeamData(
                                                members = members,
                                                invitations = invitations,
                                                currentUserId = currentUserId,
                                                maxSeats = maxSeats,
                                                bookkeeperAccess = bookkeeperAccess,
                                                isCurrentUserOwner = isCurrentUserOwner,
                                            )
                                        )
                                    )
                                }
                            },
                            onFailure = { error ->
                                logger.e(error) { "Failed to load connected bookkeeper firms" }
                                updateState {
                                    copy(
                                        teamData = DokusState.success(
                                            TeamData(
                                                members = members,
                                                invitations = invitations,
                                                currentUserId = currentUserId,
                                                maxSeats = maxSeats,
                                                bookkeeperAccess = emptyList(),
                                                isCurrentUserOwner = isCurrentUserOwner,
                                            )
                                        )
                                    )
                                }
                                action(TeamSettingsAction.ShowError(error.asDokusException))
                            }
                        )
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to load invitations" }
                        updateState {
                            copy(
                                teamData = DokusState.error(
                                    exception = error.asDokusException,
                                    retryHandler = { intent(TeamSettingsIntent.Load) }
                                )
                            )
                        }
                    }
                )
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load team members" }
                updateState {
                    copy(
                        teamData = DokusState.error(
                            exception = error.asDokusException,
                            retryHandler = { intent(TeamSettingsIntent.Load) }
                        )
                    )
                }
            }
        )
    }

    private suspend fun TeamSettingsCtx.handleUpdateInviteEmail(email: String) {
        updateState { copy(inviteEmail = email) }
    }

    private suspend fun TeamSettingsCtx.handleUpdateInviteRole(role: UserRole) {
        updateState { copy(inviteRole = role) }
    }

    private suspend fun TeamSettingsCtx.handleResetInviteForm() {
        updateState {
            copy(
                inviteEmail = "",
                inviteRole = UserRole.Editor
            )
        }
    }

    private suspend fun TeamSettingsCtx.handleSendInvitation() {
        var capturedEmail: String? = null
        var capturedRole: UserRole? = null

        withState {
            if (!teamData.isSuccess()) return@withState
            capturedEmail = inviteEmail
            capturedRole = inviteRole
        }

        val inviteEmail = capturedEmail ?: return
        val inviteRole = capturedRole ?: return

        // Validate email
        if (inviteEmail.isBlank()) {
            val exception = DokusException.Validation.EmailRequired
            updateState { copy(actionState = TeamSettingsActionState.Error(exception)) }
            action(TeamSettingsAction.ShowError(exception))
            return
        }

        // Basic email validation
        if (!inviteEmail.contains("@") || !inviteEmail.contains(".")) {
            val exception = DokusException.Validation.InvalidEmail
            updateState { copy(actionState = TeamSettingsActionState.Error(exception)) }
            action(TeamSettingsAction.ShowError(exception))
            return
        }

        val currentEmail = inviteEmail.trim()

        logger.d { "Sending invitation to $currentEmail" }
        updateState { copy(actionState = TeamSettingsActionState.Inviting) }

        val request = CreateInvitationRequest(
            email = Email(currentEmail),
            role = inviteRole
        )

        useCases.createInvitation(request).fold(
            onSuccess = {
                logger.i { "Invitation sent to $currentEmail" }
                updateState {
                    copy(
                        inviteEmail = "",
                        inviteRole = UserRole.Editor,
                        actionState = TeamSettingsActionState.Success(TeamSettingsSuccess.InviteSent)
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
                updateState { copy(actionState = TeamSettingsActionState.Error(displayException)) }
                action(TeamSettingsAction.ShowError(displayException))
            }
        )
    }

    private suspend fun TeamSettingsCtx.handleUpdateBookkeeperSearchQuery(query: String) {
        updateState {
            copy(
                bookkeeperSearchQuery = query,
                selectedBookkeeperFirmId = null,
            )
        }
    }

    private suspend fun TeamSettingsCtx.handleSearchBookkeeperFirms() {
        var capturedQuery: String? = null
        var capturedSelectedFirmId: FirmId? = null

        withState {
            val data = (teamData as? DokusState.Success)?.data ?: return@withState
            if (!data.isCurrentUserOwner) return@withState
            capturedQuery = bookkeeperSearchQuery.trim()
            capturedSelectedFirmId = selectedBookkeeperFirmId
        }

        val query = capturedQuery ?: return

        if (query.length < 2) {
            updateState {
                copy(
                    bookkeeperSearchResults = emptyList(),
                    bookkeeperSearchLoading = false,
                    selectedBookkeeperFirmId = null,
                )
            }
            return
        }

        updateState { copy(bookkeeperSearchLoading = true) }

        useCases.searchBookkeeperFirms(query = query).fold(
            onSuccess = { results ->
                val sorted = results.sortedBy { it.name.value.lowercase() }
                val selectedId = capturedSelectedFirmId?.takeIf { selected ->
                    sorted.any { it.firmId == selected }
                }
                updateState {
                    copy(
                        bookkeeperSearchResults = sorted,
                        bookkeeperSearchLoading = false,
                        selectedBookkeeperFirmId = selectedId,
                    )
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to search bookkeeper firms" }
                updateState {
                    copy(
                        bookkeeperSearchResults = emptyList(),
                        bookkeeperSearchLoading = false,
                        selectedBookkeeperFirmId = null,
                    )
                }
                action(TeamSettingsAction.ShowError(error.asDokusException))
            }
        )
    }

    private suspend fun TeamSettingsCtx.handleSelectBookkeeperFirm(firmId: FirmId?) {
        updateState { copy(selectedBookkeeperFirmId = firmId) }
    }

    private suspend fun TeamSettingsCtx.handleGrantBookkeeperAccess() {
        var isOwner = false
        var capturedFirmId: FirmId? = null

        withState {
            val data = (teamData as? DokusState.Success)?.data ?: return@withState
            isOwner = data.isCurrentUserOwner
            capturedFirmId = selectedBookkeeperFirmId
        }

        if (!isOwner) {
            action(TeamSettingsAction.ShowError(DokusException.NotAuthorized()))
            return
        }

        val selectedFirmId = capturedFirmId
        if (selectedFirmId == null) {
            val exception = DokusException.BadRequest("Select a bookkeeper firm first")
            updateState { copy(actionState = TeamSettingsActionState.Error(exception)) }
            action(TeamSettingsAction.ShowError(exception))
            return
        }

        updateState { copy(actionState = TeamSettingsActionState.Processing) }

        useCases.grantBookkeeperAccess(selectedFirmId).fold(
            onSuccess = {
                updateState {
                    copy(
                        actionState = TeamSettingsActionState.Success(
                            TeamSettingsSuccess.BookkeeperAccessGranted
                        ),
                        bookkeeperSearchQuery = "",
                        bookkeeperSearchResults = emptyList(),
                        selectedBookkeeperFirmId = null,
                    )
                }
                action(TeamSettingsAction.ShowSuccess(TeamSettingsSuccess.BookkeeperAccessGranted))
                action(TeamSettingsAction.DismissBookkeeperDialog)
                refreshBookkeeperAccess()
            },
            onFailure = { error ->
                val exception = error.asDokusException
                updateState { copy(actionState = TeamSettingsActionState.Error(exception)) }
                action(TeamSettingsAction.ShowError(exception))
            }
        )
    }

    private suspend fun TeamSettingsCtx.handleRevokeBookkeeperAccess(firmId: FirmId) {
        var isOwner = false

        withState {
            val data = (teamData as? DokusState.Success)?.data ?: return@withState
            isOwner = data.isCurrentUserOwner
        }

        if (!isOwner) {
            action(TeamSettingsAction.ShowError(DokusException.NotAuthorized()))
            return
        }

        updateState { copy(actionState = TeamSettingsActionState.Processing) }

        useCases.revokeBookkeeperAccess(firmId).fold(
            onSuccess = {
                updateState {
                    copy(
                        actionState = TeamSettingsActionState.Success(
                            TeamSettingsSuccess.BookkeeperAccessRevoked
                        ),
                    )
                }
                action(TeamSettingsAction.ShowSuccess(TeamSettingsSuccess.BookkeeperAccessRevoked))
                refreshBookkeeperAccess()
            },
            onFailure = { error ->
                val exception = error.asDokusException
                updateState { copy(actionState = TeamSettingsActionState.Error(exception)) }
                action(TeamSettingsAction.ShowError(exception))
            }
        )
    }

    private suspend fun TeamSettingsCtx.handleResetBookkeeperAccessForm() {
        updateState {
            copy(
                bookkeeperSearchQuery = "",
                bookkeeperSearchResults = emptyList(),
                bookkeeperSearchLoading = false,
                selectedBookkeeperFirmId = null,
            )
        }
    }

    private suspend fun TeamSettingsCtx.handleCancelInvitation(invitationId: InvitationId) {
        var isSuccess = false
        withState { isSuccess = teamData.isSuccess() }
        if (!isSuccess) return

        logger.d { "Cancelling invitation $invitationId" }
        updateState { copy(actionState = TeamSettingsActionState.Processing) }

        useCases.cancelInvitation(invitationId).fold(
            onSuccess = {
                logger.i { "Invitation cancelled: $invitationId" }
                updateState {
                    copy(
                        actionState = TeamSettingsActionState.Success(
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
                updateState { copy(actionState = TeamSettingsActionState.Error(displayException)) }
                action(TeamSettingsAction.ShowError(displayException))
            }
        )
    }

    private suspend fun TeamSettingsCtx.handleUpdateMemberRole(userId: UserId, newRole: UserRole) {
        var isSuccess = false
        withState { isSuccess = teamData.isSuccess() }
        if (!isSuccess) return

        logger.d { "Updating role for $userId to $newRole" }
        updateState { copy(actionState = TeamSettingsActionState.Processing) }

        useCases.updateTeamMemberRole(userId, newRole).fold(
            onSuccess = {
                logger.i { "Role updated for $userId to $newRole" }
                updateState {
                    copy(actionState = TeamSettingsActionState.Success(TeamSettingsSuccess.RoleUpdated))
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
                updateState { copy(actionState = TeamSettingsActionState.Error(displayException)) }
                action(TeamSettingsAction.ShowError(displayException))
            }
        )
    }

    private suspend fun TeamSettingsCtx.handleRemoveMember(userId: UserId) {
        var isSuccess = false
        withState { isSuccess = teamData.isSuccess() }
        if (!isSuccess) return

        logger.d { "Removing member $userId" }
        updateState { copy(actionState = TeamSettingsActionState.Processing) }

        useCases.removeTeamMember(userId).fold(
            onSuccess = {
                logger.i { "Member removed: $userId" }
                updateState {
                    copy(
                        actionState = TeamSettingsActionState.Success(
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
                updateState { copy(actionState = TeamSettingsActionState.Error(displayException)) }
                action(TeamSettingsAction.ShowError(displayException))
            }
        )
    }

    private suspend fun TeamSettingsCtx.handleTransferOwnership(newOwnerId: UserId) {
        var isSuccess = false
        withState { isSuccess = teamData.isSuccess() }
        if (!isSuccess) return

        logger.d { "Transferring ownership to $newOwnerId" }
        updateState { copy(actionState = TeamSettingsActionState.Processing) }

        useCases.transferWorkspaceOwnership(newOwnerId).fold(
            onSuccess = {
                logger.i { "Ownership transferred to $newOwnerId" }
                updateState {
                    copy(
                        actionState = TeamSettingsActionState.Success(
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
                updateState { copy(actionState = TeamSettingsActionState.Error(displayException)) }
                action(TeamSettingsAction.ShowError(displayException))
            }
        )
    }

    private suspend fun TeamSettingsCtx.handleResetActionState() {
        updateState { copy(actionState = TeamSettingsActionState.Idle) }
    }

    private suspend fun TeamSettingsCtx.refreshMembers() {
        var capturedTeamData: TeamData? = null
        withState { capturedTeamData = (teamData as? DokusState.Success)?.data }
        val teamData = capturedTeamData ?: return

        useCases.listTeamMembers().fold(
            onSuccess = { members ->
                logger.i { "Refreshed ${members.size} team members" }
                updateState {
                    copy(teamData = DokusState.success(teamData.copy(members = members)))
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to refresh members" }
            }
        )
    }

    private suspend fun TeamSettingsCtx.refreshInvitations() {
        var capturedTeamData: TeamData? = null
        withState { capturedTeamData = (teamData as? DokusState.Success)?.data }
        val teamData = capturedTeamData ?: return

        useCases.listPendingInvitations().fold(
            onSuccess = { invitations ->
                logger.i { "Refreshed ${invitations.size} pending invitations" }
                updateState {
                    copy(teamData = DokusState.success(teamData.copy(invitations = invitations)))
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to refresh invitations" }
            }
        )
    }

    private suspend fun TeamSettingsCtx.refreshBookkeeperAccess() {
        var capturedTeamData: TeamData? = null
        withState { capturedTeamData = (teamData as? DokusState.Success)?.data }
        val teamData = capturedTeamData ?: return

        if (!teamData.isCurrentUserOwner) {
            updateState {
                copy(teamData = DokusState.success(teamData.copy(bookkeeperAccess = emptyList())))
            }
            return
        }

        useCases.listBookkeeperAccess().fold(
            onSuccess = { access ->
                updateState {
                    copy(teamData = DokusState.success(teamData.copy(bookkeeperAccess = access)))
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to refresh connected bookkeeper firms" }
                action(TeamSettingsAction.ShowError(error.asDokusException))
            }
        )
    }
}
