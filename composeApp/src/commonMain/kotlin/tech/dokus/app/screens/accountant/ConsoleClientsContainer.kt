package tech.dokus.app.screens.accountant

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.auth.ConsoleClientSummary
import tech.dokus.features.auth.usecases.GetAccountMeUseCase
import tech.dokus.features.auth.usecases.GetConsoleClientDocumentUseCase
import tech.dokus.features.auth.usecases.ListConsoleClientDocumentsUseCase
import tech.dokus.features.auth.usecases.ListConsoleClientsUseCase
import tech.dokus.foundation.app.shell.WorkspaceContextStore
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

internal typealias ConsoleClientsCtx =
    PipelineContext<ConsoleClientsState, ConsoleClientsIntent, ConsoleClientsAction>

internal class ConsoleClientsContainer(
    private val getAccountMeUseCase: GetAccountMeUseCase,
    private val listConsoleClientsUseCase: ListConsoleClientsUseCase,
    private val listConsoleClientDocumentsUseCase: ListConsoleClientDocumentsUseCase,
    private val getConsoleClientDocumentUseCase: GetConsoleClientDocumentUseCase,
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
                    ConsoleClientsIntent.BackToClients -> handleBackToClients()
                    is ConsoleClientsIntent.OpenDocument -> handleOpenDocument(intent.documentId)
                }
            }
        }

    private suspend fun ConsoleClientsCtx.handleRefresh() {
        updateState { ConsoleClientsState.Loading }

        val accountMe = getAccountMeUseCase().getOrElse { error ->
            logger.e(error) { "Failed to resolve account context for console" }
            updateState {
                ConsoleClientsState.Error(
                    exception = error.asDokusException,
                    retryHandler = { intent(ConsoleClientsIntent.Refresh) }
                )
            }
            return
        }

        val selectedFirmId = WorkspaceContextStore.state.value.selectedFirmId
        val firm = accountMe.firms.firstOrNull { it.id == selectedFirmId }
            ?: accountMe.firms.firstOrNull()
        if (firm == null) {
            updateState {
                ConsoleClientsState.Error(
                    exception = DokusException.WorkspaceContextUnavailable,
                    retryHandler = { intent(ConsoleClientsIntent.Refresh) }
                )
            }
            return
        }
        WorkspaceContextStore.selectFirmWorkspace(firm.id)

        listConsoleClientsUseCase(firm.id).fold(
            onSuccess = { clients ->
                updateState {
                    ConsoleClientsState.Content(
                        firmId = firm.id,
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
            updateState {
                copy(
                    selectedClientTenantId = tenantId,
                    documentsState = DokusState.loading(),
                    selectedDocument = null,
                    loadingDocumentId = null,
                )
            }

            listConsoleClientDocumentsUseCase(
                firmId = firmId,
                tenantId = tenantId,
                page = 0,
                limit = 50,
            ).fold(
                onSuccess = { paginated ->
                    updateState {
                        copy(
                            selectedClientTenantId = tenantId,
                            documentsState = DokusState.success(paginated.items),
                            selectedDocument = null,
                            loadingDocumentId = null,
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load console client documents" }
                    val exception = error.asDokusException
                    updateState {
                        copy(
                            selectedClientTenantId = tenantId,
                            documentsState = DokusState.error(exception) {
                                intent(ConsoleClientsIntent.SelectClient(tenantId))
                            },
                            selectedDocument = null,
                            loadingDocumentId = null,
                        )
                    }
                    action(ConsoleClientsAction.ShowError(exception))
                }
            )
        }
    }

    private suspend fun ConsoleClientsCtx.handleOpenDocument(documentId: String) {
        withState<ConsoleClientsState.Content, _> {
            val tenantId = selectedClientTenantId ?: return@withState

            updateState { copy(loadingDocumentId = documentId) }

            getConsoleClientDocumentUseCase(
                firmId = firmId,
                tenantId = tenantId,
                documentId = documentId,
            ).fold(
                onSuccess = { document ->
                    updateState {
                        copy(
                            selectedDocument = document,
                            loadingDocumentId = null,
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load console document detail" }
                    updateState { copy(loadingDocumentId = null) }
                    action(ConsoleClientsAction.ShowError(error.asDokusException))
                }
            )
        }
    }

    private suspend fun ConsoleClientsCtx.handleBackToClients() {
        withState<ConsoleClientsState.Content, _> {
            updateState {
                copy(
                    selectedClientTenantId = null,
                    documentsState = DokusState.idle(),
                    selectedDocument = null,
                    loadingDocumentId = null,
                )
            }
        }
    }
}
