package tech.dokus.features.banking.presentation.payments.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.IgnoredReason
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.model.BankAccountDto
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
 * Dialog state for ignore-with-reason flow.
 */
@Immutable
data class IgnoreDialogState(
    val transactionId: BankTransactionId,
    val selectedReason: IgnoredReason? = null,
)

/**
 * Dialog state for mark-as-transfer flow.
 */
@Immutable
data class TransferDialogState(
    val transactionId: BankTransactionId,
    val sourceAccountId: BankAccountId?,
    val availableAccounts: List<BankAccountDto> = emptyList(),
    val selectedDestinationAccountId: BankAccountId? = null,
    val isSubmitting: Boolean = false,
)

/**
 * State for PaymentsScreen.
 */
@Immutable
data class PaymentsState(
    val transactions: DokusState<PaginationState<BankTransactionDto>>,
    val summary: DokusState<BankTransactionSummary>,
    val accountNames: Map<BankAccountId, String> = emptyMap(),
    val filterTab: PaymentFilterTab = PaymentFilterTab.All,
    val selectedAccountId: BankAccountId? = null,
    val selectedTransactionId: BankTransactionId? = null,
    val ignoreDialogState: IgnoreDialogState? = null,
    val transferDialogState: TransferDialogState? = null,
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
    data class SetAccountFilter(val accountId: BankAccountId?) : PaymentsIntent
    data class SelectTransaction(val transactionId: BankTransactionId?) : PaymentsIntent
    data class LinkDocument(val transactionId: BankTransactionId) : PaymentsIntent
    data class IgnoreTransaction(val transactionId: BankTransactionId) : PaymentsIntent
    data class SelectIgnoreReason(val reason: IgnoredReason) : PaymentsIntent
    data object ConfirmIgnore : PaymentsIntent
    data object DismissIgnoreDialog : PaymentsIntent
    data class ConfirmMatch(val transactionId: BankTransactionId) : PaymentsIntent
    data class CreateExpense(val transactionId: BankTransactionId) : PaymentsIntent
    data class MarkTransfer(val transactionId: BankTransactionId) : PaymentsIntent
    data class SelectTransferDestination(val accountId: BankAccountId) : PaymentsIntent
    data object ConfirmTransfer : PaymentsIntent
    data object DismissTransferDialog : PaymentsIntent
    data class UndoTransfer(val transactionId: BankTransactionId) : PaymentsIntent
}

/**
 * Actions for PaymentsScreen.
 */
@Immutable
sealed interface PaymentsAction : MVIAction {
    data class ShowError(val error: DokusException) : PaymentsAction
}
