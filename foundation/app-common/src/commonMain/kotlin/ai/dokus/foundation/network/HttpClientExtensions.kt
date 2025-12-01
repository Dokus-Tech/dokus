package ai.dokus.foundation.network

import ai.dokus.foundation.domain.config.DokusEndpoint
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.exceptions.NotFound
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
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

fun HttpClientConfig<*>.withDokusEndpoint(endpoint: DokusEndpoint) {
    defaultRequest {
        host = endpoint.host
        port = endpoint.port
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
                            throw DokusException.Unauthorized()
                        }

                        HttpStatusCode.Forbidden -> {
                            throw DokusException.Forbidden()
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
