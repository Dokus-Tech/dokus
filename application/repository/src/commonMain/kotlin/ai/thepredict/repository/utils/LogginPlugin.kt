package ai.thepredict.repository.utils

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.request

internal val LoggingPlugin = createClientPlugin("LoggingPlugin") {
    onRequest { request, _ ->
        println("Request: ${request.url} ${request.method.value}")
        println("Headers: ${request.headers.entries()}")
    }
    onResponse { response ->
        println("Response: ${response.status.value} for ${response.request.url}")
        println("Headers: ${response.headers.entries()}")
    }
}