package tech.dokus.foundation.app.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun interface SseEventCollector {
    suspend fun collect(
        httpClient: HttpClient,
        request: HttpRequestBuilder.() -> Unit,
        onConnected: () -> Unit,
        onEvent: suspend (ServerSentEvent) -> Unit,
    )
}

object KtorSseEventCollector : SseEventCollector {
    override suspend fun collect(
        httpClient: HttpClient,
        request: HttpRequestBuilder.() -> Unit,
        onConnected: () -> Unit,
        onEvent: suspend (ServerSentEvent) -> Unit,
    ) {
        httpClient.sse(request = request) {
            onConnected()
            incoming.collect { event ->
                onEvent(event)
            }
        }
    }
}

fun <T> observeSseEvents(
    httpClient: HttpClient,
    request: HttpRequestBuilder.() -> Unit,
    decodeEvent: (ServerSentEvent) -> T?,
    staleTimeout: Duration = defaultSseStaleTimeout,
    sseEventCollector: SseEventCollector = KtorSseEventCollector,
): Flow<T> {
    return restartableFlow(
        staleTimeout = staleTimeout,
        source = {
            flow {
                sseEventCollector.collect(
                    httpClient = httpClient,
                    request = request,
                    onConnected = {},
                    onEvent = { event ->
                        val decodedEvent = decodeEvent(event)
                        if (decodedEvent != null) {
                            emit(decodedEvent)
                        }
                    },
                )
            }
        },
    )
}

val defaultSseStaleTimeout: Duration = 30.seconds
