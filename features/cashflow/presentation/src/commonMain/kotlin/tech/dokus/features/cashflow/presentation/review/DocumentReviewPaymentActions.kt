package tech.dokus.features.cashflow.presentation.review

import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.model.CashflowEntryDto
import tech.dokus.domain.model.CashflowPaymentRequest
import tech.dokus.domain.model.UndoAutoPaymentRequest
import tech.dokus.features.cashflow.usecases.GetCashflowEntryUseCase
import tech.dokus.features.cashflow.usecases.GetAutoPaymentStatusUseCase
import tech.dokus.features.cashflow.usecases.GetCashflowPaymentCandidatesUseCase
import tech.dokus.features.cashflow.usecases.RecordCashflowPaymentUseCase
import tech.dokus.features.cashflow.usecases.UndoAutoPaymentUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

internal class DocumentReviewPaymentActions(
    private val getCashflowEntry: GetCashflowEntryUseCase,
    private val getCashflowPaymentCandidates: GetCashflowPaymentCandidatesUseCase,
    private val getAutoPaymentStatus: GetAutoPaymentStatusUseCase,
    private val recordCashflowPayment: RecordCashflowPaymentUseCase,
    private val undoAutoPayment: UndoAutoPaymentUseCase,
    private val logger: Logger,
) {
    suspend fun DocumentReviewCtx.handleLoadCashflowEntry() {
        withState {
            val entryId = confirmedCashflowEntryId ?: documentRecord?.cashflowEntryId ?: return@withState
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
                            intent(DocumentReviewIntent.LoadAutoPaymentStatus)
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to load cashflow entry: $entryId" }
                        withState {
                            updateState {
                                copy(
                                    cashflowEntryState = DokusState.error(
                                        exception = error.asDokusException,
                                        retryHandler = { intent(DocumentReviewIntent.LoadCashflowEntry) }
                                    )
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleLoadAutoPaymentStatus() {
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
                                        retryHandler = { intent(DocumentReviewIntent.LoadAutoPaymentStatus) }
                                    )
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleOpenPaymentSheet() {
        withState {
            val entry = (cashflowEntryState as? DokusState.Success<*>)?.data as? CashflowEntryDto ?: run {
                intent(DocumentReviewIntent.LoadCashflowEntry)
                return@withState
            }
            updateState {
                copy(
                    paymentSheetState = PaymentSheetState(
                        amountText = entry.remainingAmount.toDisplayString(),
                        amount = entry.remainingAmount,
                        isLoadingTransactions = true
                    )
                )
            }
            intent(DocumentReviewIntent.LoadPaymentCandidates)
        }
    }

    suspend fun DocumentReviewCtx.handleClosePaymentSheet() {
        withState {
            updateState { copy(paymentSheetState = null) }
        }
    }

    suspend fun DocumentReviewCtx.handleLoadPaymentCandidates() {
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
                                        amountText = selectedAmount?.toDisplayString() ?: currentSheet.amountText,
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

    suspend fun DocumentReviewCtx.handleOpenPaymentTransactionPicker() {
        withState {
            val sheet = paymentSheetState ?: return@withState
            updateState {
                copy(
                    paymentSheetState = sheet.copy(
                        showTransactionPicker = true
                    )
                )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleClosePaymentTransactionPicker() {
        withState {
            val sheet = paymentSheetState ?: return@withState
            updateState {
                copy(
                    paymentSheetState = sheet.copy(
                        showTransactionPicker = false
                    )
                )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleSelectPaymentTransaction(transactionId: tech.dokus.domain.ids.BankTransactionId) {
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
                        amountText = selectedAmount.toDisplayString(),
                        amountError = null,
                        showTransactionPicker = false
                    )
                )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleClearPaymentTransactionSelection() {
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

    suspend fun DocumentReviewCtx.handleUpdatePaymentAmountText(text: String) {
        withState {
            val sheet = paymentSheetState ?: return@withState
            updateState {
                copy(
                    paymentSheetState = sheet.copy(
                        amountText = text,
                        amount = Money.from(text),
                        amountError = null
                    )
                )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleUpdatePaymentPaidAt(date: LocalDate) {
        withState {
            val sheet = paymentSheetState ?: return@withState
            updateState { copy(paymentSheetState = sheet.copy(paidAt = date)) }
        }
    }

    suspend fun DocumentReviewCtx.handleUpdatePaymentNote(note: String) {
        withState {
            val sheet = paymentSheetState ?: return@withState
            updateState { copy(paymentSheetState = sheet.copy(note = note)) }
        }
    }

    suspend fun DocumentReviewCtx.handleSubmitPayment() {
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
                            intent(DocumentReviewIntent.LoadAutoPaymentStatus)
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to record payment: ${entry.id}" }
                        withState {
                            val currentSheet = paymentSheetState
                            if (currentSheet != null) {
                                updateState {
                                    copy(
                                        paymentSheetState = currentSheet.copy(isSubmitting = false),
                                    )
                                }
                            }
                        }
                        action(DocumentReviewAction.ShowError(error.asDokusException))
                    }
                )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleUndoAutoPayment(reason: String?) {
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
                            intent(DocumentReviewIntent.LoadAutoPaymentStatus)
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to undo auto payment: ${entry.id}" }
                        withState {
                            updateState { copy(isUndoingAutoPayment = false) }
                        }
                        action(DocumentReviewAction.ShowError(error.asDokusException))
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
