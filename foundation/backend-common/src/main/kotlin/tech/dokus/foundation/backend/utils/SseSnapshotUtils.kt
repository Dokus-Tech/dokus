package tech.dokus.foundation.backend.utils

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.sse.SSEServerContent
import io.ktor.server.sse.ServerSSESession
import io.ktor.sse.ServerSentEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

suspend fun ApplicationCall.respondSse(
    handler: suspend ServerSSESession.() -> Unit,
) {
    response.header("X-Accel-Buffering", "no")
    response.header(HttpHeaders.Connection, "keep-alive")
    respond(SSEServerContent(this, handler))
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

val defaultSseHeartbeatPeriod: Duration = 15.seconds
