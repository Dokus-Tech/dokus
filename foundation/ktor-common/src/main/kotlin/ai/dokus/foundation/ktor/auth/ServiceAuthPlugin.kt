@file:OptIn(ExperimentalUuidApi::class)

package ai.dokus.foundation.ktor.auth

import ai.dokus.foundation.domain.model.AuthenticationInfo
import ai.dokus.foundation.ktor.security.JwtValidator
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.header
import io.ktor.util.AttributeKey
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi

/**
 * Attribute key for storing authentication info in call attributes.
 */
val AuthInfoAttributeKey = AttributeKey<AuthenticationInfo>("AuthInfo")

/**
 * Plugin that validates JWT locally and stores auth info for the request.
 *
 * This plugin:
 * 1. Extracts JWT from Authorization header
 * 2. Validates and decodes JWT locally using JwtValidator (from Koin)
 * 3. Stores AuthenticationInfo in both call.attributes and RequestAuthHolder
 *
 * No RPC call is made - validation is entirely local using the shared JWT secret.
 * This is much faster than the old approach that made RPC calls to auth service.
 */
val ServiceAuthPlugin = createApplicationPlugin(name = "ServiceAuthPlugin") {
    val logger = LoggerFactory.getLogger("ServiceAuthPlugin")

    onCall { call ->
        try {
            val jwtValidator = call.application.get<JwtValidator>()

            val authHeader = call.request.header("Authorization")
            if (authHeader != null && authHeader.startsWith("Bearer ", ignoreCase = true)) {
                val token = authHeader.substring(7)
                val authInfo = jwtValidator.validateAndExtract(token)

                if (authInfo != null) {
                    // Store in both call attributes and RequestAuthHolder
                    call.attributes.put(AuthInfoAttributeKey, authInfo)
                    logger.debug(
                        "Auth validated locally for user: {}, tenant: {}",
                        authInfo.userId.value,
                        authInfo.tenantId.value
                    )
                } else {
                    logger.debug("JWT validation failed - no auth context set")
                }
            }
        } catch (e: Exception) {
            logger.error("Error validating JWT", e)
        }
    }

    onCallRespond { _, _ -> }
}
