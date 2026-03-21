package tech.dokus.features.cashflow.presentation.review.mvi.payment

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.model.AutoPaymentStatus
import tech.dokus.domain.model.CashflowEntryDto
import tech.dokus.features.cashflow.presentation.review.PaymentSheetState
import tech.dokus.foundation.app.state.DokusState

@Immutable
data class DocumentPaymentState(
    val confirmedCashflowEntryId: CashflowEntryId? = null,
    val cashflowEntryState: DokusState<CashflowEntryDto> = DokusState.idle(),
    val autoPaymentStatus: DokusState<AutoPaymentStatus> = DokusState.idle(),
    val isUndoingAutoPayment: Boolean = false,
    val paymentSheetState: PaymentSheetState? = null,
) : MVIState

@Immutable
sealed interface DocumentPaymentIntent : MVIIntent {
    data class SetCashflowEntryId(val id: CashflowEntryId) : DocumentPaymentIntent
    data object LoadCashflowEntry : DocumentPaymentIntent
    data object LoadAutoPaymentStatus : DocumentPaymentIntent
    data object OpenPaymentSheet : DocumentPaymentIntent
    data object ClosePaymentSheet : DocumentPaymentIntent
    data object LoadPaymentCandidates : DocumentPaymentIntent
    data object OpenPaymentTransactionPicker : DocumentPaymentIntent
    data object ClosePaymentTransactionPicker : DocumentPaymentIntent
    data class SelectPaymentTransaction(val transactionId: BankTransactionId) : DocumentPaymentIntent
    data object ClearPaymentTransactionSelection : DocumentPaymentIntent
    data class UpdatePaymentAmountText(val text: String) : DocumentPaymentIntent
    data class UpdatePaymentPaidAt(val date: LocalDate) : DocumentPaymentIntent
    data class UpdatePaymentNote(val note: String) : DocumentPaymentIntent
    data object SubmitPayment : DocumentPaymentIntent
    data class UndoAutoPayment(val reason: String? = null) : DocumentPaymentIntent
}

@Immutable
sealed interface DocumentPaymentAction : MVIAction {
    data object PaymentRecorded : DocumentPaymentAction
    data object AutoPaymentUndone : DocumentPaymentAction
    data class NavigateToCashflowEntry(val entryId: CashflowEntryId) : DocumentPaymentAction
    data class ShowError(val error: DokusException) : DocumentPaymentAction
}
