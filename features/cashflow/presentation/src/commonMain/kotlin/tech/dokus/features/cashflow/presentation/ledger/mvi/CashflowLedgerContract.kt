package tech.dokus.features.cashflow.presentation.ledger.mvi

import androidx.compose.runtime.Immutable
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.Money
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.foundation.app.state.DokusState

/**
 * View mode for cashflow ledger.
 * - Upcoming: Money expected in next 30 days (Open + Overdue entries)
 * - Overdue: Unpaid entries with due date in the past (Open + Overdue entries)
 * - History: Money already moved (Paid entries with paidAt)
 */
@Immutable
enum class CashflowViewMode {
    Upcoming, // Open + Overdue in next 30 days
    Overdue, // Open + Overdue with due date < today
    History // Paid (money already moved, requires paidAt != null)
}

/**
 * Direction filter for cashflow entries.
 */
@Immutable
enum class DirectionFilter {
    All, // Show both in and out
    In, // Only incoming (invoices, refunds)
    Out // Only outgoing (inbound invoices, expenses)
}

/**
 * Filter state for cashflow ledger.
 * Simplified from old complex filters to view mode + direction.
 */
@Immutable
data class CashflowFilters(
    val viewMode: CashflowViewMode = CashflowViewMode.Upcoming,
    val direction: DirectionFilter = DirectionFilter.All
)

/**
 * Bank balance state for the cashflow header.
 * Nullable when no banking integration is available.
 */
@Immutable
data class BalanceState(
    val amount: Money,
    val asOf: LocalDate,
    val accountName: String
)

/**
 * Summary data for the cashflow view.
 * Computed from the same filters as the entry list (NON-NEGOTIABLE: must match).
 */
@Immutable
data class CashflowSummary(
    val periodLabel: String, // "NEXT 30 DAYS" or "LAST 30 DAYS"
    val netAmount: Money, // The answer: totalIn - totalOut
    val totalIn: Money, // Sum of IN entries
    val totalOut: Money // Sum of OUT entries
) {
    companion object {
        val EMPTY = CashflowSummary(
            periodLabel = "",
            netAmount = Money.ZERO,
            totalIn = Money.ZERO,
            totalOut = Money.ZERO
        )
    }
}

/**
 * Payment form state for recording payments against cashflow entries.
 *
 * Uses amountText + amount pattern: TextField always updates amountText,
 * parsing updates amount when valid. Validate only on submit.
 */
@Immutable
data class PaymentFormState(
    val paidAt: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val amountText: String = "",
    val amount: Money? = null,
    val note: String = "",
    val isSubmitting: Boolean = false,
    val amountError: String? = null,
    val isOptionsExpanded: Boolean = false
) {
    companion object {
        fun withAmount(amount: Money): PaymentFormState = PaymentFormState(
            amountText = amount.toDisplayString(),
            amount = amount
        )
    }
}

/**
 * State for CashflowLedgerScreen.
 */
@Immutable
sealed interface CashflowLedgerState : MVIState, DokusState<Nothing> {

    data object Loading : CashflowLedgerState

    data class Content(
        val entries: PaginationState<CashflowEntry>,
        val filters: CashflowFilters = CashflowFilters(),
        val summary: CashflowSummary = CashflowSummary.EMPTY,
        val balance: BalanceState? = null, // null when no banking integration
        val highlightedEntryId: CashflowEntryId? = null,
        val isRefreshing: Boolean = false,
        val selectedEntryId: CashflowEntryId? = null,
        val paymentFormState: PaymentFormState = PaymentFormState(),
        val actionsEntryId: CashflowEntryId? = null // Which row's action menu is open
    ) : CashflowLedgerState

    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler
    ) : CashflowLedgerState, DokusState.Error<Nothing>
}

/**
 * Intents for CashflowLedgerScreen.
 */
@Immutable
sealed interface CashflowLedgerIntent : MVIIntent {
    data object Refresh : CashflowLedgerIntent
    data object LoadMore : CashflowLedgerIntent

    // View mode and direction filter intents
    data class SetViewMode(val mode: CashflowViewMode) : CashflowLedgerIntent
    data class SetDirectionFilter(val direction: DirectionFilter) : CashflowLedgerIntent

    data class HighlightEntry(val entryId: CashflowEntryId?) : CashflowLedgerIntent
    data class OpenEntry(val entry: CashflowEntry) : CashflowLedgerIntent

    // Detail pane intents
    data class SelectEntry(val entryId: CashflowEntryId) : CashflowLedgerIntent
    data object CloseDetailPane : CashflowLedgerIntent
    data class UpdatePaymentDate(val date: LocalDate) : CashflowLedgerIntent
    data class UpdatePaymentAmountText(val text: String) : CashflowLedgerIntent
    data class UpdatePaymentNote(val note: String) : CashflowLedgerIntent
    data object SubmitPayment : CashflowLedgerIntent
    data class OpenDocument(val documentId: DocumentId) : CashflowLedgerIntent

    // Payment options intents
    data object TogglePaymentOptions : CashflowLedgerIntent
    data object QuickMarkAsPaid : CashflowLedgerIntent
    data object CancelPaymentOptions : CashflowLedgerIntent

    // Row actions menu intents
    data class ShowRowActions(val entryId: CashflowEntryId) : CashflowLedgerIntent
    data object HideRowActions : CashflowLedgerIntent

    // Actions from the row actions menu
    data class RecordPaymentFor(val entryId: CashflowEntryId) : CashflowLedgerIntent
    data class MarkAsPaidQuick(val entryId: CashflowEntryId) : CashflowLedgerIntent
    data class ViewDocumentFor(val entry: CashflowEntry) : CashflowLedgerIntent
}

/**
 * Actions for CashflowLedgerScreen.
 */
@Immutable
sealed interface CashflowLedgerAction : MVIAction {
    data class NavigateToDocumentReview(val documentId: String) : CashflowLedgerAction
    data class NavigateToEntity(val sourceType: CashflowSourceType, val sourceId: String) : CashflowLedgerAction
    data class ShowError(val error: DokusException) : CashflowLedgerAction
    data class ShowPaymentSuccess(val entry: CashflowEntry) : CashflowLedgerAction
    data class ShowPaymentError(val error: DokusException) : CashflowLedgerAction
}
