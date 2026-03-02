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
import tech.dokus.domain.model.auth.CreateFirmRequest
import tech.dokus.domain.model.auth.FirmWorkspaceSummary
import tech.dokus.domain.model.auth.TenantWorkspaceSummary
import tech.dokus.features.auth.usecases.CreateFirmUseCase
import tech.dokus.features.auth.usecases.GetAccountMeUseCase
import tech.dokus.features.auth.usecases.SelectTenantUseCase
import tech.dokus.foundation.platform.Logger

internal typealias WorkspaceSelectCtx =
    PipelineContext<WorkspaceSelectState, WorkspaceSelectIntent, WorkspaceSelectAction>

/**
 * Container for Workspace Selection screen using FlowMVI.
 * Loads available tenant/firms from account/me and handles workspace selection.
 */
internal class WorkspaceSelectContainer(
    private val getAccountMeUseCase: GetAccountMeUseCase,
    private val createFirmUseCase: CreateFirmUseCase,
    private val selectTenantUseCase: SelectTenantUseCase,
) : Container<WorkspaceSelectState, WorkspaceSelectIntent, WorkspaceSelectAction> {

    private val logger = Logger.forClass<WorkspaceSelectContainer>()

    override val store: Store<WorkspaceSelectState, WorkspaceSelectIntent, WorkspaceSelectAction> =
        store(WorkspaceSelectState.Loading) {
            init {
                handleLoadWorkspaces()
            }
            reduce { intent ->
                when (intent) {
                    WorkspaceSelectIntent.LoadWorkspaces -> handleLoadWorkspaces()
                    is WorkspaceSelectIntent.SelectTenant -> handleSelectTenant(intent.tenantId)
                    is WorkspaceSelectIntent.SelectFirm -> handleSelectFirm(intent.firmId)
                    is WorkspaceSelectIntent.CreateFirm -> handleCreateFirm(intent.prefillTenantId)
                }
            }
        }

    private suspend fun WorkspaceSelectCtx.handleLoadWorkspaces() {
        updateState { WorkspaceSelectState.Loading }

        logger.d { "Loading available workspaces from account/me" }
        getAccountMeUseCase().fold(
            onSuccess = { accountMe ->
                val tenants = accountMe.tenants.sortedBy { it.name.value.lowercase() }
                val firms = accountMe.firms.sortedBy { it.name.value.lowercase() }
                logger.i { "Loaded ${tenants.size} tenant(s) and ${firms.size} firm(s)" }
                updateState {
                    WorkspaceSelectState.Content(
                        tenants = tenants,
                        firms = firms,
                    )
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load workspaces" }
                updateState {
                    WorkspaceSelectState.Error(
                        exception = error.asDokusException,
                        retryHandler = { intent(WorkspaceSelectIntent.LoadWorkspaces) }
                    )
                }
            }
        )
    }

    private suspend fun WorkspaceSelectCtx.handleSelectTenant(tenantId: TenantId) {
        withState<WorkspaceSelectState.Content, _> {
            val currentTenants = tenants
            val currentFirms = firms

            updateState {
                WorkspaceSelectState.SelectingTenant(
                    tenants = currentTenants,
                    firms = currentFirms,
                    selectedTenantId = tenantId,
                )
            }

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
                    updateState {
                        WorkspaceSelectState.Content(
                            tenants = currentTenants,
                            firms = currentFirms,
                        )
                    }
                }
            )
        }
    }

    private suspend fun WorkspaceSelectCtx.handleSelectFirm(firmId: FirmId) {
        withState<WorkspaceSelectState.Content, _> {
            updateState {
                WorkspaceSelectState.SelectingFirm(
                    tenants = tenants,
                    firms = firms,
                    selectedFirmId = firmId,
                )
            }
            action(WorkspaceSelectAction.NavigateToBookkeeperConsole(firmId))
        }
    }

    private suspend fun WorkspaceSelectCtx.handleCreateFirm(prefillTenantId: TenantId?) {
        withState<WorkspaceSelectState.Content, _> {
            if (isCreatingFirm) return@withState

            val currentTenants = tenants
            val currentFirms = firms
            val resolvedPrefillTenantId = prefillTenantId ?: currentTenants.firstOrNull()?.id

            if (resolvedPrefillTenantId == null) {
                action(
                    WorkspaceSelectAction.ShowSelectionError(
                        DokusException.BadRequest(
                            "Cannot setup practice without tenant prefill in this phase"
                        )
                    )
                )
                return@withState
            }

            updateState { copy(isCreatingFirm = true) }

            createFirmUseCase(
                CreateFirmRequest(prefillTenantId = resolvedPrefillTenantId)
            ).fold(
                onSuccess = { createdFirm ->
                    val updatedFirms = (currentFirms + createdFirm)
                        .distinctBy(FirmWorkspaceSummary::id)
                        .sortedBy { it.name.value.lowercase() }

                    updateState {
                        WorkspaceSelectState.Content(
                            tenants = currentTenants,
                            firms = updatedFirms,
                            isCreatingFirm = false,
                        )
                    }
                    action(WorkspaceSelectAction.NavigateToBookkeeperConsole(createdFirm.id))
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to create firm during workspace setup" }
                    updateState {
                        WorkspaceSelectState.Content(
                            tenants = currentTenants,
                            firms = currentFirms,
                            isCreatingFirm = false,
                        )
                    }
                    action(WorkspaceSelectAction.ShowSelectionError(error.asDokusException))
                },
            )
        }
    }
}
