package tech.dokus.foundation.app.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
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
                        emit(SseObservation.Activity)

                        val decodedEvent = decodeEvent(event)
                        if (decodedEvent != null) {
                            emit(SseObservation.Value(decodedEvent))
                        }
                    },
                )
            }
        },
    ).mapNotNull { observation ->
        when (observation) {
            SseObservation.Activity -> null
            is SseObservation.Value -> observation.value
        }
    }
}

val defaultSseStaleTimeout: Duration = 30.seconds

private sealed interface SseObservation<out T> {
    data object Activity : SseObservation<Nothing>
    data class Value<T>(val value: T) : SseObservation<T>
}
