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
                    is CashflowLedgerIntent.UpdateFilters -> handleUpdateFilters(intent.filters)
                    is CashflowLedgerIntent.UpdateDateRangeFilter -> handleUpdateDateRange(intent.dateRange)
                    is CashflowLedgerIntent.UpdateDirectionFilter -> handleUpdateDirection(intent.direction)
                    is CashflowLedgerIntent.UpdateStatusFilter -> handleUpdateStatus(intent.status)
                    is CashflowLedgerIntent.HighlightEntry -> handleHighlightEntry(intent.entryId)
                    is CashflowLedgerIntent.OpenEntry -> handleOpenEntry(intent.entry)
                    // Detail pane intents
                    is CashflowLedgerIntent.SelectEntry -> handleSelectEntry(intent.entryId)
                    is CashflowLedgerIntent.CloseDetailPane -> handleCloseDetailPane()
                    is CashflowLedgerIntent.UpdatePaymentDate -> handleUpdatePaymentDate(intent.date)
                    is CashflowLedgerIntent.UpdatePaymentAmount -> handleUpdatePaymentAmount(intent.amount)
                    is CashflowLedgerIntent.UpdatePaymentNote -> handleUpdatePaymentNote(intent.note)
                    is CashflowLedgerIntent.SubmitPayment -> handleSubmitPayment()
                    is CashflowLedgerIntent.OpenDocument -> handleOpenDocument(intent.documentId)
                }
            }
        }

    private suspend fun CashflowLedgerCtx.handleRefresh() {
        loadJob?.cancel()
        logger.d { "Refreshing cashflow entries" }

        loadedEntries = emptyList()
        paginationInfo = LocalPaginationInfo()

        updateState { CashflowLedgerState.Loading }

        val (fromDate, toDate) = getDateRange(currentFilters.dateRange)

        loadCashflowEntries(
            page = 0,
            pageSize = PAGE_SIZE,
            fromDate = fromDate,
            toDate = toDate,
            direction = currentFilters.direction,
            status = currentFilters.status,
            sourceType = currentFilters.sourceType
        ).fold(
            onSuccess = { response ->
                loadedEntries = response.items
                paginationInfo = paginationInfo.copy(
                    currentPage = 0,
                    isLoadingMore = false,
                    hasMorePages = response.hasMore
                )
                updateState {
                    CashflowLedgerState.Content(
                        entries = buildPaginationState(),
                        filters = currentFilters,
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

            val (fromDate, toDate) = getDateRange(currentFilters.dateRange)

            loadJob = launch {
                loadCashflowEntries(
                    page = nextPage,
                    pageSize = PAGE_SIZE,
                    fromDate = fromDate,
                    toDate = toDate,
                    direction = currentFilters.direction,
                    status = currentFilters.status,
                    sourceType = currentFilters.sourceType
                ).fold(
                    onSuccess = { response ->
                        loadedEntries = loadedEntries + response.items
                        paginationInfo = paginationInfo.copy(
                            currentPage = nextPage,
                            isLoadingMore = false,
                            hasMorePages = response.hasMore
                        )
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

    private suspend fun CashflowLedgerCtx.handleUpdateFilters(filters: CashflowFilters) {
        currentFilters = filters
        handleRefresh()
    }

    private suspend fun CashflowLedgerCtx.handleUpdateDateRange(dateRange: DateRangeFilter) {
        currentFilters = currentFilters.copy(dateRange = dateRange)
        handleRefresh()
    }

    private suspend fun CashflowLedgerCtx.handleUpdateDirection(direction: CashflowDirection?) {
        currentFilters = currentFilters.copy(direction = direction)
        handleRefresh()
    }

    private suspend fun CashflowLedgerCtx.handleUpdateStatus(status: CashflowEntryStatus?) {
        currentFilters = currentFilters.copy(status = status)
        handleRefresh()
    }

    private suspend fun CashflowLedgerCtx.handleHighlightEntry(entryId: CashflowEntryId?) {
        withState<CashflowLedgerState.Content, _> {
            updateState { copy(highlightedEntryId = entryId) }
        }
    }

    private suspend fun CashflowLedgerCtx.handleOpenEntry(entry: CashflowEntry) {
        // Open detail pane instead of navigating
        handleSelectEntry(entry.id)
    }

    private suspend fun CashflowLedgerCtx.handleSelectEntry(entryId: CashflowEntryId) {
        withState<CashflowLedgerState.Content, _> {
            val entry = entries.data.find { it.id == entryId } ?: return@withState
            updateState {
                copy(
                    selectedEntryId = entryId,
                    paymentFormState = PaymentFormState(amount = entry.remainingAmount)
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

    private suspend fun CashflowLedgerCtx.handleUpdatePaymentAmount(amount: Money) {
        withState<CashflowLedgerState.Content, _> {
            updateState {
                copy(paymentFormState = paymentFormState.copy(amount = amount, amountError = null))
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

            // Validate: amount > 0
            if (form.amount.minor <= 0) {
                updateState { copy(paymentFormState = form.copy(amountError = "Amount must be positive")) }
                return@withState
            }

            // Validate: amount <= remainingAmount
            if (form.amount > entry.remainingAmount) {
                updateState { copy(paymentFormState = form.copy(amountError = "Amount exceeds remaining")) }
                return@withState
            }

            updateState { copy(paymentFormState = form.copy(isSubmitting = true)) }

            recordPayment(
                entryId = entry.id,
                request = CashflowPaymentRequest(
                    amount = form.amount,
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
                                PaymentFormState(amount = updatedEntry.remainingAmount)
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

    private fun buildPaginationState(): PaginationState<CashflowEntry> {
        return PaginationState(
            data = loadedEntries,
            currentPage = paginationInfo.currentPage,
            pageSize = PAGE_SIZE,
            isLoadingMore = paginationInfo.isLoadingMore,
            hasMorePages = paginationInfo.hasMorePages
        )
    }

    private fun getDateRange(filter: DateRangeFilter): Pair<kotlinx.datetime.LocalDate?, kotlinx.datetime.LocalDate?> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return when (filter) {
            DateRangeFilter.ThisMonth -> {
                val startOfMonth = kotlinx.datetime.LocalDate(today.year, today.month, 1)
                val endOfMonth = startOfMonth.plus(1, DateTimeUnit.MONTH).plus(-1, DateTimeUnit.DAY)
                startOfMonth to endOfMonth
            }
            DateRangeFilter.Next3Months -> {
                today to today.plus(3, DateTimeUnit.MONTH)
            }
            DateRangeFilter.AllTime -> null to null
        }
    }

    private data class LocalPaginationInfo(
        val currentPage: Int = 0,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = true
    )
}
