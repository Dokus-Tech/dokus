package tech.dokus.features.cashflow.presentation.ledger.mvi

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.foundation.app.state.DokusState

/**
 * Date range filter options for cashflow ledger.
 */
@Immutable
enum class DateRangeFilter {
    ThisMonth,
    Next3Months,
    AllTime
}

/**
 * Filter state for cashflow ledger.
 */
@Immutable
data class CashflowFilters(
    val dateRange: DateRangeFilter = DateRangeFilter.ThisMonth,
    val direction: CashflowDirection? = null,
    val status: CashflowEntryStatus? = null,
    val sourceType: CashflowSourceType? = null
)

/**
 * State for CashflowLedgerScreen.
 */
@Immutable
sealed interface CashflowLedgerState : MVIState, DokusState<Nothing> {

    data object Loading : CashflowLedgerState

    data class Content(
        val entries: PaginationState<CashflowEntry>,
        val filters: CashflowFilters = CashflowFilters(),
        val highlightedEntryId: CashflowEntryId? = null,
        val isRefreshing: Boolean = false
    ) : CashflowLedgerState {
        val totalIn: Long
            get() = entries.data.filter { it.direction == CashflowDirection.In }
                .sumOf { it.amountGross.minor }

        val totalOut: Long
            get() = entries.data.filter { it.direction == CashflowDirection.Out }
                .sumOf { it.amountGross.minor }
    }

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
    data class UpdateFilters(val filters: CashflowFilters) : CashflowLedgerIntent
    data class UpdateDateRangeFilter(val dateRange: DateRangeFilter) : CashflowLedgerIntent
    data class UpdateDirectionFilter(val direction: CashflowDirection?) : CashflowLedgerIntent
    data class UpdateStatusFilter(val status: CashflowEntryStatus?) : CashflowLedgerIntent
    data class HighlightEntry(val entryId: CashflowEntryId?) : CashflowLedgerIntent
    data class OpenEntry(val entry: CashflowEntry) : CashflowLedgerIntent
}

/**
 * Actions for CashflowLedgerScreen.
 */
@Immutable
sealed interface CashflowLedgerAction : MVIAction {
    data class NavigateToDocumentReview(val documentId: String) : CashflowLedgerAction
    data class NavigateToEntity(val sourceType: CashflowSourceType, val sourceId: String) : CashflowLedgerAction
    data class ShowError(val error: DokusException) : CashflowLedgerAction
}
