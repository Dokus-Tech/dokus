package ai.dokus.foundation.network

import ai.dokus.foundation.domain.config.DokusEndpoint
import ai.dokus.foundation.domain.exceptions.DokusException
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun HttpClientConfig<*>.withJsonContentNegotiation() {
    install(ContentNegotiation) {
        json(Json {
            encodeDefaults = true
            isLenient = true
            coerceInputValues = true
            ignoreUnknownKeys = true
        })
    }
}

fun HttpClientConfig<*>.withDokusEndpoint(endpoint: DokusEndpoint) {
    defaultRequest {
        host = endpoint.host
        port = endpoint.port
    }
}

fun HttpClientConfig<*>.withResponseValidation(onUnauthorized: suspend () -> Unit = {}) {
    HttpResponseValidator {
        validateResponse { response: HttpResponse ->
            // Skip validation for WebSocket upgrade responses (101 Switching Protocols)
            if (response.status == HttpStatusCode.SwitchingProtocols) return@validateResponse

            if (response.status.isSuccess()) return@validateResponse
            runCatching { response.body<DokusException>() }.fold(
                onSuccess = { throw it },
                onFailure = {
                    when (response.status) {
                        HttpStatusCode.Unauthorized -> {
                            onUnauthorized()
                            throw DokusException.NotAuthenticated()
                        }

                        HttpStatusCode.Forbidden -> {
                            throw DokusException.NotAuthorized()
                        }

                        HttpStatusCode.NotFound -> throw DokusException.Unknown(it)

                        else -> throw DokusException.Unknown(it)
                    }
                }
            )
        }
    }
}

fun HttpClientConfig<*>.withLogging(logLevel: LogLevel = LogLevel.ALL) {
    install(Logging) {
        logger = Logger.SIMPLE
        level = logLevel
    }
}
