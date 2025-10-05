package ai.thepredict.repository.utils

import ai.thepredict.app.platform.Logger
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.request

/**
 * Ktor HTTP client plugin for logging requests and responses
 */
internal val LoggingPlugin = createClientPlugin("LoggingPlugin") {
    val logger = Logger.withTag("HttpClient")

    onRequest { request, _ ->
        logger.d { "→ ${request.method.value} ${request.url}" }
    }

    onResponse { response ->
        val request = response.request
        val status = response.status

        when {
            status.value in 200..299 -> {
                logger.i { "← ${request.method.value} ${request.url} [${status.value}] Success" }
            }
            status.value in 400..499 -> {
                logger.w { "← ${request.method.value} ${request.url} [${status.value}] Client error" }
            }
            status.value in 500..599 -> {
                logger.e { "← ${request.method.value} ${request.url} [${status.value}] Server error" }
            }
            else -> {
                logger.d { "← ${request.method.value} ${request.url} [${status.value}]" }
            }
        }
    }
}
