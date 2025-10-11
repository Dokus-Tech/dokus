package ai.dokus.auth.backend.middleware

import ai.dokus.auth.backend.service.RateLimitService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import org.koin.ktor.ext.inject

val RateLimitPlugin = createRouteScopedPlugin(
    name = "RateLimitPlugin",
    createConfiguration = ::RateLimitConfiguration
) {
    val rateLimitService by application.inject<RateLimitService>()

    onCall { call ->
        val ipAddress = extractClientIpAddress(call)

        try {
            rateLimitService.checkRateLimit(ipAddress, isIpAddress = true)
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.TooManyRequests,
                mapOf("error" to "Too many requests. Please try again later.")
            )
        }
    }
}

class RateLimitConfiguration

private fun extractClientIpAddress(call: ApplicationCall): String {
    return call.request.header("X-Forwarded-For")
        ?.split(",")?.firstOrNull()?.trim()
        ?: call.request.header("X-Real-IP")
        ?: call.request.local.remoteHost
}