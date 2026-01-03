package tech.dokus.foundation.app

import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named

object SharedQualifiers {
    val httpClientAuth: Qualifier = named("http_client_auth")
    val httpClientNoAuth: Qualifier = named("http_client_no_auth")
}
