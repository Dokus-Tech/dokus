package tech.dokus.foundation.app.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

fun <T> restartableFlow(
    source: suspend () -> Flow<T>,
    staleTimeout: Duration? = null,
    backoffBase: Duration = 500.milliseconds,
    backoffMax: Duration = 15.seconds,
    onStart: (suspend SendChannel<T>.() -> Unit)? = null,
    onFailure: suspend SendChannel<T>.(Throwable) -> Unit = {},
    onRestart: suspend () -> Unit = {},
): Flow<T> = channelFlow {
    var attempt = 0L
    val timeSource = TimeSource.Monotonic

    while (isActive) {
        onStart?.invoke(this)

        val upstream = runCatching { source() }.getOrElse { failure ->
            onFailure(failure)
            onRestart()
            attempt++
            delay(backoffDelay(attempt, backoffBase, backoffMax))
            continue
        }

        var lastEmissionMark = timeSource.markNow()
        var failure: Throwable? = null

        val collector = launch {
            try {
                upstream.collect { value ->
                    attempt = 0
                    lastEmissionMark = timeSource.markNow()
                    send(value)
                }
                failure = IllegalStateException("Upstream flow completed unexpectedly")
            } catch (t: Throwable) {
                failure = t
            }
        }

        val watchdog = staleTimeout?.let { timeout ->
            launch {
                while (isActive) {
                    delay(timeout)
                    if (lastEmissionMark.elapsedNow() >= timeout) {
                        val timeoutException = CancellationException("Upstream stalled after $timeout")
                        failure = timeoutException
                        collector.cancel(timeoutException)
                        break
                    }
                }
            }
        }

        collector.join()
        watchdog?.cancelAndJoin()
        onRestart()

        if (!isActive) break

        failure?.let { onFailure(it) }
        attempt++
        delay(backoffDelay(attempt, backoffBase, backoffMax))
    }
}

private fun backoffDelay(
    attempt: Long,
    base: Duration,
    max: Duration,
): Long {
    val step = min(attempt.toInt(), 6)
    val delayMs = base.toLong(DurationUnit.MILLISECONDS) * (1L shl step)
    return min(delayMs, max.toLong(DurationUnit.MILLISECONDS))
}
