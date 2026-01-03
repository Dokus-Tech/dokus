package tech.dokus.app.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCase
import tech.dokus.features.cashflow.presentation.cashflow.model.usecase.WatchPendingDocumentsUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

internal typealias DashboardCtx = PipelineContext<DashboardState, DashboardIntent, DashboardAction>

/**
 * Container for Dashboard screen using FlowMVI.
 *
 * Manages the dashboard data including:
 * - Current tenant information
 * - Pending documents for processing
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class DashboardContainer(
    private val getCurrentTenantUseCase: GetCurrentTenantUseCase,
    private val watchPendingDocuments: WatchPendingDocumentsUseCase,
) : Container<DashboardState, DashboardIntent, DashboardAction> {

    private val logger = Logger.forClass<DashboardContainer>()

    // Internal state for pending documents pagination
    private val allPendingDocuments = MutableStateFlow<List<DocumentRecordDto>>(emptyList())
    private val pendingVisibleCount = MutableStateFlow(DashboardState.PENDING_PAGE_SIZE)

    override val store: Store<DashboardState, DashboardIntent, DashboardAction> =
        store(DashboardState.Content()) {
            init {
                // Start watching pending documents on init
                launchWatchPendingDocuments()
            }

            reduce { intent ->
                when (intent) {
                    is DashboardIntent.RefreshTenant -> handleRefreshTenant()
                    is DashboardIntent.RefreshPendingDocuments -> handleRefreshPendingDocuments()
                    is DashboardIntent.LoadMorePendingDocuments -> handleLoadMorePendingDocuments()
                }
            }
        }

    private suspend fun DashboardCtx.launchWatchPendingDocuments() {
        launch {
            watchPendingDocuments().collectLatest { state ->
                when (state) {
                    is DokusState.Loading -> {
                        withState<DashboardState.Content, _> {
                            updateState { copy(pendingDocumentsState = DokusState.loading()) }
                        }
                    }

                    is DokusState.Success -> {
                        allPendingDocuments.value = state.data
                        pendingVisibleCount.value = DashboardState.PENDING_PAGE_SIZE
                        updatePendingPaginationState()
                    }

                    is DokusState.Error -> {
                        allPendingDocuments.value = emptyList()
                        withState<DashboardState.Content, _> {
                            updateState {
                                copy(
                                    pendingDocumentsState = DokusState.error(
                                        state.exception,
                                        state.retryHandler
                                    )
                                )
                            }
                        }
                    }

                    is DokusState.Idle -> {
                        withState<DashboardState.Content, _> {
                            updateState { copy(pendingDocumentsState = DokusState.idle()) }
                        }
                    }
                }
            }
        }
    }

    private suspend fun DashboardCtx.handleRefreshTenant() {
        withState<DashboardState.Content, _> {
            logger.d { "Refreshing tenant" }

            updateState { copy(tenantState = DokusState.loading()) }

            getCurrentTenantUseCase().fold(
                onSuccess = { tenant ->
                    logger.d { "Tenant loaded: ${tenant?.displayName}" }
                    updateState {
                        copy(
                            tenantState = DokusState.success(tenant),
                            currentAvatar = tenant?.avatar
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load tenant" }
                    updateState {
                        copy(
                            tenantState = DokusState.error(error) {
                                intent(DashboardIntent.RefreshTenant)
                            }
                        )
                    }
                }
            )
        }
    }

    private fun handleRefreshPendingDocuments() {
        logger.d { "Refreshing pending documents" }
        watchPendingDocuments.refresh()
    }

    private suspend fun DashboardCtx.handleLoadMorePendingDocuments() {
        val allDocs = allPendingDocuments.value
        val currentVisible = pendingVisibleCount.value

        // Don't load more if we're already showing all items
        if (currentVisible >= allDocs.size) return

        logger.d { "Loading more pending documents" }

        // Increase visible count
        pendingVisibleCount.value = (currentVisible + DashboardState.PENDING_PAGE_SIZE)
            .coerceAtMost(allDocs.size)

        updatePendingPaginationState()
    }

    private suspend fun DashboardCtx.updatePendingPaginationState() {
        val allDocs = allPendingDocuments.value
        val visibleCount = pendingVisibleCount.value
        val visibleDocs = allDocs.take(visibleCount)
        val hasMore = visibleCount < allDocs.size

        val paginationState = PaginationState(
            data = visibleDocs,
            currentPage = visibleCount / DashboardState.PENDING_PAGE_SIZE,
            pageSize = DashboardState.PENDING_PAGE_SIZE,
            hasMorePages = hasMore,
            isLoadingMore = false
        )

        withState<DashboardState.Content, _> {
            updateState {
                copy(pendingDocumentsState = DokusState.success(paginationState))
            }
        }
    }
}
