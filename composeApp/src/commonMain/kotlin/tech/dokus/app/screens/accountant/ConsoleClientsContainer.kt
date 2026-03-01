package tech.dokus.app.screens.accountant

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.auth.ConsoleClientSummary
import tech.dokus.features.auth.usecases.ListConsoleClientsUseCase
import tech.dokus.features.auth.usecases.SelectTenantUseCase
import tech.dokus.foundation.platform.Logger

internal typealias ConsoleClientsCtx =
    PipelineContext<ConsoleClientsState, ConsoleClientsIntent, ConsoleClientsAction>

internal class ConsoleClientsContainer(
    private val listConsoleClientsUseCase: ListConsoleClientsUseCase,
    private val selectTenantUseCase: SelectTenantUseCase,
) : Container<ConsoleClientsState, ConsoleClientsIntent, ConsoleClientsAction> {

    private val logger = Logger.forClass<ConsoleClientsContainer>()

    override val store: Store<ConsoleClientsState, ConsoleClientsIntent, ConsoleClientsAction> =
        store(ConsoleClientsState.Loading) {
            init {
                handleRefresh()
            }

            reduce { intent ->
                when (intent) {
                    ConsoleClientsIntent.Refresh -> handleRefresh()
                    is ConsoleClientsIntent.UpdateQuery -> handleUpdateQuery(intent.query)
                    is ConsoleClientsIntent.SelectClient -> handleSelectClient(intent.tenantId)
                }
            }
        }

    private suspend fun ConsoleClientsCtx.handleRefresh() {
        updateState { ConsoleClientsState.Loading }

        listConsoleClientsUseCase().fold(
            onSuccess = { clients ->
                updateState {
                    ConsoleClientsState.Content(
                        clients = clients.sortedWith(
                            compareBy<ConsoleClientSummary> {
                                it.companyName.value.lowercase()
                            }.thenBy {
                                it.vatNumber?.value?.lowercase() ?: ""
                            }.thenBy {
                                it.tenantId.toString()
                            }
                        )
                    )
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load console clients" }
                updateState {
                    ConsoleClientsState.Error(
                        exception = error.asDokusException,
                        retryHandler = { intent(ConsoleClientsIntent.Refresh) }
                    )
                }
            }
        )
    }

    private suspend fun ConsoleClientsCtx.handleUpdateQuery(query: String) {
        withState<ConsoleClientsState.Content, _> {
            updateState { copy(query = query) }
        }
    }

    private suspend fun ConsoleClientsCtx.handleSelectClient(tenantId: TenantId) {
        withState<ConsoleClientsState.Content, _> {
            if (selectingTenantId != null) return@withState

            updateState { copy(selectingTenantId = tenantId) }

            selectTenantUseCase(tenantId).fold(
                onSuccess = {
                    action(ConsoleClientsAction.NavigateToDocuments)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to select console client tenant: $tenantId" }
                    updateState { copy(selectingTenantId = null) }
                    action(ConsoleClientsAction.ShowError(error.asDokusException))
                }
            )
        }
    }
}
