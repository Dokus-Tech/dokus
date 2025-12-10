package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.app.core.state.DokusState
import ai.dokus.app.core.state.emit
import ai.dokus.app.core.state.emitLoading
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.ids.ClientId
import ai.dokus.foundation.domain.model.ClientDto
import ai.dokus.foundation.domain.model.ClientStats
import ai.dokus.foundation.domain.model.CreateClientRequest
import ai.dokus.foundation.domain.model.UpdateClientRequest
import ai.dokus.foundation.domain.model.common.PaginationState
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * ViewModel for client management.
 * Handles CRUD operations and Peppol configuration for clients.
 */
class ClientViewModel : BaseViewModel<DokusState<PaginationState<ClientDto>>>(DokusState.idle()), KoinComponent {

    private val logger = Logger.forClass<ClientViewModel>()
    private val dataSource: CashflowRemoteDataSource by inject()

    // Pagination state
    private val loadedClients = MutableStateFlow<List<ClientDto>>(emptyList())
    private val paginationState = MutableStateFlow(PaginationState<ClientDto>(pageSize = PAGE_SIZE))

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filter states
    private val _activeOnlyFilter = MutableStateFlow<Boolean?>(null)
    val activeOnlyFilter: StateFlow<Boolean?> = _activeOnlyFilter.asStateFlow()

    private val _peppolEnabledFilter = MutableStateFlow<Boolean?>(null)
    val peppolEnabledFilter: StateFlow<Boolean?> = _peppolEnabledFilter.asStateFlow()

    // Stats state (independent loading)
    private val _statsState = MutableStateFlow<DokusState<ClientStats>>(DokusState.idle())
    val statsState: StateFlow<DokusState<ClientStats>> = _statsState.asStateFlow()

    // Selected client for detail view
    private val _selectedClient = MutableStateFlow<DokusState<ClientDto>>(DokusState.idle())
    val selectedClient: StateFlow<DokusState<ClientDto>> = _selectedClient.asStateFlow()

    // ========================================================================
    // LIST OPERATIONS
    // ========================================================================

    /**
     * Load initial list of clients.
     */
    fun loadClients() {
        scope.launch {
            logger.d { "Loading clients" }
            paginationState.value = PaginationState(pageSize = PAGE_SIZE)
            loadedClients.value = emptyList()
            mutableState.emitLoading()
            loadPage(page = 0, reset = true)
        }
    }

    /**
     * Refresh the client list.
     */
    fun refresh() {
        loadClients()
        loadStats()
    }

    /**
     * Load the next page of clients.
     */
    fun loadNextPage() {
        val current = paginationState.value
        if (current.isLoadingMore || !current.hasMorePages) return

        scope.launch {
            logger.d { "Loading next client page (current=${current.currentPage})" }
            paginationState.value = current.copy(isLoadingMore = true)
            emitSuccess()
            loadPage(page = current.currentPage + 1, reset = false)
        }
    }

    private suspend fun loadPage(page: Int, reset: Boolean) {
        val offset = page * PAGE_SIZE
        val result = dataSource.listClients(
            search = _searchQuery.value.takeIf { it.isNotBlank() },
            activeOnly = _activeOnlyFilter.value,
            peppolEnabled = _peppolEnabledFilter.value,
            limit = PAGE_SIZE,
            offset = offset
        )

        result.fold(
            onSuccess = { response ->
                logger.i { "Loaded ${response.items.size} clients (offset=$offset, total=${response.total})" }
                loadedClients.value = if (reset) response.items else loadedClients.value + response.items

                paginationState.value = paginationState.value.copy(
                    currentPage = page,
                    isLoadingMore = false,
                    hasMorePages = response.hasMore,
                    pageSize = PAGE_SIZE
                )
                emitSuccess()
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load clients" }
                paginationState.value = paginationState.value.copy(
                    isLoadingMore = false,
                    hasMorePages = false
                )
                if (loadedClients.value.isEmpty()) {
                    mutableState.emit(error) { loadClients() }
                } else {
                    emitSuccess()
                }
            }
        )
    }

    // ========================================================================
    // SEARCH & FILTERS
    // ========================================================================

    /**
     * Update search query and reload.
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query.trim()
        loadClients()
    }

    /**
     * Update active-only filter.
     */
    fun setActiveOnlyFilter(activeOnly: Boolean?) {
        _activeOnlyFilter.value = activeOnly
        loadClients()
    }

    /**
     * Update Peppol-enabled filter.
     */
    fun setPeppolEnabledFilter(peppolEnabled: Boolean?) {
        _peppolEnabledFilter.value = peppolEnabled
        loadClients()
    }

    /**
     * Clear all filters.
     */
    fun clearFilters() {
        _searchQuery.value = ""
        _activeOnlyFilter.value = null
        _peppolEnabledFilter.value = null
        loadClients()
    }

    // ========================================================================
    // CRUD OPERATIONS
    // ========================================================================

    /**
     * Load a single client by ID.
     */
    fun loadClient(clientId: ClientId) {
        scope.launch {
            logger.d { "Loading client: $clientId" }
            _selectedClient.value = DokusState.loading()

            dataSource.getClient(clientId).fold(
                onSuccess = { client ->
                    logger.i { "Loaded client: ${client.name}" }
                    _selectedClient.value = DokusState.success(client)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load client: $clientId" }
                    _selectedClient.emitError(error) { scope.launch { loadClient(clientId) } }
                }
            )
        }
    }

    /**
     * Create a new client.
     */
    fun createClient(
        request: CreateClientRequest,
        onSuccess: (ClientDto) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        scope.launch {
            logger.d { "Creating client: ${request.name}" }

            dataSource.createClient(request).fold(
                onSuccess = { client ->
                    logger.i { "Client created: ${client.id}" }
                    onSuccess(client)
                    loadClients() // Refresh list
                    loadStats()
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to create client" }
                    onError(error)
                }
            )
        }
    }

    /**
     * Update an existing client.
     */
    fun updateClient(
        clientId: ClientId,
        request: UpdateClientRequest,
        onSuccess: (ClientDto) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        scope.launch {
            logger.d { "Updating client: $clientId" }

            dataSource.updateClient(clientId, request).fold(
                onSuccess = { client ->
                    logger.i { "Client updated: ${client.id}" }
                    _selectedClient.value = DokusState.success(client)
                    onSuccess(client)
                    loadClients() // Refresh list
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to update client: $clientId" }
                    onError(error)
                }
            )
        }
    }

    /**
     * Delete a client.
     */
    fun deleteClient(
        clientId: ClientId,
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        scope.launch {
            logger.d { "Deleting client: $clientId" }

            dataSource.deleteClient(clientId).fold(
                onSuccess = {
                    logger.i { "Client deleted: $clientId" }
                    onSuccess()
                    loadClients() // Refresh list
                    loadStats()
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to delete client: $clientId" }
                    onError(error)
                }
            )
        }
    }

    // ========================================================================
    // PEPPOL OPERATIONS
    // ========================================================================

    /**
     * Update a client's Peppol settings.
     */
    fun updateClientPeppol(
        clientId: ClientId,
        peppolId: String?,
        peppolEnabled: Boolean,
        onSuccess: (ClientDto) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        scope.launch {
            logger.d { "Updating client Peppol: $clientId, enabled=$peppolEnabled" }

            dataSource.updateClientPeppol(clientId, peppolId, peppolEnabled).fold(
                onSuccess = { client ->
                    logger.i { "Client Peppol updated: ${client.id}" }
                    _selectedClient.value = DokusState.success(client)
                    onSuccess(client)
                    loadClients() // Refresh list
                    loadStats()
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to update client Peppol: $clientId" }
                    onError(error)
                }
            )
        }
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    /**
     * Load client statistics.
     */
    fun loadStats() {
        scope.launch {
            logger.d { "Loading client stats" }
            _statsState.value = DokusState.loading()

            dataSource.getClientStats().fold(
                onSuccess = { stats ->
                    logger.d { "Loaded client stats: total=${stats.totalClients}" }
                    _statsState.value = DokusState.success(stats)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load client stats" }
                    _statsState.emitError(error) { scope.launch { loadStats() } }
                }
            )
        }
    }

    // ========================================================================
    // HELPERS
    // ========================================================================

    private fun emitSuccess() {
        val updatedState = paginationState.value.copy(
            data = loadedClients.value,
            pageSize = PAGE_SIZE
        )
        paginationState.value = updatedState
        mutableState.value = DokusState.success(updatedState)
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}

// Extension for MutableStateFlow<DokusState<T>>
private fun <T> MutableStateFlow<DokusState<T>>.emitError(
    error: Throwable,
    retryHandler: () -> Unit
) {
    value = DokusState.error(
        ai.dokus.foundation.domain.exceptions.DokusException.Unknown(error),
        retryHandler
    )
}
