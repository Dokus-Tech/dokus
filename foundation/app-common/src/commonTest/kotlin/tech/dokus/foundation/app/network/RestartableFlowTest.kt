package tech.dokus.foundation.app.network

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class RestartableFlowTest {

    @Test
    fun `restartableFlow restarts after upstream failure`() = runTest {
        var attempts = 0

        val values = restartableFlow(
            source = {
                val attempt = attempts++
                flow {
                    emit(attempt)
                    if (attempt == 0) {
                        error("boom")
                    }
                    awaitCancellation()
                }
            },
            backoffBase = 1.milliseconds,
            backoffMax = 2.milliseconds,
        ).take(2).toList()

        assertEquals(listOf(0, 1), values)
        assertEquals(2, attempts)
    }

    @Test
    fun `restartableFlow restarts stalled upstream`() = runTest {
        var attempts = 0

        val value = restartableFlow(
            source = {
                when (attempts++) {
                    0 -> flow { awaitCancellation() }
                    else -> flow {
                        emit(42)
                        awaitCancellation()
                    }
                }
            },
            staleTimeout = 10.milliseconds,
            backoffBase = 1.milliseconds,
            backoffMax = 2.milliseconds,
        ).first()

        assertEquals(42, value)
        assertEquals(2, attempts)
    }
}
