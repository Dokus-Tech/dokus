package tech.dokus.features.ai.queue

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeoutOrNull
import tech.dokus.foundation.backend.utils.loggerFor
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

// ---------------------------------------------------------------------------
// Builder DSL
// ---------------------------------------------------------------------------

/** Scope marker for [LlmQueue] builder DSL. Prevents implicit receiver leaking. */
@DslMarker
@Target(AnnotationTarget.CLASS)
annotation class LlmQueueDsl

/** Builder for [LlmQueue]. Use via [LlmQueue] factory function. */
@LlmQueueDsl
class LlmQueueBuilder {
    /** Maximum queued requests per slot before rejecting. Safety valve. */
    var maxQueueDepth: Int = 50

    private val slotConfigs = mutableMapOf<LlmModelSlot, SlotConfig>()

    /** Configure a model slot. */
    fun slot(slot: LlmModelSlot, block: SlotConfigBuilder.() -> Unit) {
        val builder = SlotConfigBuilder()
        builder.block()
        slotConfigs[slot] = SlotConfig(concurrency = builder.concurrency)
    }

    internal fun build(): LlmQueueConfig = LlmQueueConfig(
        maxQueueDepth = maxQueueDepth,
        slotConfigs = slotConfigs.toMap(),
    )
}

@LlmQueueDsl
class SlotConfigBuilder {
    /** Max concurrent executions for this slot. Default 1. */
    var concurrency: Int = 1
}

internal data class SlotConfig(val concurrency: Int)

internal data class LlmQueueConfig(
    val maxQueueDepth: Int,
    val slotConfigs: Map<LlmModelSlot, SlotConfig>,
) {
    fun concurrencyFor(slot: LlmModelSlot): Int =
        slotConfigs[slot]?.concurrency ?: 1
}

// ---------------------------------------------------------------------------
// Queue entry (type-erased wrapper for the priority queue)
// ---------------------------------------------------------------------------

private class LlmRequest<T>(
    val slot: LlmModelSlot,
    val priority: LlmPriority,
    val lane: LlmLane,
    val description: String,
    val work: suspend () -> T,
) {
    val result: CompletableDeferred<T> = CompletableDeferred()
    val enqueuedAt: TimeMark = TimeSource.Monotonic.markNow()
}

private class QueueEntry(
    private val request: LlmRequest<*>,
) : Comparable<QueueEntry> {
    val priority: LlmPriority = request.priority
    val lane: String = request.lane.label
    val description: String = request.description

    fun waitDuration(): Duration = request.enqueuedAt.elapsedNow()
    fun isCancelled(): Boolean = request.result.isCancelled

    fun cancel() {
        request.result.cancel()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun execute() {
        val req = request as LlmRequest<Any?>
        try {
            val value = req.work()
            req.result.complete(value)
        } catch (e: CancellationException) {
            req.result.cancel(e)
            throw e
        } catch (e: Exception) {
            req.result.completeExceptionally(e)
        }
    }

    override fun compareTo(other: QueueEntry): Int =
        this.priority.ordinal.compareTo(other.priority.ordinal)
}

// ---------------------------------------------------------------------------
// Metrics
// ---------------------------------------------------------------------------

/** Snapshot of queue metrics for observability. */
data class LlmQueueMetrics(
    val totalSubmitted: Long,
    val totalCompleted: Long,
    val totalFailed: Long,
    val queueDepthPerSlot: Map<LlmModelSlot, Int>,
    val activePerSlot: Map<LlmModelSlot, Int>,
)

// ---------------------------------------------------------------------------
// Thrown when the queue is full
// ---------------------------------------------------------------------------

class LlmQueueFullException(
    val slot: LlmModelSlot,
    val currentDepth: Int,
) : RuntimeException("LLM queue full for slot '${slot.value}': $currentDepth requests queued")

// ---------------------------------------------------------------------------
// LlmQueue
// ---------------------------------------------------------------------------

/**
 * Shared, priority-based queue for all LLM inference requests.
 *
 * - One priority queue per [LlmModelSlot] with per-slot concurrency control.
 * - Different model slots proceed in parallel.
 * - Callers suspend on [submit] until their work completes.
 * - Supports cancellation via structured concurrency.
 *
 * Create via the builder DSL: `LlmQueue { slot(Vision) { concurrency = 1 } }`.
 */
class LlmQueue private constructor(private val config: LlmQueueConfig) {

    companion object {
        /** Create an [LlmQueue] with the builder DSL. */
        operator fun invoke(block: LlmQueueBuilder.() -> Unit = {}): LlmQueue {
            val builder = LlmQueueBuilder()
            builder.block()
            return LlmQueue(builder.build())
        }
    }

    private val logger = loggerFor()

    private val scope = CoroutineScope(
        Dispatchers.Default + SupervisorJob() + CoroutineName("LlmQueue")
    )

    private val slotSemaphores: Map<LlmModelSlot, Semaphore> =
        config.slotConfigs.mapValues { (_, cfg) -> Semaphore(cfg.concurrency) }
    private val slotQueues: Map<LlmModelSlot, PriorityBlockingQueue<QueueEntry>> =
        config.slotConfigs.mapValues { PriorityBlockingQueue(16) }
    private val signal = Channel<Unit>(Channel.CONFLATED)

    private val totalSubmitted = AtomicLong(0)
    private val totalCompleted = AtomicLong(0)
    private val totalFailed = AtomicLong(0)
    private val activePerSlot: Map<LlmModelSlot, AtomicInteger> =
        config.slotConfigs.mapValues { AtomicInteger(0) }

    private var dispatcherJob: Job? = null

    /**
     * Submit work to the queue and suspend until completion.
     *
     * The lane's [LlmLane.slot] and [LlmLane.defaultPriority] are used automatically.
     *
     * @param lane The logical lane (determines slot + priority)
     * @param description Human-readable description for logging
     * @param work The suspend function to execute when the slot is available
     * @return The result of [work]
     * @throws CancellationException if the caller's coroutine is cancelled
     * @throws LlmQueueFullException if the queue depth limit is reached
     */
    suspend fun <T> submit(
        lane: LlmLane,
        description: String = "",
        work: suspend () -> T,
    ): T {
        val request = LlmRequest(
            slot = lane.slot,
            priority = lane.defaultPriority,
            lane = lane,
            description = description,
            work = work,
        )

        val queue = slotQueues[lane.slot]
            ?: error("No queue configured for slot '${lane.slot.value}'. Configure it via LlmQueue { slot(...) {} }.")
        val depth = queue.size
        if (depth >= config.maxQueueDepth) {
            throw LlmQueueFullException(lane.slot, depth)
        }

        totalSubmitted.incrementAndGet()
        queue.add(QueueEntry(request))
        signal.trySend(Unit)

        logger.debug(
            "Enqueued LLM request: slot={}, priority={}, lane={}, queueDepth={}, desc={}",
            lane.slot.value, lane.defaultPriority, lane.label, depth + 1, description
        )

        return request.result.await()
    }

    /** Start the queue dispatcher. Call once on application startup. */
    fun start() {
        if (dispatcherJob != null) {
            logger.warn("LlmQueue already started")
            return
        }

        val slotSummary = config.slotConfigs.entries.joinToString { "${it.key.value}=${it.value.concurrency}" }
        logger.info("Starting LlmQueue: slots=[{}], maxQueueDepth={}", slotSummary, config.maxQueueDepth)

        dispatcherJob = scope.launch {
            while (isActive) {
                dispatchReadyRequests()
                withTimeoutOrNull(1.seconds) { signal.receive() }
            }
        }
    }

    /** Stop the queue. Cancels pending requests and waits for in-flight to complete. */
    fun stop() {
        logger.info("Stopping LlmQueue...")
        dispatcherJob?.cancel()
        dispatcherJob = null
        signal.close()
        scope.coroutineContext[Job]?.cancel()
        slotQueues.values.forEach { queue ->
            queue.forEach { it.cancel() }
            queue.clear()
        }
        logger.info("LlmQueue stopped")
    }

    /** Get current queue metrics for observability. */
    fun metrics(): LlmQueueMetrics = LlmQueueMetrics(
        totalSubmitted = totalSubmitted.get(),
        totalCompleted = totalCompleted.get(),
        totalFailed = totalFailed.get(),
        queueDepthPerSlot = slotQueues.mapValues { it.value.size },
        activePerSlot = activePerSlot.mapValues { it.value.get() },
    )

    // =========================================================================
    // Internal dispatcher
    // =========================================================================

    private fun dispatchReadyRequests() {
        for ((slot, queue) in slotQueues) {
            val semaphore = slotSemaphores[slot] ?: continue
            val active = activePerSlot[slot] ?: continue

            while (true) {
                if (!semaphore.tryAcquire()) break

                val entry = queue.poll()
                if (entry == null) {
                    semaphore.release()
                    break
                }

                if (entry.isCancelled()) {
                    semaphore.release()
                    continue
                }

                active.incrementAndGet()
                val waitDuration = entry.waitDuration()

                logger.info(
                    "Dispatching LLM request: slot={}, priority={}, lane={}, waitMs={}, desc={}",
                    slot.value, entry.priority, entry.lane,
                    waitDuration.inWholeMilliseconds, entry.description
                )

                scope.launch {
                    try {
                        entry.execute()
                        totalCompleted.incrementAndGet()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        totalFailed.incrementAndGet()
                        logger.error(
                            "LLM request failed: slot={}, lane={}, desc={}",
                            slot.value, entry.lane, entry.description, e
                        )
                    } finally {
                        active.decrementAndGet()
                        semaphore.release()
                        signal.trySend(Unit)
                    }
                }
            }
        }
    }
}
