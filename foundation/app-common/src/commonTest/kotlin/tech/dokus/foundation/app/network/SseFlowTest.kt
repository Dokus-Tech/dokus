package tech.dokus.foundation.app.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class SseFlowTest {

    @Test
    fun `observeSseEvents treats heartbeat frames as activity`() = runTest {
        var connectionAttempts = 0
        val httpClient = HttpClient(MockEngine { error("Unexpected HTTP request in SseFlowTest") })

        val payload = observeSseEvents(
            httpClient = httpClient,
            request = {},
            decodeEvent = { event -> event.data },
            staleTimeout = 10.milliseconds,
            sseEventCollector = object : SseEventCollector {
                override suspend fun collect(
                    httpClient: HttpClient,
                    request: io.ktor.client.request.HttpRequestBuilder.() -> Unit,
                    onConnected: () -> Unit,
                    onEvent: suspend (ServerSentEvent) -> Unit,
                ) {
                    connectionAttempts++
                    onConnected()

                    repeat(4) {
                        delay(5.milliseconds)
                        onEvent(ServerSentEvent(comments = "heartbeat"))
                    }

                    delay(5.milliseconds)
                    onEvent(ServerSentEvent(event = "message", data = "payload"))
                    awaitCancellation()
                }
            },
        ).first()

        assertEquals("payload", payload)
        assertEquals(1, connectionAttempts)
        httpClient.close()
    }
}
