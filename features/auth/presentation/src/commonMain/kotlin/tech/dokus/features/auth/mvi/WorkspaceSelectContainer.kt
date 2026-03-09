package tech.dokus.features.auth.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.features.auth.usecases.GetAccountMeUseCase
import tech.dokus.features.auth.usecases.RefreshSessionNowUseCase
import tech.dokus.features.auth.usecases.SelectTenantUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.platform.Logger

internal typealias WorkspaceSelectCtx =
    PipelineContext<WorkspaceSelectState, WorkspaceSelectIntent, WorkspaceSelectAction>

/**
 * Container for Workspace Selection screen using FlowMVI.
 * Loads available tenant/firms from account/me and handles workspace selection.
 */
internal class WorkspaceSelectContainer(
    private val getAccountMeUseCase: GetAccountMeUseCase,
    private val selectTenantUseCase: SelectTenantUseCase,
    private val refreshSessionNowUseCase: RefreshSessionNowUseCase,
) : Container<WorkspaceSelectState, WorkspaceSelectIntent, WorkspaceSelectAction> {

    private val logger = Logger.forClass<WorkspaceSelectContainer>()

    override val store: Store<WorkspaceSelectState, WorkspaceSelectIntent, WorkspaceSelectAction> =
        store(WorkspaceSelectState.initial) {
            init {
                handleLoadWorkspaces()
            }
            reduce { intent ->
                when (intent) {
                    WorkspaceSelectIntent.LoadWorkspaces -> handleLoadWorkspaces()
                    is WorkspaceSelectIntent.SelectTenant -> handleSelectTenant(intent.tenantId)
                    is WorkspaceSelectIntent.SelectFirm -> handleSelectFirm(intent.firmId)
                }
            }
        }

    private suspend fun WorkspaceSelectCtx.handleLoadWorkspaces() {
        updateState { copy(workspaces = workspaces.asLoading) }

        logger.d { "Loading available workspaces from account/me" }
        getAccountMeUseCase().fold(
            onSuccess = { accountMe ->
                val tenants = accountMe.tenants.sortedBy { it.name.value.lowercase() }
                val firms = accountMe.firms.sortedBy { it.name.value.lowercase() }
                logger.i { "Loaded ${tenants.size} tenant(s) and ${firms.size} firm(s)" }
                updateState {
                    copy(workspaces = DokusState.success(WorkspaceSelectData(tenants, firms)))
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load workspaces" }
                updateState {
                    copy(
                        workspaces = DokusState.error(
                            exception = error.asDokusException,
                            retryHandler = { intent(WorkspaceSelectIntent.LoadWorkspaces) },
                        )
                    )
                }
            }
        )
    }

    private suspend fun WorkspaceSelectCtx.handleSelectTenant(tenantId: TenantId) {
        withState {
            if (!workspaces.isSuccess()) return@withState

            updateState { copy(isSelectingTenant = true) }

            logger.d { "Selecting tenant: $tenantId" }
            selectTenantUseCase(tenantId).fold(
                onSuccess = {
                    logger.i { "Tenant selected successfully: $tenantId" }
                    action(WorkspaceSelectAction.NavigateToHome)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to select tenant: $tenantId" }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.WorkspaceSelectFailed
                    } else {
                        exception
                    }
                    action(WorkspaceSelectAction.ShowSelectionError(displayException))
                    updateState { copy(isSelectingTenant = false) }
                }
            )
        }
    }

    private suspend fun WorkspaceSelectCtx.handleSelectFirm(firmId: FirmId) {
        withState {
            if (!workspaces.isSuccess()) return@withState

            updateState { copy(isSelectingFirm = true) }

            refreshSessionNowUseCase().fold(
                onSuccess = {
                    action(WorkspaceSelectAction.NavigateToBookkeeperConsole(firmId))
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to refresh session before firm navigation: $firmId" }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.WorkspaceSelectFailed
                    } else {
                        exception
                    }
                    action(WorkspaceSelectAction.ShowSelectionError(displayException))
                    updateState { copy(isSelectingFirm = false) }
                }
            )
        }
    }
}
