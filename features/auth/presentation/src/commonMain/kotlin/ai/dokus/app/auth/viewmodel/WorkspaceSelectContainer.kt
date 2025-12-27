package ai.dokus.app.auth.viewmodel

import ai.dokus.app.auth.datasource.TenantRemoteDataSource
import ai.dokus.app.auth.usecases.SelectTenantUseCase
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.TenantId
import ai.dokus.foundation.platform.Logger
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce

internal typealias WorkspaceSelectCtx = PipelineContext<WorkspaceSelectState, WorkspaceSelectIntent, WorkspaceSelectAction>

/**
 * Container for Workspace Selection screen using FlowMVI.
 * Manages loading available tenants and processing tenant selection.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class WorkspaceSelectContainer(
    private val tenantDataSource: TenantRemoteDataSource,
    private val selectTenantUseCase: SelectTenantUseCase,
) : Container<WorkspaceSelectState, WorkspaceSelectIntent, WorkspaceSelectAction> {

    private val logger = Logger.forClass<WorkspaceSelectContainer>()

    override val store: Store<WorkspaceSelectState, WorkspaceSelectIntent, WorkspaceSelectAction> =
        store(WorkspaceSelectState.Loading) {
            init {
                handleLoadTenants()
            }
            reduce { intent ->
                when (intent) {
                    is WorkspaceSelectIntent.LoadTenants -> handleLoadTenants()
                    is WorkspaceSelectIntent.SelectTenant -> handleSelectTenant(intent.tenantId)
                }
            }
        }

    private suspend fun WorkspaceSelectCtx.handleLoadTenants() {
        updateState { WorkspaceSelectState.Loading }

        logger.d { "Loading available tenants" }
        tenantDataSource.listMyTenants().fold(
            onSuccess = { tenants ->
                logger.i { "Loaded ${tenants.size} tenants" }
                updateState {
                    WorkspaceSelectState.Content(data = tenants)
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load tenants" }
                updateState {
                    WorkspaceSelectState.Error(
                        exception = error.asDokusException,
                        retryHandler = { intent(WorkspaceSelectIntent.LoadTenants) }
                    )
                }
            }
        )
    }

    private suspend fun WorkspaceSelectCtx.handleSelectTenant(tenantId: TenantId) {
        withState<WorkspaceSelectState.Content, _> {
            val currentTenants = tenants

            updateState {
                WorkspaceSelectState.Selecting(
                    tenants = currentTenants,
                    selectedTenantId = tenantId
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
                    action(WorkspaceSelectAction.ShowSelectionError(error.message ?: "Failed to select workspace"))
                    updateState {
                        WorkspaceSelectState.Content(data = currentTenants)
                    }
                }
            )
        }
    }
}
