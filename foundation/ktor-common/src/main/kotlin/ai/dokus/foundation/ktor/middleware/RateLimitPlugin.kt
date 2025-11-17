package ai.dokus.foundation.ktor.middleware

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.request.header

val RateLimitPlugin = createRouteScopedPlugin(
    name = "RateLimitPlugin",
    createConfiguration = ::RateLimitConfiguration
) {
}

class RateLimitConfiguration

private fun extractClientIpAddress(call: ApplicationCall): String {
    return call.request.header("X-Forwarded-For")
        ?.split(",")?.firstOrNull()?.trim()
        ?: call.request.header("X-Real-IP")
        ?: call.request.local.remoteHost
}