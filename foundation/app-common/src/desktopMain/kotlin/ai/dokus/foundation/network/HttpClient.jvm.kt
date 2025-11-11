package ai.dokus.foundation.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO

internal actual fun createDokusHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(CIO, block)
