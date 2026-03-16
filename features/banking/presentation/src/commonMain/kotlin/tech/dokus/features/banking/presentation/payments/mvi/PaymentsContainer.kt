package tech.dokus.features.banking.presentation.payments.mvi

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.IgnoredReason
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.features.banking.usecases.ConfirmTransactionUseCase
import tech.dokus.features.banking.usecases.CreateExpenseFromTransactionUseCase
import tech.dokus.features.banking.usecases.GetTransactionSummaryUseCase
import tech.dokus.features.banking.usecases.IgnoreTransactionUseCase
import tech.dokus.features.banking.usecases.ListBankAccountsUseCase
import tech.dokus.features.banking.usecases.ListBankTransactionsUseCase
import tech.dokus.features.banking.usecases.MarkTransferTransactionUseCase
import tech.dokus.features.banking.usecases.UndoTransferTransactionUseCase
import tech.dokus.domain.model.MarkTransferMode
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.platform.Logger

internal typealias PaymentsCtx = PipelineContext<PaymentsState, PaymentsIntent, PaymentsAction>

private const val PAGE_SIZE = 50

internal class PaymentsContainer(
    private val listTransactions: ListBankTransactionsUseCase,
    private val getTransactionSummary: GetTransactionSummaryUseCase,
    private val listAccounts: ListBankAccountsUseCase,
    private val ignoreTransaction: IgnoreTransactionUseCase,
    private val confirmTransaction: ConfirmTransactionUseCase,
    private val createExpenseFromTransaction: CreateExpenseFromTransactionUseCase,
    private val markTransferTransaction: MarkTransferTransactionUseCase,
    private val undoTransferTransaction: UndoTransferTransactionUseCase,
) : Container<PaymentsState, PaymentsIntent, PaymentsAction> {

    private val logger = Logger.forClass<PaymentsContainer>()

    override val store: Store<PaymentsState, PaymentsIntent, PaymentsAction> =
        store(PaymentsState.initial) {
            init {
                handleRefresh()
            }

            reduce { intent ->
                when (intent) {
                    is PaymentsIntent.Refresh -> handleRefresh()
                    is PaymentsIntent.LoadMore -> handleLoadMore()
                    is PaymentsIntent.SetFilterTab -> handleSetFilterTab(intent.tab)
                    is PaymentsIntent.SetAccountFilter -> handleSetAccountFilter(intent.accountId)
                    is PaymentsIntent.SelectTransaction -> handleSelectTransaction(intent.transactionId)
                    is PaymentsIntent.LinkDocument -> { /* TODO: Navigate to link flow */ }
                    is PaymentsIntent.IgnoreTransaction -> handleOpenIgnoreDialog(intent.transactionId)
                    is PaymentsIntent.SelectIgnoreReason -> handleSelectIgnoreReason(intent.reason)
                    is PaymentsIntent.ConfirmIgnore -> handleConfirmIgnore()
                    is PaymentsIntent.DismissIgnoreDialog -> handleDismissIgnoreDialog()
                    is PaymentsIntent.ConfirmMatch -> handleConfirmMatch(intent.transactionId)
                    is PaymentsIntent.CreateExpense -> handleCreateExpense(intent.transactionId)
                    is PaymentsIntent.MarkTransfer -> handleOpenTransferDialog(intent.transactionId)
                    is PaymentsIntent.SelectTransferDestination -> handleSelectTransferDestination(intent.accountId)
                    is PaymentsIntent.ConfirmTransfer -> handleConfirmTransfer()
                    is PaymentsIntent.DismissTransferDialog -> handleDismissTransferDialog()
                    is PaymentsIntent.UndoTransfer -> handleUndoTransfer(intent.transactionId)
                }
            }
        }

    private suspend fun PaymentsCtx.handleRefresh() {
        updateState { copy(transactions = transactions.asLoading, summary = summary.asLoading) }
        loadTransactions(page = 0, reset = true)
    }

    private suspend fun PaymentsCtx.handleLoadMore() {
        withState {
            val paginationState =
                transactions.let { if (it.isSuccess()) it.data else return@withState }

            if (!paginationState.hasMorePages) return@withState

            updateState { copy(transactions = transactions.asLoading) }
            loadTransactions(page = paginationState.currentPage + 1, reset = false)
        }
    }

    private suspend fun PaymentsCtx.handleSetFilterTab(tab: PaymentFilterTab) {
        withState {
            if (filterTab == tab) return@withState
            updateState {
                copy(
                    transactions = transactions.asLoading,
                    filterTab = tab,
                    selectedTransactionId = null,
                )
            }
            loadTransactions(page = 0, reset = true)
        }
    }

    private suspend fun PaymentsCtx.handleSetAccountFilter(accountId: BankAccountId?) {
        updateState { copy(selectedAccountId = accountId, selectedTransactionId = null) }
    }

    private suspend fun PaymentsCtx.handleSelectTransaction(transactionId: BankTransactionId?) {
        updateState { copy(selectedTransactionId = transactionId) }
    }

    private suspend fun PaymentsCtx.handleOpenIgnoreDialog(transactionId: BankTransactionId) {
        updateState { copy(ignoreDialogState = IgnoreDialogState(transactionId = transactionId)) }
    }

    private suspend fun PaymentsCtx.handleSelectIgnoreReason(reason: IgnoredReason) {
        updateState { copy(ignoreDialogState = ignoreDialogState?.copy(selectedReason = reason)) }
    }

    private suspend fun PaymentsCtx.handleDismissIgnoreDialog() {
        updateState { copy(ignoreDialogState = null) }
    }

    private suspend fun PaymentsCtx.handleConfirmIgnore() {
        withState {
            val dialog = ignoreDialogState ?: return@withState
            val reason = dialog.selectedReason ?: return@withState

            updateState { copy(ignoreDialogState = null) }

            ignoreTransaction(dialog.transactionId, reason).fold(
                onSuccess = { updatedTx ->
                    updateTransactionInList(updatedTx.id) { updatedTx }
                    refreshSummary()
                },
                onFailure = { error ->
                    action(PaymentsAction.ShowError(error.asDokusException))
                }
            )
        }
    }

    private suspend fun PaymentsCtx.handleConfirmMatch(transactionId: BankTransactionId) {
        confirmTransaction(transactionId).fold(
            onSuccess = { updatedTx ->
                updateTransactionInList(updatedTx.id) { updatedTx }
                refreshSummary()
            },
            onFailure = { error ->
                action(PaymentsAction.ShowError(error.asDokusException))
            }
        )
    }

    private suspend fun PaymentsCtx.handleCreateExpense(transactionId: BankTransactionId) {
        createExpenseFromTransaction(transactionId).fold(
            onSuccess = { updatedTx ->
                updateTransactionInList(updatedTx.id) { updatedTx }
                refreshSummary()
            },
            onFailure = { error ->
                action(PaymentsAction.ShowError(error.asDokusException))
            }
        )
    }

    // ─── Transfer handlers ────────────────────────────────────────────

    private suspend fun PaymentsCtx.handleOpenTransferDialog(transactionId: BankTransactionId) {
        withState {
            val tx = transactions.lastData?.data?.find { it.id == transactionId } ?: return@withState
            val accounts = listAccounts().getOrNull() ?: emptyList()
            val filtered = accounts.filter { it.id != tx.bankAccountId && it.isActive }
            updateState {
                copy(
                    transferDialogState = TransferDialogState(
                        transactionId = transactionId,
                        sourceAccountId = tx.bankAccountId,
                        availableAccounts = filtered,
                    )
                )
            }
        }
    }

    private suspend fun PaymentsCtx.handleSelectTransferDestination(accountId: BankAccountId) {
        updateState {
            copy(transferDialogState = transferDialogState?.copy(selectedDestinationAccountId = accountId))
        }
    }

    private suspend fun PaymentsCtx.handleDismissTransferDialog() {
        updateState { copy(transferDialogState = null) }
    }

    private suspend fun PaymentsCtx.handleConfirmTransfer() {
        withState {
            val dialog = transferDialogState ?: return@withState
            val destinationId = dialog.selectedDestinationAccountId ?: return@withState

            updateState { copy(transferDialogState = dialog.copy(isSubmitting = true)) }

            markTransferTransaction(
                transactionId = dialog.transactionId,
                mode = MarkTransferMode.OneSided,
                destinationAccountId = destinationId,
            ).fold(
                onSuccess = { updatedTx ->
                    updateState { copy(transferDialogState = null) }
                    updateTransactionInList(updatedTx.id) { updatedTx }
                    refreshSummary()
                },
                onFailure = { error ->
                    updateState { copy(transferDialogState = transferDialogState?.copy(isSubmitting = false)) }
                    action(PaymentsAction.ShowError(error.asDokusException))
                }
            )
        }
    }

    private suspend fun PaymentsCtx.handleUndoTransfer(transactionId: BankTransactionId) {
        undoTransferTransaction(transactionId).fold(
            onSuccess = { updatedTx ->
                updateTransactionInList(updatedTx.id) { updatedTx }
                refreshSummary()
            },
            onFailure = { error ->
                action(PaymentsAction.ShowError(error.asDokusException))
            }
        )
    }

    private suspend fun PaymentsCtx.loadTransactions(page: Int, reset: Boolean) {
        withState {
            val statusFilter = filterTab.toStatusFilter()

            if (reset) {
                logger.d { "Refreshing transactions, filter=$filterTab" }

                coroutineScope {
                    val txDeferred = async {
                        listTransactions(
                            page = 0,
                            pageSize = PAGE_SIZE,
                            status = statusFilter,
                        )
                    }
                    val summaryDeferred = async { getTransactionSummary() }
                    val accountsDeferred = async { listAccounts() }

                    val txResult = txDeferred.await()
                    val summaryResult = summaryDeferred.await()
                    val accountsResult = accountsDeferred.await()

                    accountsResult.onSuccess { accounts ->
                        val names = accounts.associate { it.id to it.name }
                        updateState { copy(accountNames = names) }
                    }

                    txResult.fold(
                        onSuccess = { response ->
                            updateState {
                                copy(
                                    transactions = DokusState.success(
                                        PaginationState(
                                            data = response.items,
                                            currentPage = 0,
                                            pageSize = PAGE_SIZE,
                                            hasMorePages = response.hasMore,
                                        )
                                    ),
                                    selectedTransactionId = selectedTransactionId?.takeIf { id ->
                                        response.items.any { it.id == id }
                                    },
                                )
                            }
                        },
                        onFailure = { error ->
                            logger.e(error) { "Failed to load transactions" }
                            val dokusError = error.asDokusException
                            val hadData = transactions.lastData != null
                            updateState {
                                copy(
                                    transactions = DokusState.error(
                                        exception = dokusError,
                                        retryHandler = { intent(PaymentsIntent.Refresh) },
                                        lastData = transactions.lastData,
                                    )
                                )
                            }
                            if (hadData) {
                                action(PaymentsAction.ShowError(dokusError))
                            }
                        }
                    )

                    summaryResult.fold(
                        onSuccess = { summary ->
                            updateState { copy(summary = DokusState.success(summary)) }
                        },
                        onFailure = { error ->
                            logger.e(error) { "Failed to load summary" }
                            updateState {
                                copy(
                                    summary = DokusState.error(
                                        exception = error.asDokusException,
                                        retryHandler = { intent(PaymentsIntent.Refresh) },
                                        lastData = summary.lastData,
                                    )
                                )
                            }
                        }
                    )
                }
            } else {
                logger.d { "Loading more transactions, page=$page" }

                listTransactions(
                    page = page,
                    pageSize = PAGE_SIZE,
                    status = statusFilter,
                ).fold(
                    onSuccess = { response ->
                        val previousData = transactions.lastData?.data ?: emptyList()
                        updateState {
                            copy(
                                transactions = DokusState.success(
                                    PaginationState(
                                        data = previousData + response.items,
                                        currentPage = page,
                                        pageSize = PAGE_SIZE,
                                        hasMorePages = response.hasMore,
                                    )
                                )
                            )
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to load more transactions" }
                        updateState {
                            copy(
                                transactions = DokusState.error(
                                    exception = error.asDokusException,
                                    retryHandler = { intent(PaymentsIntent.Refresh) },
                                    lastData = transactions.lastData,
                                )
                            )
                        }
                        action(PaymentsAction.ShowError(error.asDokusException))
                    }
                )
            }
        }
    }

    private suspend fun PaymentsCtx.refreshSummary() {
        getTransactionSummary().fold(
            onSuccess = { summary ->
                updateState { copy(summary = DokusState.success(summary)) }
            },
            onFailure = { /* silent — summary refresh is best-effort */ }
        )
    }

    private suspend fun PaymentsCtx.updateTransactionInList(
        transactionId: BankTransactionId,
        transform: (tech.dokus.domain.model.BankTransactionDto) -> tech.dokus.domain.model.BankTransactionDto,
    ) {
        withState {
            val currentPagination = transactions.lastData ?: return@withState
            val updatedList = currentPagination.data.map {
                if (it.id == transactionId) transform(it) else it
            }
            updateState {
                copy(transactions = DokusState.success(currentPagination.copy(data = updatedList)))
            }
        }
    }
}

private fun PaymentFilterTab.toStatusFilter(): BankTransactionStatus? = when (this) {
    PaymentFilterTab.All -> null
    PaymentFilterTab.NeedsReview -> BankTransactionStatus.NeedsReview
    PaymentFilterTab.Unmatched -> BankTransactionStatus.Unmatched
    PaymentFilterTab.Matched -> BankTransactionStatus.Matched
    PaymentFilterTab.Ignored -> BankTransactionStatus.Ignored
}
