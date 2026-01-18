package tech.dokus.features.cashflow.presentation.ledger.mvi

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
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
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.CashflowPaymentRequest
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.domain.config.BuildKonfig
import tech.dokus.features.cashflow.usecases.LoadCashflowEntriesUseCase
import tech.dokus.features.cashflow.usecases.RecordCashflowPaymentUseCase
import tech.dokus.foundation.platform.Logger

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
    private val recordPayment: RecordCashflowPaymentUseCase,
    private val highlightEntryId: CashflowEntryId? = null
) : Container<CashflowLedgerState, CashflowLedgerIntent, CashflowLedgerAction> {

    private val logger = Logger.forClass<CashflowLedgerContainer>()
    private var loadedEntries: List<CashflowEntry> = emptyList()
    private var paginationInfo = LocalPaginationInfo()
    private var currentFilters = CashflowFilters()
    private var loadJob: Job? = null

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
                    // Detail pane intents
                    is CashflowLedgerIntent.SelectEntry -> handleSelectEntry(intent.entryId)
                    is CashflowLedgerIntent.CloseDetailPane -> handleCloseDetailPane()
                    is CashflowLedgerIntent.UpdatePaymentDate -> handleUpdatePaymentDate(intent.date)
                    is CashflowLedgerIntent.UpdatePaymentAmountText -> handleUpdatePaymentAmountText(intent.text)
                    is CashflowLedgerIntent.UpdatePaymentNote -> handleUpdatePaymentNote(intent.note)
                    is CashflowLedgerIntent.SubmitPayment -> handleSubmitPayment()
                    is CashflowLedgerIntent.OpenDocument -> handleOpenDocument(intent.documentId)
                    // Payment options intents
                    is CashflowLedgerIntent.TogglePaymentOptions -> handleTogglePaymentOptions()
                    is CashflowLedgerIntent.QuickMarkAsPaid -> handleQuickMarkAsPaid()
                    is CashflowLedgerIntent.CancelPaymentOptions -> handleCancelPaymentOptions()
                    // Row actions menu intents
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
        logger.d { "Refreshing cashflow entries with viewMode=${currentFilters.viewMode}, direction=${currentFilters.direction}" }

        loadedEntries = emptyList()
        paginationInfo = LocalPaginationInfo()

        updateState { CashflowLedgerState.Loading }

        val (fromDate, toDate) = getDateRangeForViewMode(currentFilters.viewMode)
        val statuses = getStatusesForViewMode(currentFilters.viewMode)
        val direction = mapDirectionFilter(currentFilters.direction)

        // For Upcoming view, we need both Open and Overdue statuses.
        // API only supports single status, so:
        // - Upcoming: pass null (get all), filter client-side for Open/Overdue
        // - History: pass Paid (single status is fine)
        val apiStatus = when (currentFilters.viewMode) {
            CashflowViewMode.Upcoming -> null // Get all, filter client-side
            CashflowViewMode.History -> CashflowEntryStatus.Paid
        }

        loadCashflowEntries(
            page = 0,
            pageSize = PAGE_SIZE,
            fromDate = fromDate,
            toDate = toDate,
            direction = direction,
            status = apiStatus,
            sourceType = null
        ).fold(
            onSuccess = { response ->
                // Filter and sort entries based on view mode
                val filteredEntries = filterEntriesForViewMode(response.items, currentFilters.viewMode)
                val sortedEntries = sortEntriesForViewMode(filteredEntries, currentFilters.viewMode)

                loadedEntries = sortedEntries
                paginationInfo = paginationInfo.copy(
                    currentPage = 0,
                    isLoadingMore = false,
                    hasMorePages = response.hasMore
                )

                // Compute summary from filtered entries (NON-NEGOTIABLE: must match list)
                val summary = computeSummary(sortedEntries, currentFilters.viewMode, currentFilters.direction)

                updateState {
                    CashflowLedgerState.Content(
                        entries = buildPaginationState(),
                        filters = currentFilters,
                        summary = summary,
                        balance = getMockBalanceIfEnabled(),
                        highlightedEntryId = highlightEntryId
                    )
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load cashflow entries" }
                updateState {
                    CashflowLedgerState.Error(
                        exception = error.asDokusException,
                        retryHandler = { intent(CashflowLedgerIntent.Refresh) }
                    )
                }
            }
        )
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

            // Same logic as handleRefresh: null for Upcoming, Paid for History
            val apiStatus = when (currentFilters.viewMode) {
                CashflowViewMode.Upcoming -> null
                CashflowViewMode.History -> CashflowEntryStatus.Paid
            }

            loadJob = launch {
                loadCashflowEntries(
                    page = nextPage,
                    pageSize = PAGE_SIZE,
                    fromDate = fromDate,
                    toDate = toDate,
                    direction = direction,
                    status = apiStatus,
                    sourceType = null
                ).fold(
                    onSuccess = { response ->
                        val filteredEntries = filterEntriesForViewMode(response.items, currentFilters.viewMode)
                        val sortedNew = sortEntriesForViewMode(filteredEntries, currentFilters.viewMode)

                        loadedEntries = loadedEntries + sortedNew
                        paginationInfo = paginationInfo.copy(
                            currentPage = nextPage,
                            isLoadingMore = false,
                            hasMorePages = response.hasMore
                        )

                        // Recompute summary with all entries
                        val summary = computeSummary(loadedEntries, currentFilters.viewMode, currentFilters.direction)
                        updateState { copy(entries = buildPaginationState(), summary = summary) }
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
        currentFilters = currentFilters.copy(viewMode = mode)
        handleRefresh()
    }

    private suspend fun CashflowLedgerCtx.handleSetDirectionFilter(direction: DirectionFilter) {
        currentFilters = currentFilters.copy(direction = direction)
        handleRefresh()
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
            action(CashflowLedgerAction.NavigateToEntity(entry.sourceType, entry.sourceId.toString()))
        }
    }

    private suspend fun CashflowLedgerCtx.handleSelectEntry(entryId: CashflowEntryId) {
        withState<CashflowLedgerState.Content, _> {
            val entry = entries.data.find { it.id == entryId } ?: return@withState
            updateState {
                copy(
                    selectedEntryId = entryId,
                    paymentFormState = PaymentFormState.withAmount(entry.remainingAmount)
                )
            }
        }
    }

    private suspend fun CashflowLedgerCtx.handleCloseDetailPane() {
        withState<CashflowLedgerState.Content, _> {
            updateState {
                copy(
                    selectedEntryId = null,
                    paymentFormState = PaymentFormState()
                )
            }
        }
    }

    private suspend fun CashflowLedgerCtx.handleUpdatePaymentDate(date: LocalDate) {
        withState<CashflowLedgerState.Content, _> {
            updateState {
                copy(paymentFormState = paymentFormState.copy(paidAt = date))
            }
        }
    }

    private suspend fun CashflowLedgerCtx.handleUpdatePaymentAmountText(text: String) {
        withState<CashflowLedgerState.Content, _> {
            val parsed = Money.parse(text)
            updateState {
                copy(paymentFormState = paymentFormState.copy(
                    amountText = text,
                    amount = parsed,
                    amountError = null
                ))
            }
        }
    }

    private suspend fun CashflowLedgerCtx.handleUpdatePaymentNote(note: String) {
        withState<CashflowLedgerState.Content, _> {
            updateState {
                copy(paymentFormState = paymentFormState.copy(note = note))
            }
        }
    }

    private suspend fun CashflowLedgerCtx.handleSubmitPayment() {
        withState<CashflowLedgerState.Content, _> {
            val entry = entries.data.find { it.id == selectedEntryId } ?: return@withState
            val form = paymentFormState

            // Validate: amount must be valid
            val amount = form.amount
            if (amount == null || amount.minor <= 0) {
                updateState { copy(paymentFormState = form.copy(amountError = "Amount must be positive")) }
                return@withState
            }

            // Validate: amount <= remainingAmount
            if (amount > entry.remainingAmount) {
                updateState { copy(paymentFormState = form.copy(amountError = "Amount exceeds remaining")) }
                return@withState
            }

            updateState { copy(paymentFormState = form.copy(isSubmitting = true)) }

            recordPayment(
                entryId = entry.id,
                request = CashflowPaymentRequest(
                    amount = amount,
                    paidAt = form.paidAt.atTime(0, 0),
                    note = form.note.ifBlank { null }
                )
            ).fold(
                onSuccess = { updatedEntry ->
                    // Update entry in-place (preserves scroll, filters, pagination)
                    val updatedList = entries.data.map {
                        if (it.id == updatedEntry.id) updatedEntry else it
                    }
                    loadedEntries = loadedEntries.map {
                        if (it.id == updatedEntry.id) updatedEntry else it
                    }

                    // If fully paid (remaining == 0), close pane. Otherwise keep open with new remaining.
                    val fullyPaid = updatedEntry.remainingAmount.isZero
                    updateState {
                        copy(
                            entries = entries.copy(data = updatedList),
                            selectedEntryId = if (fullyPaid) null else selectedEntryId,
                            paymentFormState = if (fullyPaid) {
                                PaymentFormState()
                            } else {
                                PaymentFormState.withAmount(updatedEntry.remainingAmount)
                            }
                        )
                    }
                    action(CashflowLedgerAction.ShowPaymentSuccess(updatedEntry))
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to record payment" }
                    updateState { copy(paymentFormState = form.copy(isSubmitting = false)) }
                    action(CashflowLedgerAction.ShowPaymentError(error.asDokusException))
                }
            )
        }
    }

    private suspend fun CashflowLedgerCtx.handleOpenDocument(documentId: DocumentId) {
        action(CashflowLedgerAction.NavigateToDocumentReview(documentId.toString()))
    }

    private suspend fun CashflowLedgerCtx.handleTogglePaymentOptions() {
        withState<CashflowLedgerState.Content, _> {
            updateState {
                copy(paymentFormState = paymentFormState.copy(
                    isOptionsExpanded = !paymentFormState.isOptionsExpanded,
                    amountError = null
                ))
            }
        }
    }

    private suspend fun CashflowLedgerCtx.handleQuickMarkAsPaid() {
        withState<CashflowLedgerState.Content, _> {
            val entry = entries.data.find { it.id == selectedEntryId } ?: return@withState
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

            // Set defaults
            updateState {
                copy(paymentFormState = PaymentFormState.withAmount(entry.remainingAmount).copy(
                    paidAt = today,
                    note = ""
                ))
            }
        }
        // Dispatch SubmitPayment intent (same validation path)
        intent(CashflowLedgerIntent.SubmitPayment)
    }

    private suspend fun CashflowLedgerCtx.handleCancelPaymentOptions() {
        withState<CashflowLedgerState.Content, _> {
            val entry = entries.data.find { it.id == selectedEntryId } ?: return@withState
            updateState {
                copy(paymentFormState = PaymentFormState.withAmount(entry.remainingAmount).copy(
                    isOptionsExpanded = false
                ))
            }
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
        // Close actions menu and open payment pane for this entry
        withState<CashflowLedgerState.Content, _> {
            val entry = entries.data.find { it.id == entryId } ?: return@withState
            updateState {
                copy(
                    actionsEntryId = null,
                    selectedEntryId = entryId,
                    paymentFormState = PaymentFormState.withAmount(entry.remainingAmount)
                )
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
            action(CashflowLedgerAction.NavigateToEntity(entry.sourceType, entry.sourceId.toString()))
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
     * - History: [today-30d, today] (uses paidAt for filtering)
     */
    private fun getDateRangeForViewMode(viewMode: CashflowViewMode): Pair<LocalDate, LocalDate> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return when (viewMode) {
            CashflowViewMode.Upcoming -> today to today.plus(30, DateTimeUnit.DAY)
            CashflowViewMode.History -> today.plus(-30, DateTimeUnit.DAY) to today
        }
    }

    /**
     * Get status filters based on view mode.
     * - Upcoming: Open, Overdue (money not yet moved)
     * - History: Paid (money already moved)
     */
    private fun getStatusesForViewMode(viewMode: CashflowViewMode): List<CashflowEntryStatus> {
        return when (viewMode) {
            CashflowViewMode.Upcoming -> listOf(CashflowEntryStatus.Open, CashflowEntryStatus.Overdue)
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
     * Filter entries based on view mode requirements.
     * - Cancelled entries are excluded from both views
     */
    private fun filterEntriesForViewMode(
        entries: List<CashflowEntry>,
        viewMode: CashflowViewMode
    ): List<CashflowEntry> {
        return entries.filter { entry ->
            // Exclude cancelled entries (filtered server-side but double-check)
            if (entry.status == CashflowEntryStatus.Cancelled) return@filter false

            when (viewMode) {
                CashflowViewMode.Upcoming -> {
                    entry.status in listOf(CashflowEntryStatus.Open, CashflowEntryStatus.Overdue)
                }
                CashflowViewMode.History -> {
                    // History = Paid entries (money already moved)
                    entry.status == CashflowEntryStatus.Paid
                }
            }
        }
    }

    /**
     * Sort entries based on view mode.
     * - Upcoming: by eventDate ASC (soonest first)
     * - History: by paidAt DESC (most recent first)
     */
    private fun sortEntriesForViewMode(
        entries: List<CashflowEntry>,
        viewMode: CashflowViewMode
    ): List<CashflowEntry> {
        return when (viewMode) {
            CashflowViewMode.Upcoming -> entries.sortedBy { it.computeUpcomingSortDate() }
            CashflowViewMode.History -> entries.sortedByDescending { it.computeHistorySortDate() }
        }
    }

    /**
     * Compute summary from entries.
     * NON-NEGOTIABLE: Summary must match the filtered list exactly.
     *
     * When direction filter is active, only show that direction's total.
     * This ensures summary matches what's visible in the list.
     */
    private fun computeSummary(
        entries: List<CashflowEntry>,
        viewMode: CashflowViewMode,
        directionFilter: DirectionFilter
    ): CashflowSummary {
        val periodLabel = when (viewMode) {
            CashflowViewMode.Upcoming -> "NEXT 30 DAYS"
            CashflowViewMode.History -> "LAST 30 DAYS"
        }

        // Compute totals based on direction filter
        val totalIn = when (directionFilter) {
            DirectionFilter.Out -> Money.ZERO // Don't show In when filtering Out
            else -> entries
                .filter { it.direction == CashflowDirection.In }
                .fold(Money.ZERO) { acc, entry -> acc + entry.amountGross }
        }

        val totalOut = when (directionFilter) {
            DirectionFilter.In -> Money.ZERO // Don't show Out when filtering In
            else -> entries
                .filter { it.direction == CashflowDirection.Out }
                .fold(Money.ZERO) { acc, entry -> acc + entry.amountGross }
        }

        val netAmount = totalIn - totalOut

        return CashflowSummary(
            periodLabel = periodLabel,
            netAmount = netAmount,
            totalIn = totalIn,
            totalOut = totalOut
        )
    }

    private data class LocalPaginationInfo(
        val currentPage: Int = 0,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = true
    )
}

// Extension functions for sorting (separate for each mode - semantics differ)

/**
 * Upcoming: sort by expected cash date.
 * Uses eventDate which represents the expected/due date.
 */
private fun CashflowEntry.computeUpcomingSortDate(): LocalDate = eventDate

/**
 * History: sort by when payment occurred.
 * Uses updatedAt as proxy for payment date (entry is updated when paid).
 * TODO: Add explicit paidAt field to CashflowEntry for accurate sorting.
 */
private fun CashflowEntry.computeHistorySortDate(): LocalDate = updatedAt.date

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
