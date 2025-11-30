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
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.decodeFromString
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

            // Attempt to parse a structured DokusException only when JSON is present.
            val isJson = response.contentType()?.match(ContentType.Application.Json) == true
            val bodyText = runCatching { response.bodyAsText() }.getOrNull()
            if (isJson && bodyText != null) {
                runCatching { Json { ignoreUnknownKeys = true }.decodeFromString(DokusException.serializer(), bodyText) }
                    .onSuccess { throw it }
            }

            // Fallback: map common HTTP codes; include body text for diagnostics.
            when (response.status) {
                HttpStatusCode.Unauthorized -> {
                    onUnauthorized()
                    throw DokusException.NotAuthenticated()
                }

                HttpStatusCode.Forbidden -> throw DokusException.NotAuthorized()
                HttpStatusCode.NotFound -> throw DokusException.InternalError(bodyText ?: "Resource not found")
                else -> throw DokusException.Unknown(IllegalStateException("HTTP ${response.status.value}: ${bodyText.orEmpty()}"))
            }
        }
    }
}

fun HttpClientConfig<*>.withLogging(logLevel: LogLevel = LogLevel.ALL) {
    install(Logging) {
        logger = Logger.SIMPLE
        level = logLevel
    }
}
