package tech.dokus.features.cashflow.presentation.overview.mvi

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.CashflowEntryDto
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
            netAmount = Money.zero(Currency.Eur),
            totalIn = Money.zero(Currency.Eur),
            totalOut = Money.zero(Currency.Eur)
        )
    }
}

/**
 * State for CashFlowOverviewScreen.
 */
@Immutable
data class CashFlowOverviewState(
    val entries: DokusState<PaginationState<CashflowEntryDto>>,
    val filters: CashflowFilters = CashflowFilters(),
    val summary: CashflowSummary = CashflowSummary.EMPTY,
    val balance: BalanceState? = null,
    val highlightedEntryId: CashflowEntryId? = null,
    val actionsEntryId: CashflowEntryId? = null,
    val actionError: DokusException? = null,
) : MVIState {
    companion object {
        val initial by lazy {
            CashFlowOverviewState(entries = DokusState.loading())
        }
    }
}

/**
 * Intents for CashFlowOverviewScreen.
 */
@Immutable
sealed interface CashFlowOverviewIntent : MVIIntent {
    data object Refresh : CashFlowOverviewIntent
    data object LoadMore : CashFlowOverviewIntent

    // View mode and direction filter intents
    data class SetViewMode(val mode: CashflowViewMode) : CashFlowOverviewIntent
    data class SetDirectionFilter(val direction: DirectionFilter) : CashFlowOverviewIntent

    data class HighlightEntry(val entryId: CashflowEntryId?) : CashFlowOverviewIntent
    data class OpenEntry(val entry: CashflowEntryDto) : CashFlowOverviewIntent

    // Row actions menu intents
    data class ShowRowActions(val entryId: CashflowEntryId) : CashFlowOverviewIntent
    data object HideRowActions : CashFlowOverviewIntent

    // Actions from the row actions menu
    data class RecordPaymentFor(val entryId: CashflowEntryId) : CashFlowOverviewIntent
    data class MarkAsPaidQuick(val entryId: CashflowEntryId) : CashFlowOverviewIntent
    data class ViewDocumentFor(val entry: CashflowEntryDto) : CashFlowOverviewIntent

    data object DismissActionError : CashFlowOverviewIntent
}

/**
 * Actions for CashFlowOverviewScreen.
 */
@Immutable
sealed interface CashFlowOverviewAction : MVIAction {
    data class NavigateToDocumentDetail(val documentId: DocumentId) : CashFlowOverviewAction
    data class NavigateToEntity(val sourceType: CashflowSourceType, val sourceId: String) : CashFlowOverviewAction
}
