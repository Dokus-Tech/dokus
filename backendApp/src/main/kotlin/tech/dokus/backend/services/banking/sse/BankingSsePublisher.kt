package tech.dokus.backend.services.banking.sse

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.TenantId
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

private const val DefaultBufferCapacity = 32

/**
 * Signal emitted when a bank transaction's match state changes.
 */
internal sealed interface BankingMatchSignal {
    data class MatchUpdated(val transactionId: BankTransactionId) : BankingMatchSignal
    data class MatchRemoved(val transactionId: BankTransactionId) : BankingMatchSignal
}

/**
 * Event hub for banking collection-level SSE.
 * Follows the same pattern as [DocumentCollectionEventHub].
 */
internal class BankingCollectionEventHub {
    private val streams = ConcurrentHashMap<TenantId, MutableSharedFlow<BankingMatchSignal>>()

    fun publish(tenantId: TenantId, signal: BankingMatchSignal) {
        streams[tenantId]?.tryEmit(signal)
    }

    fun eventsFor(tenantId: TenantId): SharedFlow<BankingMatchSignal> {
        return streams.getOrPut(tenantId) {
            MutableSharedFlow(
                replay = 0,
                extraBufferCapacity = DefaultBufferCapacity,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        }.asSharedFlow()
    }
}

private data class TransactionKey(
    val tenantId: TenantId,
    val transactionId: BankTransactionId,
)

/**
 * Event hub for per-transaction SSE (detail view).
 * Auto-evicts flows when no subscribers remain.
 */
internal class BankingTransactionEventHub {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val streams = ConcurrentHashMap<TransactionKey, MutableSharedFlow<BankingMatchSignal>>()

    fun eventsFor(
        tenantId: TenantId,
        transactionId: BankTransactionId,
    ): SharedFlow<BankingMatchSignal> {
        val key = TransactionKey(tenantId, transactionId)
        return streams.computeIfAbsent(key) { k ->
            createFlow().also { flow -> scheduleEviction(k, flow) }
        }.asSharedFlow()
    }

    fun publish(tenantId: TenantId, transactionId: BankTransactionId, signal: BankingMatchSignal) {
        val key = TransactionKey(tenantId, transactionId)
        streams[key]?.tryEmit(signal)
    }

    private fun createFlow(): MutableSharedFlow<BankingMatchSignal> {
        return MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = DefaultBufferCapacity,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    }

    private fun scheduleEviction(key: TransactionKey, flow: MutableSharedFlow<BankingMatchSignal>) {
        scope.launch {
            while (true) {
                val subscribed = withTimeoutOrNull(EvictionDelay) {
                    flow.subscriptionCount.first { it > 0 }
                }
                if (subscribed == null) {
                    streams.remove(key, flow)
                    break
                }
                flow.subscriptionCount.first { it == 0 }
                delay(EvictionDelay)
                if (flow.subscriptionCount.value == 0) {
                    streams.remove(key, flow)
                    break
                }
            }
        }
    }

    companion object {
        private val EvictionDelay = 30.seconds
    }
}

/**
 * Publishes banking match state changes to SSE subscribers.
 * Facade over collection and per-transaction event hubs.
 */
internal class BankingSsePublisher(
    private val collectionHub: BankingCollectionEventHub,
    private val transactionHub: BankingTransactionEventHub,
) {
    fun publishMatchUpdated(tenantId: TenantId, transactionId: BankTransactionId) {
        val signal = BankingMatchSignal.MatchUpdated(transactionId)
        transactionHub.publish(tenantId, transactionId, signal)
        collectionHub.publish(tenantId, signal)
    }

    fun publishMatchRemoved(tenantId: TenantId, transactionId: BankTransactionId) {
        val signal = BankingMatchSignal.MatchRemoved(transactionId)
        transactionHub.publish(tenantId, transactionId, signal)
        collectionHub.publish(tenantId, signal)
    }
}
