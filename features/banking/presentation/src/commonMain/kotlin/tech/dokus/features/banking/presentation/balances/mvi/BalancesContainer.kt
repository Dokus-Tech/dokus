package tech.dokus.features.banking.presentation.balances.mvi

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.features.banking.usecases.GetAccountSummaryUseCase
import tech.dokus.features.banking.usecases.GetBalanceHistoryUseCase
import tech.dokus.features.banking.usecases.GetTransactionSummaryUseCase
import tech.dokus.features.banking.usecases.ListBankConnectionsUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

internal typealias BalancesCtx = PipelineContext<BalancesState, BalancesIntent, BalancesAction>

internal class BalancesContainer(
    private val listConnections: ListBankConnectionsUseCase,
    private val getAccountSummary: GetAccountSummaryUseCase,
    private val getTransactionSummary: GetTransactionSummaryUseCase,
    private val getBalanceHistory: GetBalanceHistoryUseCase,
) : Container<BalancesState, BalancesIntent, BalancesAction> {

    private val logger = Logger.forClass<BalancesContainer>()

    override val store: Store<BalancesState, BalancesIntent, BalancesAction> =
        store(BalancesState.initial) {
            init {
                handleRefresh()
            }

            reduce { intent ->
                when (intent) {
                    is BalancesIntent.Refresh -> handleRefresh()
                    is BalancesIntent.SetTimeRange -> handleSetTimeRange(intent.range)
                }
            }
        }

    private suspend fun BalancesCtx.handleRefresh() {
        updateState {
            copy(
                connections = connections.asLoading,
                summary = summary.asLoading,
                transactionSummary = transactionSummary.asLoading,
                balanceHistory = balanceHistory.asLoading,
            )
        }

        withState {
            val days = timeRange.days

            coroutineScope {
                val connectionsDeferred = async { listConnections() }
                val summaryDeferred = async { getAccountSummary() }
                val txSummaryDeferred = async { getTransactionSummary() }
                val historyDeferred = async { getBalanceHistory(days) }

                val connectionsResult = connectionsDeferred.await()
                val summaryResult = summaryDeferred.await()
                val txSummaryResult = txSummaryDeferred.await()
                val historyResult = historyDeferred.await()

                connectionsResult.fold(
                    onSuccess = { data ->
                        updateState { copy(connections = DokusState.success(data)) }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to load connections" }
                        updateState {
                            copy(
                                connections = DokusState.error(
                                    exception = error.asDokusException,
                                    retryHandler = { intent(BalancesIntent.Refresh) },
                                    lastData = connections.lastData,
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

            getBalanceHistory(range.days).fold(
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
