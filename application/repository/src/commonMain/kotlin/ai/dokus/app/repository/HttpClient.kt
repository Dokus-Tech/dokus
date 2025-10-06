package ai.dokus.app.repository

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

expect fun httpClient(config: HttpClientConfig<*>.() -> Unit = {}): HttpClient