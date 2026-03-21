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
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.config.BuildKonfig
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.model.CashflowEntryDto
import tech.dokus.domain.model.CashflowPaymentRequest
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.features.cashflow.usecases.GetCashflowOverviewUseCase
import tech.dokus.features.cashflow.usecases.LoadCashflowEntriesUseCase
import tech.dokus.features.cashflow.usecases.RecordCashflowPaymentUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isSuccess
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
    private var loadJob: Job? = null
    private var pendingHighlightEntryId: CashflowEntryId? = highlightEntryId

    override val store: Store<CashflowLedgerState, CashflowLedgerIntent, CashflowLedgerAction> =
        store(CashflowLedgerState.initial) {
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
                    is CashflowLedgerIntent.ShowRowActions -> handleShowRowActions(intent.entryId)
                    is CashflowLedgerIntent.HideRowActions -> handleHideRowActions()
                    is CashflowLedgerIntent.RecordPaymentFor -> handleRecordPaymentFor(intent.entryId)
                    is CashflowLedgerIntent.MarkAsPaidQuick -> handleMarkAsPaidQuick(intent.entryId)
                    is CashflowLedgerIntent.ViewDocumentFor -> handleViewDocumentFor(intent.entry)
                }
            }
        }

    private suspend fun CashflowLedgerCtx.handleRefresh() {
        loadJob?.cancel()
        updateState { copy(entries = entries.asLoading) }
        loadEntries(page = 0, reset = true)
    }

    private suspend fun CashflowLedgerCtx.handleLoadMore() {
        withState {
            val paginationState =
                entries.let { if (it.isSuccess()) it.data else return@withState }

            if (!paginationState.hasMorePages) return@withState

            updateState { copy(entries = entries.asLoading) }

            val nextPage = paginationState.currentPage + 1
            loadEntries(page = nextPage, reset = false)
        }
    }

    private suspend fun CashflowLedgerCtx.handleSetViewMode(mode: CashflowViewMode) {
        withState {
            if (filters.viewMode == mode) return@withState
            loadJob?.cancel()
            updateState { copy(entries = entries.asLoading, filters = filters.copy(viewMode = mode)) }
            loadEntries(page = 0, reset = true)
        }
    }

    private suspend fun CashflowLedgerCtx.handleSetDirectionFilter(direction: DirectionFilter) {
        withState {
            if (filters.direction == direction) return@withState
            loadJob?.cancel()
            updateState { copy(entries = entries.asLoading, filters = filters.copy(direction = direction)) }
            loadEntries(page = 0, reset = true)
        }
    }

    private suspend fun CashflowLedgerCtx.handleHighlightEntry(entryId: CashflowEntryId?) {
        updateState { copy(highlightedEntryId = entryId) }
    }

    private suspend fun CashflowLedgerCtx.handleOpenEntry(entry: CashflowEntryDto) {
        if (entry.documentId != null) {
            action(CashflowLedgerAction.NavigateToDocumentReview(entry.documentId!!))
        } else {
            action(CashflowLedgerAction.NavigateToEntity(entry.sourceType, entry.sourceId))
        }
    }

    private suspend fun CashflowLedgerCtx.handleShowRowActions(entryId: CashflowEntryId) {
        updateState { copy(actionsEntryId = entryId) }
    }

    private suspend fun CashflowLedgerCtx.handleHideRowActions() {
        updateState { copy(actionsEntryId = null) }
    }

    private suspend fun CashflowLedgerCtx.handleRecordPaymentFor(entryId: CashflowEntryId) {
        withState {
            val entry = entries.lastData?.data?.find { it.id == entryId } ?: return@withState
            updateState { copy(actionsEntryId = null) }
            if (entry.documentId != null) {
                action(CashflowLedgerAction.NavigateToDocumentReview(entry.documentId!!))
            } else {
                action(CashflowLedgerAction.NavigateToEntity(entry.sourceType, entry.sourceId))
            }
        }
    }

    private suspend fun CashflowLedgerCtx.handleMarkAsPaidQuick(entryId: CashflowEntryId) {
        withState {
            updateState { copy(actionsEntryId = null) }

            val entry = entries.lastData?.data?.find { it.id == entryId } ?: return@withState
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
                    val currentPagination = entries.lastData ?: return@fold
                    val updatedList = currentPagination.data.map {
                        if (it.id == updatedEntry.id) updatedEntry else it
                    }
                    updateState {
                        copy(entries = DokusState.success(currentPagination.copy(data = updatedList)))
                    }
                    action(CashflowLedgerAction.ShowPaymentSuccess(updatedEntry))
                },
                onFailure = { error ->
                    action(CashflowLedgerAction.ShowPaymentError(error.asDokusException))
                }
            )
        }
    }

    private suspend fun CashflowLedgerCtx.handleViewDocumentFor(entry: CashflowEntryDto) {
        updateState { copy(actionsEntryId = null) }

        if (entry.documentId != null) {
            action(CashflowLedgerAction.NavigateToDocumentReview(entry.documentId!!))
        } else {
            action(CashflowLedgerAction.NavigateToEntity(entry.sourceType, entry.sourceId))
        }
    }

    private suspend fun CashflowLedgerCtx.loadEntries(page: Int, reset: Boolean) {
        withState {
            val viewMode = filters.viewMode
            val direction = filters.direction
            val (fromDate, toDate) = getDateRangeForViewMode(viewMode)
            val mappedDirection = mapDirectionFilter(direction)
            val domainViewMode = mapViewModeToDomain(viewMode)
            val apiStatuses = getStatusesForViewMode(viewMode)

            if (reset) {
                logger.d { "Refreshing cashflow entries with viewMode=$viewMode, direction=$direction" }

                coroutineScope {
                    val overviewDeferred = async {
                        getCashflowOverview(
                            viewMode = domainViewMode,
                            fromDate = fromDate,
                            toDate = toDate,
                            direction = mappedDirection,
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
                            direction = mappedDirection,
                            statuses = apiStatuses,
                            sourceType = null
                        )
                    }

                    val overviewResult = overviewDeferred.await()
                    val entriesResult = entriesDeferred.await()

                    if (overviewResult.isSuccess && entriesResult.isSuccess) {
                        val overview = overviewResult.getOrThrow()
                        val response = entriesResult.getOrThrow()

                        var newEntries = response.items

                        val resolvedHighlightId = pendingHighlightEntryId?.let { requestedEntryId ->
                            if (newEntries.none { it.id == requestedEntryId }) {
                                val highlightEntry = loadCashflowEntries(
                                    page = 0,
                                    pageSize = 1,
                                    entryId = requestedEntryId
                                ).getOrNull()?.items?.firstOrNull()

                                if (highlightEntry != null) {
                                    newEntries = (listOf(highlightEntry) + newEntries).distinctBy { it.id }
                                }
                            }
                            requestedEntryId.takeIf { id -> newEntries.any { it.id == id } }
                        }
                        pendingHighlightEntryId = null

                        val summary = CashflowSummary(
                            periodLabel = when (viewMode) {
                                CashflowViewMode.Upcoming -> "NEXT 30 DAYS"
                                CashflowViewMode.Overdue -> "OVERDUE"
                                CashflowViewMode.History -> "LAST 30 DAYS"
                            },
                            netAmount = overview.netCashflow,
                            totalIn = overview.cashIn.total,
                            totalOut = overview.cashOut.total
                        )

                        updateState {
                            copy(
                                entries = DokusState.success(
                                    PaginationState(
                                        data = newEntries,
                                        currentPage = 0,
                                        pageSize = PAGE_SIZE,
                                        hasMorePages = response.hasMore
                                    )
                                ),
                                summary = summary,
                                balance = getMockBalanceIfEnabled(),
                                highlightedEntryId = resolvedHighlightId,
                                actionsEntryId = actionsEntryId?.takeIf { id ->
                                    newEntries.any { it.id == id }
                                }
                            )
                        }
                    } else {
                        val error = entriesResult.exceptionOrNull() ?: overviewResult.exceptionOrNull()!!
                        logger.e(error) { "Failed to load cashflow entries or overview" }
                        val dokusError = error.asDokusException

                        val hadData = entries.lastData != null
                        updateState {
                            copy(
                                entries = DokusState.error(
                                    exception = dokusError,
                                    retryHandler = { intent(CashflowLedgerIntent.Refresh) },
                                    lastData = entries.lastData
                                )
                            )
                        }

                        if (hadData) {
                            action(CashflowLedgerAction.ShowError(dokusError))
                        }
                    }
                }
            } else {
                logger.d { "Loading more entries, page=$page" }

                loadJob = launch {
                    loadCashflowEntries(
                        page = page,
                        pageSize = PAGE_SIZE,
                        viewMode = domainViewMode,
                        fromDate = fromDate,
                        toDate = toDate,
                        direction = mappedDirection,
                        statuses = apiStatuses,
                        sourceType = null
                    ).fold(
                        onSuccess = { response ->
                            val previousData = entries.lastData?.data ?: emptyList()
                            updateState {
                                copy(
                                    entries = DokusState.success(
                                        PaginationState(
                                            data = previousData + response.items,
                                            currentPage = page,
                                            pageSize = PAGE_SIZE,
                                            hasMorePages = response.hasMore
                                        )
                                    )
                                )
                            }
                        },
                        onFailure = { error ->
                            logger.e(error) { "Failed to load more entries" }
                            updateState {
                                copy(
                                    entries = DokusState.error(
                                        exception = error.asDokusException,
                                        retryHandler = { intent(CashflowLedgerIntent.Refresh) },
                                        lastData = entries.lastData
                                    )
                                )
                            }
                            action(CashflowLedgerAction.ShowError(error.asDokusException))
                        }
                    )
                }
            }
        }
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
     */
    private fun getStatusesForViewMode(viewMode: CashflowViewMode): List<CashflowEntryStatus> {
        return when (viewMode) {
            CashflowViewMode.Upcoming -> listOf(CashflowEntryStatus.Open, CashflowEntryStatus.Overdue)
            CashflowViewMode.Overdue -> listOf(CashflowEntryStatus.Open, CashflowEntryStatus.Overdue)
            CashflowViewMode.History -> listOf(CashflowEntryStatus.Paid)
        }
    }

    private fun mapDirectionFilter(filter: DirectionFilter): CashflowDirection? {
        return when (filter) {
            DirectionFilter.All -> null
            DirectionFilter.In -> CashflowDirection.In
            DirectionFilter.Out -> CashflowDirection.Out
        }
    }

    private fun mapViewModeToDomain(viewMode: CashflowViewMode): DomainViewMode {
        return when (viewMode) {
            CashflowViewMode.Upcoming -> DomainViewMode.Upcoming
            CashflowViewMode.Overdue -> DomainViewMode.Overdue
            CashflowViewMode.History -> DomainViewMode.History
        }
    }
}

/**
 * Returns mock balance state when SHOW_BALANCE_MOCK is enabled (local dev).
 * Returns null in production (no banking integration yet).
 */
private fun getMockBalanceIfEnabled(): BalanceState? {
    return if (BuildKonfig.SHOW_BALANCE_MOCK) {
        BalanceState(
            amount = tech.dokus.domain.Money(1248234),
            asOf = LocalDate(2026, 1, 15),
            accountName = "KBC"
        )
    } else {
        null
    }
}
