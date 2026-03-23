@file:Suppress("TooManyFunctions")

package tech.dokus.features.cashflow.presentation.detail.mvi.payment

import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.model.CashflowEntryDto
import tech.dokus.domain.model.CashflowPaymentRequest
import tech.dokus.domain.model.UndoAutoPaymentRequest
import tech.dokus.features.cashflow.presentation.detail.PaymentSheetState
import tech.dokus.features.cashflow.usecases.GetAutoPaymentStatusUseCase
import tech.dokus.features.cashflow.usecases.GetCashflowEntryUseCase
import tech.dokus.features.cashflow.usecases.GetCashflowPaymentCandidatesUseCase
import tech.dokus.features.cashflow.usecases.RecordCashflowPaymentUseCase
import tech.dokus.features.cashflow.usecases.UndoAutoPaymentUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

private typealias PaymentCtx = PipelineContext<DocumentPaymentState, DocumentPaymentIntent, DocumentPaymentAction>

internal class DocumentPaymentContainer(
    private val getCashflowEntry: GetCashflowEntryUseCase,
    private val getCashflowPaymentCandidates: GetCashflowPaymentCandidatesUseCase,
    private val getAutoPaymentStatus: GetAutoPaymentStatusUseCase,
    private val recordCashflowPayment: RecordCashflowPaymentUseCase,
    private val undoAutoPayment: UndoAutoPaymentUseCase,
) : Container<DocumentPaymentState, DocumentPaymentIntent, DocumentPaymentAction> {

    private val logger = Logger.forClass<DocumentPaymentContainer>()

    override val store: Store<DocumentPaymentState, DocumentPaymentIntent, DocumentPaymentAction> =
        store(DocumentPaymentState()) {
            reduce { intent ->
                when (intent) {
                    is DocumentPaymentIntent.SetCashflowEntryId -> handleSetCashflowEntryId(intent.id)
                    is DocumentPaymentIntent.LoadCashflowEntry -> handleLoadCashflowEntry()
                    is DocumentPaymentIntent.LoadAutoPaymentStatus -> handleLoadAutoPaymentStatus()
                    is DocumentPaymentIntent.OpenPaymentSheet -> handleOpenPaymentSheet()
                    is DocumentPaymentIntent.ClosePaymentSheet -> handleClosePaymentSheet()
                    is DocumentPaymentIntent.LoadPaymentCandidates -> handleLoadPaymentCandidates()
                    is DocumentPaymentIntent.OpenPaymentTransactionPicker -> handleOpenPaymentTransactionPicker()
                    is DocumentPaymentIntent.ClosePaymentTransactionPicker -> handleClosePaymentTransactionPicker()
                    is DocumentPaymentIntent.SelectPaymentTransaction ->
                        handleSelectPaymentTransaction(intent.transactionId)
                    is DocumentPaymentIntent.ClearPaymentTransactionSelection ->
                        handleClearPaymentTransactionSelection()
                    is DocumentPaymentIntent.UpdatePaymentAmountText ->
                        handleUpdatePaymentAmountText(intent.text)
                    is DocumentPaymentIntent.UpdatePaymentPaidAt ->
                        handleUpdatePaymentPaidAt(intent.date)
                    is DocumentPaymentIntent.UpdatePaymentNote -> handleUpdatePaymentNote(intent.note)
                    is DocumentPaymentIntent.SubmitPayment -> handleSubmitPayment()
                    is DocumentPaymentIntent.UndoAutoPayment -> handleUndoAutoPayment(intent.reason)
                }
            }
        }

    private suspend fun PaymentCtx.handleSetCashflowEntryId(
        id: tech.dokus.domain.ids.CashflowEntryId,
    ) {
        updateState { copy(confirmedCashflowEntryId = id) }
    }

    private suspend fun PaymentCtx.handleLoadCashflowEntry() {
        withState {
            val entryId = confirmedCashflowEntryId ?: return@withState
            updateState { copy(cashflowEntryState = DokusState.loading()) }
            launch {
                getCashflowEntry(entryId).fold(
                    onSuccess = { entry ->
                        withState {
                            updateState {
                                copy(
                                    cashflowEntryState = DokusState.success(entry),
                                    confirmedCashflowEntryId = entry.id,
                                )
                            }
                            intent(DocumentPaymentIntent.LoadAutoPaymentStatus)
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to load cashflow entry: $entryId" }
                        withState {
                            updateState {
                                copy(
                                    cashflowEntryState = DokusState.error(
                                        exception = error.asDokusException,
                                        retryHandler = { intent(DocumentPaymentIntent.LoadCashflowEntry) }
                                    )
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    private suspend fun PaymentCtx.handleLoadAutoPaymentStatus() {
        withState {
            val entry = (cashflowEntryState as? DokusState.Success<*>)?.data as? CashflowEntryDto ?: run {
                updateState { copy(autoPaymentStatus = DokusState.idle()) }
                return@withState
            }
            updateState { copy(autoPaymentStatus = DokusState.loading()) }
            launch {
                getAutoPaymentStatus(entry.id).fold(
                    onSuccess = { status ->
                        withState {
                            updateState { copy(autoPaymentStatus = DokusState.success(status)) }
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to load auto-payment status for entry: ${entry.id}" }
                        withState {
                            updateState {
                                copy(
                                    autoPaymentStatus = DokusState.error(
                                        exception = error.asDokusException,
                                        retryHandler = { intent(DocumentPaymentIntent.LoadAutoPaymentStatus) }
                                    )
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    private suspend fun PaymentCtx.handleOpenPaymentSheet() {
        withState {
            val entry = (cashflowEntryState as? DokusState.Success<*>)?.data as? CashflowEntryDto ?: run {
                intent(DocumentPaymentIntent.LoadCashflowEntry)
                return@withState
            }
            updateState {
                copy(
                    paymentSheetState = PaymentSheetState(
                        amountText = entry.remainingAmount.formatAmount(),
                        amount = entry.remainingAmount,
                        isLoadingTransactions = true
                    )
                )
            }
            intent(DocumentPaymentIntent.LoadPaymentCandidates)
        }
    }

    private suspend fun PaymentCtx.handleClosePaymentSheet() {
        withState {
            updateState { copy(paymentSheetState = null) }
        }
    }

    private suspend fun PaymentCtx.handleLoadPaymentCandidates() {
        withState {
            val entry = (cashflowEntryState as? DokusState.Success<*>)?.data as? CashflowEntryDto ?: return@withState
            val sheet = paymentSheetState ?: return@withState
            updateState {
                copy(
                    paymentSheetState = sheet.copy(
                        isLoadingTransactions = true,
                        transactionsError = null
                    )
                )
            }
            launch {
                getCashflowPaymentCandidates(entry.id).fold(
                    onSuccess = { candidates ->
                        withState {
                            val currentSheet = paymentSheetState ?: return@withState
                            val strongCandidate = candidates.firstOrNull {
                                it.status == BankTransactionStatus.NeedsReview
                            }
                            val selectableTransactions = candidates.filter {
                                it.status == BankTransactionStatus.Unmatched
                            }
                            val selected = strongCandidate
                            val selectedAmount = selected?.signedAmount?.absoluteOrNull()
                            updateState {
                                copy(
                                    paymentSheetState = currentSheet.copy(
                                        suggestedTransaction = strongCandidate,
                                        selectedTransaction = selected,
                                        selectableTransactions = selectableTransactions,
                                        paidAt = selected?.transactionDate ?: currentSheet.paidAt,
                                        amount = selectedAmount ?: currentSheet.amount,
                                        amountText = selectedAmount?.formatAmount() ?: currentSheet.amountText,
                                        isLoadingTransactions = false,
                                        transactionsError = null
                                    )
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to load payment candidates for entry: ${entry.id}" }
                        withState {
                            val currentSheet = paymentSheetState ?: return@withState
                            updateState {
                                copy(
                                    paymentSheetState = currentSheet.copy(
                                        isLoadingTransactions = false,
                                        transactionsError = error.asDokusException
                                    )
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    private suspend fun PaymentCtx.handleOpenPaymentTransactionPicker() {
        withState {
            val sheet = paymentSheetState ?: return@withState
            updateState {
                copy(paymentSheetState = sheet.copy(showTransactionPicker = true))
            }
        }
    }

    private suspend fun PaymentCtx.handleClosePaymentTransactionPicker() {
        withState {
            val sheet = paymentSheetState ?: return@withState
            updateState {
                copy(paymentSheetState = sheet.copy(showTransactionPicker = false))
            }
        }
    }

    private suspend fun PaymentCtx.handleSelectPaymentTransaction(
        transactionId: tech.dokus.domain.ids.BankTransactionId,
    ) {
        withState {
            val sheet = paymentSheetState ?: return@withState
            val selected = (
                listOfNotNull(sheet.suggestedTransaction) + sheet.selectableTransactions
                ).firstOrNull { it.id == transactionId } ?: return@withState

            val selectedAmount = selected.signedAmount.absoluteOrNull() ?: return@withState
            updateState {
                copy(
                    paymentSheetState = sheet.copy(
                        selectedTransaction = selected,
                        paidAt = selected.transactionDate,
                        amount = selectedAmount,
                        amountText = selectedAmount.formatAmount(),
                        amountError = null,
                        showTransactionPicker = false
                    )
                )
            }
        }
    }

    private suspend fun PaymentCtx.handleClearPaymentTransactionSelection() {
        withState {
            val sheet = paymentSheetState ?: return@withState
            updateState {
                copy(
                    paymentSheetState = sheet.copy(
                        selectedTransaction = null,
                        showTransactionPicker = false
                    )
                )
            }
        }
    }

    private suspend fun PaymentCtx.handleUpdatePaymentAmountText(text: String) {
        withState {
            val sheet = paymentSheetState ?: return@withState
            val currency = sheet.amount?.currency
                ?: (cashflowEntryState as? DokusState.Success<CashflowEntryDto>)?.data?.currency
                ?: tech.dokus.domain.enums.Currency.Eur
            updateState {
                copy(
                    paymentSheetState = sheet.copy(
                        amountText = text,
                        amount = Money.from(text, currency),
                        amountError = null
                    )
                )
            }
        }
    }

    private suspend fun PaymentCtx.handleUpdatePaymentPaidAt(date: kotlinx.datetime.LocalDate) {
        withState {
            val sheet = paymentSheetState ?: return@withState
            updateState { copy(paymentSheetState = sheet.copy(paidAt = date)) }
        }
    }

    private suspend fun PaymentCtx.handleUpdatePaymentNote(note: String) {
        withState {
            val sheet = paymentSheetState ?: return@withState
            updateState { copy(paymentSheetState = sheet.copy(note = note)) }
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private suspend fun PaymentCtx.handleSubmitPayment() {
        withState {
            val entry = (cashflowEntryState as? DokusState.Success<*>)?.data as? CashflowEntryDto
                ?: return@withState
            val sheet = paymentSheetState ?: return@withState

            val amount = sheet.amount
            if (amount == null || !amount.isPositive) {
                updateState {
                    copy(
                        paymentSheetState = sheet.copy(
                            amountError = DokusException.Validation.PaymentAmountMustBePositive
                        )
                    )
                }
                return@withState
            }
            if (amount > entry.remainingAmount) {
                updateState {
                    copy(
                        paymentSheetState = sheet.copy(
                            amountError = DokusException.Validation.PaymentAmountExceedsRemaining
                        )
                    )
                }
                return@withState
            }

            updateState { copy(paymentSheetState = sheet.copy(isSubmitting = true, amountError = null)) }
            launch {
                val shouldIgnoreSuggested = sheet.selectedTransaction == null && sheet.suggestedTransaction != null
                recordCashflowPayment(
                    entryId = entry.id,
                    request = CashflowPaymentRequest(
                        amount = amount,
                        paidAt = LocalDateTime(sheet.paidAt, LocalTime(0, 0)),
                        note = sheet.note.ifBlank { null },
                        bankTransactionId = sheet.selectedTransaction?.id,
                        dismissSuggestedMatch = shouldIgnoreSuggested
                    )
                ).fold(
                    onSuccess = { updatedEntry ->
                        withState {
                            updateState {
                                copy(
                                    cashflowEntryState = DokusState.success(updatedEntry),
                                    paymentSheetState = null,
                                )
                            }
                            intent(DocumentPaymentIntent.LoadAutoPaymentStatus)
                        }
                        action(DocumentPaymentAction.PaymentRecorded)
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to record payment: ${entry.id}" }
                        withState {
                            val currentSheet = paymentSheetState
                            if (currentSheet != null) {
                                updateState {
                                    copy(paymentSheetState = currentSheet.copy(isSubmitting = false))
                                }
                            }
                        }
                        action(DocumentPaymentAction.ShowError(error.asDokusException))
                    }
                )
            }
        }
    }

    private suspend fun PaymentCtx.handleUndoAutoPayment(reason: String?) {
        withState {
            if (isUndoingAutoPayment) return@withState
            val entry = (cashflowEntryState as? DokusState.Success<*>)?.data as? CashflowEntryDto ?: return@withState
            updateState { copy(isUndoingAutoPayment = true) }
            launch {
                undoAutoPayment(
                    entryId = entry.id,
                    request = UndoAutoPaymentRequest(reason = reason)
                ).fold(
                    onSuccess = { updatedEntry ->
                        withState {
                            updateState {
                                copy(
                                    isUndoingAutoPayment = false,
                                    cashflowEntryState = DokusState.success(updatedEntry),
                                )
                            }
                            intent(DocumentPaymentIntent.LoadAutoPaymentStatus)
                        }
                        action(DocumentPaymentAction.AutoPaymentUndone)
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to undo auto payment: ${entry.id}" }
                        withState {
                            updateState { copy(isUndoingAutoPayment = false) }
                        }
                        action(DocumentPaymentAction.ShowError(error.asDokusException))
                    }
                )
            }
        }
    }

    private fun Money.absoluteOrNull(): Money? = when {
        isZero -> null
        isNegative -> -this
        else -> this
    }
}
