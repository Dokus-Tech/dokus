package ai.dokus.foundation.ktor.middleware

import ai.dokus.foundation.ktor.auth.RequestAuthHolder
import ai.dokus.foundation.ktor.security.JwtValidator
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.header
import io.ktor.util.AttributeKey
import org.slf4j.LoggerFactory

/**
 * Plugin that extracts JWT authentication and stores it in RequestAuthHolder for RPC access.
 */
val RpcAuthPlugin = createApplicationPlugin(name = "RpcAuthPlugin") {
    val logger = LoggerFactory.getLogger("RpcAuthPlugin")

    onCall { call ->
        try {
            val jwtValidator =
                call.application.environment.config.propertyOrNull("jwt.secret")?.let {
                    // Get from Koin
                    call.application.attributes.getOrNull(AttributeKey<JwtValidator>("JwtValidator"))
                }

            if (jwtValidator != null) {
                val authHeader = call.request.header("Authorization")
                if (authHeader != null && authHeader.startsWith("Bearer ", ignoreCase = true)) {
                    val token = authHeader.substring(7)
                    val authInfo = jwtValidator.validateAndExtract(token)
                    if (authInfo != null) {
                        logger.debug("Setting auth context for user: ${authInfo.userId.value}")
                        RequestAuthHolder.set(authInfo)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error extracting auth info", e)
        }
    }

    onCallRespond { call, _ ->
        // Clear auth context after request
        RequestAuthHolder.clear()
    }
}