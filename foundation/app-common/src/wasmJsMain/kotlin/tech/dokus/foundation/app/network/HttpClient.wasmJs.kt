package tech.dokus.foundation.app.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

internal actual fun createDokusHttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(block)
