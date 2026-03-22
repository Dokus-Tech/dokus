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
import tech.dokus.domain.ids.FirmId
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
        store(ConsoleClientsState.initial) {
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
                    is ConsoleClientsIntent.DismissActionError -> updateState { copy(actionError = null) }
                }
            }
        }

    private suspend fun ConsoleClientsCtx.handleRefresh() {
        updateState { copy(clients = clients.asLoading) }

        val accountMe = getAccountMeUseCase().getOrElse { error ->
            logger.e(error) { "Failed to resolve account context for console" }
            updateState {
                copy(
                    clients = DokusState.error(
                        exception = error.asDokusException,
                        retryHandler = { intent(ConsoleClientsIntent.Refresh) },
                    )
                )
            }
            return
        }

        val selectedFirmId = WorkspaceContextStore.state.value.selectedFirmId
        val firm = accountMe.firms.firstOrNull { it.id == selectedFirmId }
            ?: accountMe.firms.firstOrNull()
        if (firm == null) {
            updateState {
                copy(
                    clients = DokusState.error(
                        exception = DokusException.WorkspaceContextUnavailable,
                        retryHandler = { intent(ConsoleClientsIntent.Refresh) },
                    )
                )
            }
            return
        }
        listConsoleClientsUseCase(firm.id).fold(
            onSuccess = { clients ->
                updateState {
                    copy(
                        firmId = firm.id,
                        firmName = firm.name.value,
                        clients = DokusState.success(
                            clients.sortedWith(
                                compareBy<ConsoleClientSummary> {
                                    it.companyName.value.lowercase()
                                }.thenBy {
                                    it.vatNumber?.value?.lowercase() ?: ""
                                }.thenBy {
                                    it.tenantId.toString()
                                }
                            )
                        ),
                    )
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load console clients" }
                updateState {
                    copy(
                        clients = DokusState.error(
                            exception = error.asDokusException,
                            retryHandler = { intent(ConsoleClientsIntent.Refresh) },
                        )
                    )
                }
            }
        )
    }

    private suspend fun ConsoleClientsCtx.handleUpdateQuery(query: String) {
        updateState { copy(query = query) }
    }

    private suspend fun ConsoleClientsCtx.handleSelectClient(tenantId: TenantId) {
        var currentFirmId: FirmId? = null
        withState { currentFirmId = firmId }
        val firmId = currentFirmId ?: return
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
                        documentsState = DokusState.error(
                            exception = exception,
                            retryHandler = { intent(ConsoleClientsIntent.SelectClient(tenantId)) },
                        ),
                        selectedDocument = null,
                        loadingDocumentId = null,
                        actionError = exception,
                    )
                }
            }
        )
    }

    private suspend fun ConsoleClientsCtx.handleOpenDocument(documentId: String) {
        var currentFirmId: FirmId? = null
        var currentTenantId: TenantId? = null
        withState {
            currentFirmId = firmId
            currentTenantId = selectedClientTenantId
        }
        val firmId = currentFirmId ?: return
        val tenantId = currentTenantId ?: return

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
                updateState { copy(loadingDocumentId = null, actionError = error.asDokusException) }
            }
        )
    }

    private suspend fun ConsoleClientsCtx.handleBackToClients() {
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
