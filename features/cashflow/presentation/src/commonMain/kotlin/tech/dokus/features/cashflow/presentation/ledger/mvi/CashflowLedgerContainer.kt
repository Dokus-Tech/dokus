package tech.dokus.features.cashflow.presentation.ledger.mvi

import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.Money
import tech.dokus.domain.config.BuildKonfig
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.CashflowPaymentRequest
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.features.cashflow.usecases.GetCashflowOverviewUseCase
import tech.dokus.features.cashflow.usecases.LoadCashflowEntriesUseCase
import tech.dokus.features.cashflow.usecases.RecordCashflowPaymentUseCase
import tech.dokus.foundation.platform.Logger
import kotlin.time.Clock
import tech.dokus.domain.enums.CashflowViewMode as DomainViewMode

internal typealias CashflowLedgerCtx = PipelineContext<CashflowLedgerState, CashflowLedgerIntent, CashflowLedgerAction>

private const val PAGE_SIZE = 50

/**
 * Container for CashflowLedgerScreen using FlowMVI.
 *
 * Manages cashflow entries (projection ledger):
 * - Loading entries from CashflowEntriesTable
 * - Filtering by date range, direction, status
 * - Pagination with load-more
 * - Deep link highlighting for entry IDs
 */
internal class CashflowLedgerContainer(
    private val loadCashflowEntries: LoadCashflowEntriesUseCase,
    private val getCashflowOverview: GetCashflowOverviewUseCase,
    private val recordPayment: RecordCashflowPaymentUseCase,
    private val highlightEntryId: CashflowEntryId? = null
) : Container<CashflowLedgerState, CashflowLedgerIntent, CashflowLedgerAction> {

    private val logger = Logger.forClass<CashflowLedgerContainer>()
    private var loadedEntries: List<CashflowEntry> = emptyList()
    private var paginationInfo = LocalPaginationInfo()
    private var currentFilters = CashflowFilters()
    private var loadJob: Job? = null
    private var pendingHighlightEntryId: CashflowEntryId? = highlightEntryId

    override val store: Store<CashflowLedgerState, CashflowLedgerIntent, CashflowLedgerAction> =
        store(CashflowLedgerState.Loading) {
            init {
                handleRefresh()
            }

            reduce { intent ->
                when (intent) {
                    is CashflowLedgerIntent.Refresh -> handleRefresh()
                    is CashflowLedgerIntent.LoadMore -> handleLoadMore()
                    is CashflowLedgerIntent.SetViewMode -> handleSetViewMode(intent.mode)
                    is CashflowLedgerIntent.SetDirectionFilter -> handleSetDirectionFilter(intent.direction)
                    is CashflowLedgerIntent.HighlightEntry -> handleHighlightEntry(intent.entryId)
                    is CashflowLedgerIntent.OpenEntry -> handleOpenEntry(intent.entry)
                    // Row actions menu intents
                    is CashflowLedgerIntent.ShowRowActions -> handleShowRowActions(intent.entryId)
                    is CashflowLedgerIntent.HideRowActions -> handleHideRowActions()
                    is CashflowLedgerIntent.RecordPaymentFor -> handleRecordPaymentFor(intent.entryId)
                    is CashflowLedgerIntent.MarkAsPaidQuick -> handleMarkAsPaidQuick(intent.entryId)
                    is CashflowLedgerIntent.ViewDocumentFor -> handleViewDocumentFor(intent.entry)
                }
            }
        }

    private suspend fun CashflowLedgerCtx.handleRefresh(
        keepContentIfAvailable: Boolean = true,
        fallbackFiltersOnFailure: CashflowFilters? = null,
    ) {
        loadJob?.cancel()
        logger.d {
            "Refreshing cashflow entries with viewMode=${currentFilters.viewMode}, direction=${currentFilters.direction}"
        }

        var previousContent: CashflowLedgerState.Content? = null
        withState<CashflowLedgerState.Content, _> {
            previousContent = this
        }
        val showInlineRefresh = keepContentIfAvailable && previousContent != null
        // Safe: sequential intent processing prevents races with handleLoadMore
        val previousEntries = loadedEntries
        val previousPaginationInfo = paginationInfo

        if (showInlineRefresh) {
            val contentToRefresh = requireNotNull(previousContent)
            updateState {
                contentToRefresh.copy(
                    filters = currentFilters,
                    isRefreshing = true,
                )
            }
        } else {
            loadedEntries = emptyList()
            paginationInfo = LocalPaginationInfo()
            updateState { CashflowLedgerState.Loading }
        }

        val (fromDate, toDate) = getDateRangeForViewMode(currentFilters.viewMode)
        val direction = mapDirectionFilter(currentFilters.direction)
        val domainViewMode = mapViewModeToDomain(currentFilters.viewMode)

        // Get statuses for API call - server now supports multi-status filtering
        val apiStatuses = getStatusesForViewMode(currentFilters.viewMode)

        // Parallel fetch: overview + entries
        coroutineScope {
            val overviewDeferred = async {
                getCashflowOverview(
                    viewMode = domainViewMode,
                    fromDate = fromDate,
                    toDate = toDate,
                    direction = direction,
                    statuses = apiStatuses
                )
            }
            val entriesDeferred = async {
                loadCashflowEntries(
                    page = 0,
                    pageSize = PAGE_SIZE,
                    viewMode = domainViewMode,
                    fromDate = fromDate,
                    toDate = toDate,
                    direction = direction,
                    statuses = apiStatuses,
                    sourceType = null
                )
            }

            val overviewResult = overviewDeferred.await()
            val entriesResult = entriesDeferred.await()

            if (overviewResult.isSuccess && entriesResult.isSuccess) {
                val overview = overviewResult.getOrThrow()
                val response = entriesResult.getOrThrow()

                // Server returns entries already filtered and sorted
                loadedEntries = response.items
                paginationInfo = paginationInfo.copy(
                    currentPage = 0,
                    isLoadingMore = false,
                    hasMorePages = response.hasMore
                )

                val resolvedHighlightId = pendingHighlightEntryId?.let { requestedEntryId ->
                    if (loadedEntries.none { it.id == requestedEntryId }) {
                        val highlightEntry = loadCashflowEntries(
                            page = 0,
                            pageSize = 1,
                            entryId = requestedEntryId
                        ).getOrNull()?.items?.firstOrNull()

                        if (highlightEntry != null) {
                            loadedEntries = (listOf(highlightEntry) + loadedEntries).distinctBy { it.id }
                        }
                    }
                    requestedEntryId.takeIf { id -> loadedEntries.any { it.id == id } }
                }
                pendingHighlightEntryId = null

                // Summary comes from server - display directly
                val summary = CashflowSummary(
                    periodLabel = when (currentFilters.viewMode) {
                        CashflowViewMode.Upcoming -> "NEXT 30 DAYS"
                        CashflowViewMode.Overdue -> "OVERDUE"
                        CashflowViewMode.History -> "LAST 30 DAYS"
                    },
                    netAmount = overview.netCashflow,
                    totalIn = overview.cashIn.total,
                    totalOut = overview.cashOut.total
                )

                updateState {
                    val currentContent = this as? CashflowLedgerState.Content

                    CashflowLedgerState.Content(
                        entries = buildPaginationState(),
                        filters = currentFilters,
                        summary = summary,
                        balance = getMockBalanceIfEnabled(),
                        highlightedEntryId = resolvedHighlightId,
                        isRefreshing = false,
                        actionsEntryId = currentContent?.actionsEntryId?.takeIf { id ->
                            loadedEntries.any { it.id == id }
                        },
                    )
                }
            } else {
                // Handle error - prefer entries error if both failed
                val error = entriesResult.exceptionOrNull() ?: overviewResult.exceptionOrNull()!!
                logger.e(error) { "Failed to load cashflow entries or overview" }
                val dokusError = error.asDokusException
                if (showInlineRefresh) {
                    loadedEntries = previousEntries
                    paginationInfo = previousPaginationInfo
                    fallbackFiltersOnFailure?.let { currentFilters = it }
                    val contentToRestore = requireNotNull(previousContent)
                    updateState {
                        contentToRestore.copy(
                            entries = buildPaginationState(),
                            filters = currentFilters,
                            isRefreshing = false,
                            actionsEntryId = null
                        )
                    }
                    action(CashflowLedgerAction.ShowError(dokusError))
                } else {
                    updateState {
                        CashflowLedgerState.Error(
                            exception = dokusError,
                            retryHandler = { intent(CashflowLedgerIntent.Refresh) }
                        )
                    }
                }
            }
        }
    }

    private suspend fun CashflowLedgerCtx.handleLoadMore() {
        withState<CashflowLedgerState.Content, _> {
            if (paginationInfo.isLoadingMore || !paginationInfo.hasMorePages) return@withState

            val nextPage = paginationInfo.currentPage + 1
            logger.d { "Loading more entries, page=$nextPage" }

            paginationInfo = paginationInfo.copy(isLoadingMore = true)
            updateState { copy(entries = buildPaginationState()) }

            val (fromDate, toDate) = getDateRangeForViewMode(currentFilters.viewMode)
            val direction = mapDirectionFilter(currentFilters.direction)
            val domainViewMode = mapViewModeToDomain(currentFilters.viewMode)
            val apiStatuses = getStatusesForViewMode(currentFilters.viewMode)

            loadJob = launch {
                loadCashflowEntries(
                    page = nextPage,
                    pageSize = PAGE_SIZE,
                    viewMode = domainViewMode,
                    fromDate = fromDate,
                    toDate = toDate,
                    direction = direction,
                    statuses = apiStatuses,
                    sourceType = null
                ).fold(
                    onSuccess = { response ->
                        // Server returns entries already sorted
                        loadedEntries = loadedEntries + response.items
                        paginationInfo = paginationInfo.copy(
                            currentPage = nextPage,
                            isLoadingMore = false,
                            hasMorePages = response.hasMore
                        )

                        // Summary stays the same (from overview endpoint), just update entries
                        updateState { copy(entries = buildPaginationState()) }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to load more entries" }
                        paginationInfo = paginationInfo.copy(isLoadingMore = false)
                        updateState { copy(entries = buildPaginationState()) }
                        action(CashflowLedgerAction.ShowError(error.asDokusException))
                    }
                )
            }
        }
    }

    private suspend fun CashflowLedgerCtx.handleSetViewMode(mode: CashflowViewMode) {
        if (currentFilters.viewMode == mode) return
        val previousFilters = currentFilters
        currentFilters = currentFilters.copy(viewMode = mode)
        handleRefresh(
            keepContentIfAvailable = true,
            fallbackFiltersOnFailure = previousFilters,
        )
    }

    private suspend fun CashflowLedgerCtx.handleSetDirectionFilter(direction: DirectionFilter) {
        if (currentFilters.direction == direction) return
        val previousFilters = currentFilters
        currentFilters = currentFilters.copy(direction = direction)
        handleRefresh(
            keepContentIfAvailable = true,
            fallbackFiltersOnFailure = previousFilters,
        )
    }

    private suspend fun CashflowLedgerCtx.handleHighlightEntry(entryId: CashflowEntryId?) {
        withState<CashflowLedgerState.Content, _> {
            updateState { copy(highlightedEntryId = entryId) }
        }
    }

    private suspend fun CashflowLedgerCtx.handleOpenEntry(entry: CashflowEntry) {
        // Primary action: Navigate to document review
        if (entry.documentId != null) {
            action(CashflowLedgerAction.NavigateToDocumentReview(entry.documentId.toString()))
        } else {
            // For entries without a document (e.g., manual entries), navigate to entity
            action(CashflowLedgerAction.NavigateToEntity(entry.sourceType, entry.sourceId))
        }
    }

    // Row actions menu handlers

    private suspend fun CashflowLedgerCtx.handleShowRowActions(entryId: CashflowEntryId) {
        withState<CashflowLedgerState.Content, _> {
            updateState { copy(actionsEntryId = entryId) }
        }
    }

    private suspend fun CashflowLedgerCtx.handleHideRowActions() {
        withState<CashflowLedgerState.Content, _> {
            updateState { copy(actionsEntryId = null) }
        }
    }

    private suspend fun CashflowLedgerCtx.handleRecordPaymentFor(entryId: CashflowEntryId) {
        // Close actions menu and navigate to document review (which has payment recording)
        withState<CashflowLedgerState.Content, _> {
            val entry = entries.data.find { it.id == entryId } ?: return@withState
            updateState { copy(actionsEntryId = null) }
            if (entry.documentId != null) {
                action(CashflowLedgerAction.NavigateToDocumentReview(entry.documentId.toString()))
            } else {
                action(CashflowLedgerAction.NavigateToEntity(entry.sourceType, entry.sourceId))
            }
        }
    }

    private suspend fun CashflowLedgerCtx.handleMarkAsPaidQuick(entryId: CashflowEntryId) {
        // Close actions menu first
        withState<CashflowLedgerState.Content, _> {
            updateState { copy(actionsEntryId = null) }
        }

        // Then mark the entry as paid with full amount
        withState<CashflowLedgerState.Content, _> {
            val entry = entries.data.find { it.id == entryId } ?: return@withState
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

            recordPayment(
                entryId = entryId,
                request = CashflowPaymentRequest(
                    amount = entry.remainingAmount,
                    paidAt = today.atTime(0, 0),
                    note = null
                )
            ).fold(
                onSuccess = { updatedEntry ->
                    val updatedList = entries.data.map {
                        if (it.id == updatedEntry.id) updatedEntry else it
                    }
                    loadedEntries = loadedEntries.map {
                        if (it.id == updatedEntry.id) updatedEntry else it
                    }
                    updateState { copy(entries = entries.copy(data = updatedList)) }
                    action(CashflowLedgerAction.ShowPaymentSuccess(updatedEntry))
                },
                onFailure = { error ->
                    action(CashflowLedgerAction.ShowPaymentError(error.asDokusException))
                }
            )
        }
    }

    private suspend fun CashflowLedgerCtx.handleViewDocumentFor(entry: CashflowEntry) {
        // Close actions menu and navigate to document
        withState<CashflowLedgerState.Content, _> {
            updateState { copy(actionsEntryId = null) }
        }

        if (entry.documentId != null) {
            action(CashflowLedgerAction.NavigateToDocumentReview(entry.documentId.toString()))
        } else {
            action(CashflowLedgerAction.NavigateToEntity(entry.sourceType, entry.sourceId))
        }
    }

    private fun buildPaginationState(): PaginationState<CashflowEntry> {
        return PaginationState(
            data = loadedEntries,
            currentPage = paginationInfo.currentPage,
            pageSize = PAGE_SIZE,
            isLoadingMore = paginationInfo.isLoadingMore,
            hasMorePages = paginationInfo.hasMorePages
        )
    }

    /**
     * Get date range based on view mode.
     * - Upcoming: [today, today+30d]
     * - Overdue: [today, today] (range ignored server-side; overdue is eventDate < today)
     * - History: [today-30d, today] (uses paidAt for filtering)
     */
    private fun getDateRangeForViewMode(viewMode: CashflowViewMode): Pair<LocalDate, LocalDate> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return when (viewMode) {
            CashflowViewMode.Upcoming -> today to today.plus(30, DateTimeUnit.DAY)
            CashflowViewMode.Overdue -> today to today
            CashflowViewMode.History -> today.plus(-30, DateTimeUnit.DAY) to today
        }
    }

    /**
     * Get status filters based on view mode.
     * - Upcoming: Open, Overdue (money not yet moved)
     * - Overdue: Open, Overdue (money not yet moved, due date in past)
     * - History: Paid (money already moved)
     */
    private fun getStatusesForViewMode(viewMode: CashflowViewMode): List<CashflowEntryStatus> {
        return when (viewMode) {
            CashflowViewMode.Upcoming -> listOf(CashflowEntryStatus.Open, CashflowEntryStatus.Overdue)
            CashflowViewMode.Overdue -> listOf(CashflowEntryStatus.Open, CashflowEntryStatus.Overdue)
            CashflowViewMode.History -> listOf(CashflowEntryStatus.Paid)
        }
    }

    /**
     * Map DirectionFilter to CashflowDirection for API call.
     */
    private fun mapDirectionFilter(filter: DirectionFilter): CashflowDirection? {
        return when (filter) {
            DirectionFilter.All -> null
            DirectionFilter.In -> CashflowDirection.In
            DirectionFilter.Out -> CashflowDirection.Out
        }
    }

    /**
     * Map local CashflowViewMode to domain CashflowViewMode for API call.
     */
    private fun mapViewModeToDomain(viewMode: CashflowViewMode): DomainViewMode {
        return when (viewMode) {
            CashflowViewMode.Upcoming -> DomainViewMode.Upcoming
            CashflowViewMode.Overdue -> DomainViewMode.Overdue
            CashflowViewMode.History -> DomainViewMode.History
        }
    }

    private data class LocalPaginationInfo(
        val currentPage: Int = 0,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = true
    )
}

/**
 * Returns mock balance state when SHOW_BALANCE_MOCK is enabled (local dev).
 * Returns null in production (no banking integration yet).
 */
private fun getMockBalanceIfEnabled(): BalanceState? {
    return if (BuildKonfig.SHOW_BALANCE_MOCK) {
        BalanceState(
            amount = Money(1248234), // €12,482.34 (minor units = cents)
            asOf = LocalDate(2026, 1, 15),
            accountName = "KBC"
        )
    } else {
        null
    }
}
