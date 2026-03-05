package tech.dokus.features.cashflow.presentation.review

import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.Money
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.CashflowPaymentRequest
import tech.dokus.features.cashflow.usecases.GetCashflowEntryUseCase
import tech.dokus.features.cashflow.usecases.GetCashflowPaymentCandidatesUseCase
import tech.dokus.features.cashflow.usecases.RecordCashflowPaymentUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

internal class DocumentReviewPaymentActions(
    private val getCashflowEntry: GetCashflowEntryUseCase,
    private val getCashflowPaymentCandidates: GetCashflowPaymentCandidatesUseCase,
    private val recordCashflowPayment: RecordCashflowPaymentUseCase,
    private val logger: Logger,
) {
    suspend fun DocumentReviewCtx.handleLoadCashflowEntry() {
        withState<DocumentReviewState.Content, _> {
            val entryId = confirmedCashflowEntryId ?: document.cashflowEntryId ?: return@withState
            updateState { copy(cashflowEntryState = DokusState.loading()) }
            launch {
                getCashflowEntry(entryId).fold(
                    onSuccess = { entry ->
                        withState<DocumentReviewState.Content, _> {
                            updateState {
                                copy(
                                    cashflowEntryState = DokusState.success(entry),
                                    confirmedCashflowEntryId = entry.id,
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to load cashflow entry: $entryId" }
                        withState<DocumentReviewState.Content, _> {
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

    suspend fun DocumentReviewCtx.handleOpenPaymentSheet() {
        withState<DocumentReviewState.Content, _> {
            val entry = (cashflowEntryState as? DokusState.Success<*>)?.data as? CashflowEntry ?: run {
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
            launch {
                intent(DocumentReviewIntent.LoadPaymentCandidates)
            }
        }
    }

    suspend fun DocumentReviewCtx.handleClosePaymentSheet() {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(paymentSheetState = null) }
        }
    }

    suspend fun DocumentReviewCtx.handleLoadPaymentCandidates() {
        withState<DocumentReviewState.Content, _> {
            val entry = (cashflowEntryState as? DokusState.Success<*>)?.data as? CashflowEntry ?: return@withState
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
                    onSuccess = { response ->
                        withState<DocumentReviewState.Content, _> {
                            val currentSheet = paymentSheetState ?: return@withState
                            val strongCandidate = response.strongCandidate
                            val selected = strongCandidate
                            val selectedAmount = selected?.signedAmount?.absoluteOrNull()
                            updateState {
                                copy(
                                    paymentSheetState = currentSheet.copy(
                                        suggestedTransaction = strongCandidate,
                                        selectedTransaction = selected,
                                        selectableTransactions = response.selectableTransactions,
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
                        withState<DocumentReviewState.Content, _> {
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
        withState<DocumentReviewState.Content, _> {
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
        withState<DocumentReviewState.Content, _> {
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

    suspend fun DocumentReviewCtx.handleSelectPaymentTransaction(transactionId: tech.dokus.domain.ids.ImportedBankTransactionId) {
        withState<DocumentReviewState.Content, _> {
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
        withState<DocumentReviewState.Content, _> {
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
        withState<DocumentReviewState.Content, _> {
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
        withState<DocumentReviewState.Content, _> {
            val sheet = paymentSheetState ?: return@withState
            updateState { copy(paymentSheetState = sheet.copy(paidAt = date)) }
        }
    }

    suspend fun DocumentReviewCtx.handleUpdatePaymentNote(note: String) {
        withState<DocumentReviewState.Content, _> {
            val sheet = paymentSheetState ?: return@withState
            updateState { copy(paymentSheetState = sheet.copy(note = note)) }
        }
    }

    suspend fun DocumentReviewCtx.handleSubmitPayment() {
        withState<DocumentReviewState.Content, _> {
            val entry = (cashflowEntryState as? DokusState.Success<*>)?.data as? CashflowEntry
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
                        ignoreSuggestedTransaction = shouldIgnoreSuggested
                    )
                ).fold(
                    onSuccess = { updatedEntry ->
                        withState<DocumentReviewState.Content, _> {
                            updateState {
                                copy(
                                    cashflowEntryState = DokusState.success(updatedEntry),
                                    paymentSheetState = null,
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to record payment: ${entry.id}" }
                        withState<DocumentReviewState.Content, _> {
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

    private fun Money.absoluteOrNull(): Money? = when {
        isZero -> null
        isNegative -> -this
        else -> this
    }
}
