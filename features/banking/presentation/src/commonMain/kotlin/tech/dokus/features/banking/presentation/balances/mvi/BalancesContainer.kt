package tech.dokus.features.banking.presentation.balances.mvi

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.features.banking.usecases.GetAccountSummaryUseCase
import tech.dokus.features.banking.usecases.GetBalanceHistoryUseCase
import tech.dokus.features.banking.usecases.GetTransactionSummaryUseCase
import tech.dokus.features.banking.usecases.ListBankAccountsUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

internal typealias BalancesCtx = PipelineContext<BalancesState, BalancesIntent, Nothing>

internal class BalancesContainer(
    private val listAccounts: ListBankAccountsUseCase,
    private val getAccountSummary: GetAccountSummaryUseCase,
    private val getTransactionSummary: GetTransactionSummaryUseCase,
    private val getBalanceHistory: GetBalanceHistoryUseCase,
) : Container<BalancesState, BalancesIntent, Nothing> {

    private val logger = Logger.forClass<BalancesContainer>()

    override val store: Store<BalancesState, BalancesIntent, Nothing> =
        store(BalancesState.initial) {
            init {
                handleRefresh()
            }

            reduce { intent ->
                when (intent) {
                    is BalancesIntent.Refresh -> handleRefresh()
                    is BalancesIntent.SetTimeRange -> handleSetTimeRange(intent.range)
                    is BalancesIntent.UploadStatement -> logger.i { "Upload statement (not yet implemented)" }
                    is BalancesIntent.ConnectAccount -> logger.i { "Connect account (not yet implemented)" }
                    is BalancesIntent.DismissActionError -> updateState { copy(actionError = null) }
                }
            }
        }

    private suspend fun BalancesCtx.handleRefresh() {
        updateState {
            copy(
                accounts = accounts.asLoading,
                summary = summary.asLoading,
                transactionSummary = transactionSummary.asLoading,
                balanceHistory = balanceHistory.asLoading,
            )
        }

        withState {
            val days = timeRange.timeframe

            coroutineScope {
                val accountsDeferred = async { listAccounts() }
                val summaryDeferred = async { getAccountSummary() }
                val txSummaryDeferred = async { getTransactionSummary() }
                val historyDeferred = async { getBalanceHistory(days) }

                val accountsResult = accountsDeferred.await()
                val summaryResult = summaryDeferred.await()
                val txSummaryResult = txSummaryDeferred.await()
                val historyResult = historyDeferred.await()

                accountsResult.fold(
                    onSuccess = { data ->
                        updateState { copy(accounts = DokusState.success(data)) }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to load accounts" }
                        updateState {
                            copy(
                                accounts = DokusState.error(
                                    exception = error.asDokusException,
                                    retryHandler = { intent(BalancesIntent.Refresh) },
                                    lastData = accounts.lastData,
                                )
                            )
                        }
                    }
                )

                summaryResult.fold(
                    onSuccess = { data ->
                        updateState { copy(summary = DokusState.success(data)) }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to load account summary" }
                        updateState {
                            copy(
                                summary = DokusState.error(
                                    exception = error.asDokusException,
                                    retryHandler = { intent(BalancesIntent.Refresh) },
                                    lastData = summary.lastData,
                                )
                            )
                        }
                    }
                )

                txSummaryResult.fold(
                    onSuccess = { data ->
                        updateState { copy(transactionSummary = DokusState.success(data)) }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to load transaction summary" }
                        updateState {
                            copy(
                                transactionSummary = DokusState.error(
                                    exception = error.asDokusException,
                                    retryHandler = { intent(BalancesIntent.Refresh) },
                                    lastData = transactionSummary.lastData,
                                )
                            )
                        }
                    }
                )

                historyResult.fold(
                    onSuccess = { data ->
                        updateState { copy(balanceHistory = DokusState.success(data)) }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to load balance history" }
                        updateState {
                            copy(
                                balanceHistory = DokusState.error(
                                    exception = error.asDokusException,
                                    retryHandler = { intent(BalancesIntent.Refresh) },
                                    lastData = balanceHistory.lastData,
                                )
                            )
                        }
                    }
                )
            }
        }
    }

    private suspend fun BalancesCtx.handleSetTimeRange(range: BalanceTimeRange) {
        withState {
            if (timeRange == range) return@withState

            updateState {
                copy(
                    timeRange = range,
                    balanceHistory = balanceHistory.asLoading,
                )
            }

            getBalanceHistory(range.timeframe).fold(
                onSuccess = { data ->
                    updateState { copy(balanceHistory = DokusState.success(data)) }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load balance history for range ${range.id}" }
                    updateState {
                        copy(
                            balanceHistory = DokusState.error(
                                exception = error.asDokusException,
                                retryHandler = { intent(BalancesIntent.SetTimeRange(range)) },
                                lastData = balanceHistory.lastData,
                            )
                        )
                    }
                }
            )
        }
    }
}
