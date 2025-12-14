package ai.dokus.foundation.network

import ai.dokus.foundation.domain.config.DokusEndpoint
import ai.dokus.foundation.domain.exceptions.DokusException
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
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

/**
 * Configures the HTTP client to use the gateway endpoint with path prefix.
 * All requests go through the Traefik gateway, with the path prefix prepended.
 */
fun HttpClientConfig<*>.withDokusEndpoint(endpoint: DokusEndpoint) {
    defaultRequest {
        host = endpoint.gatewayHost
        port = endpoint.gatewayPort
        url {
            protocol = when (endpoint.gatewayProtocol) {
                "https" -> URLProtocol.HTTPS
                else -> URLProtocol.HTTP
            }
            // Path prefix is applied in routes via type-safe resources
        }
    }
}

fun HttpClientConfig<*>.withResponseValidation(onUnauthorized: suspend () -> Unit = {}) {
    HttpResponseValidator {
        validateResponse { response: HttpResponse ->
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

                        HttpStatusCode.NotFound -> throw DokusException.NotFound()

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

fun HttpClientConfig<*>.withResources() {
    install(Resources)
}
