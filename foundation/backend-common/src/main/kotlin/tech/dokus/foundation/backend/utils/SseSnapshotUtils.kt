package tech.dokus.foundation.backend.utils

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.sse.SSEServerContent
import io.ktor.server.sse.ServerSSESession
import io.ktor.server.sse.heartbeat
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = loggerFor("SseSnapshotUtils")

suspend fun ApplicationCall.respondSse(
    handler: suspend ServerSSESession.() -> Unit,
) {
    response.header("X-Accel-Buffering", "no")
    response.header(HttpHeaders.Connection, "keep-alive")
    respond(SSEServerContent(this, handler))
}

suspend fun <T> ApplicationCall.respondSnapshotSse(
    updates: Flow<*>,
    event: String,
    snapshot: suspend () -> T,
    encode: (T) -> String,
    heartbeatPeriod: Duration = defaultSseHeartbeatPeriod,
) {
    respondSse {
        heartbeat {
            period = heartbeatPeriod
        }
        sendJsonEvent(event = event, payload = snapshot(), encode = encode)
        updates.collect {
            try {
                sendJsonEvent(event = event, payload = snapshot(), encode = encode)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Failed to load SSE snapshot for event {}", event, e)
            }
        }
    }
}

suspend fun <T> ServerSSESession.sendJsonEvent(
    event: String,
    payload: T,
    encode: (T) -> String,
) {
    send(
        ServerSentEvent(
            event = event,
            data = encode(payload),
        )
    )
}

suspend fun ServerSSESession.sendEvent(
    event: String,
    data: String? = null,
) {
    send(
        ServerSentEvent(
            event = event,
            data = data,
        )
    )
}

val defaultSseHeartbeatPeriod: Duration = 15.seconds
