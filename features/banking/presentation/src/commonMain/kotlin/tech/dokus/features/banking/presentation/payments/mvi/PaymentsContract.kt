package tech.dokus.features.banking.presentation.payments.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.BankTransactionSummary
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.foundation.app.state.DokusState

/**
 * Filter tab for the payments screen.
 */
@Immutable
enum class PaymentFilterTab {
    All,
    NeedsReview,
    Unmatched,
    Matched,
    Ignored,
}

/**
 * State for PaymentsScreen.
 */
@Immutable
data class PaymentsState(
    val transactions: DokusState<PaginationState<BankTransactionDto>>,
    val summary: DokusState<BankTransactionSummary>,
    val filterTab: PaymentFilterTab = PaymentFilterTab.All,
    val selectedTransactionId: BankTransactionId? = null,
) : MVIState {
    companion object {
        val initial by lazy {
            PaymentsState(
                transactions = DokusState.loading(),
                summary = DokusState.loading(),
            )
        }
    }
}

/**
 * Intents for PaymentsScreen.
 */
@Immutable
sealed interface PaymentsIntent : MVIIntent {
    data object Refresh : PaymentsIntent
    data object LoadMore : PaymentsIntent
    data class SetFilterTab(val tab: PaymentFilterTab) : PaymentsIntent
    data class SelectTransaction(val transactionId: BankTransactionId?) : PaymentsIntent
    data class LinkDocument(val transactionId: BankTransactionId) : PaymentsIntent
    data class IgnoreTransaction(val transactionId: BankTransactionId) : PaymentsIntent
    data class ConfirmMatch(val transactionId: BankTransactionId) : PaymentsIntent
}

/**
 * Actions for PaymentsScreen.
 */
@Immutable
sealed interface PaymentsAction : MVIAction {
    data class ShowError(val error: DokusException) : PaymentsAction
}
