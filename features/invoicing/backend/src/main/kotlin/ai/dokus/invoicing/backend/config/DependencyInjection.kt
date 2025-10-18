package ai.dokus.invoicing.backend.config

import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin

fun Application.configureDependencyInjection() {
    install(Koin) {
        modules(rpcClientModule)
    }
}
