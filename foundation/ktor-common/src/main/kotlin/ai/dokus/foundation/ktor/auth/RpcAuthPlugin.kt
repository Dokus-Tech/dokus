package ai.dokus.foundation.ktor.auth

import ai.dokus.foundation.domain.model.AuthenticationInfo
import ai.dokus.foundation.domain.rpc.AuthValidationRemoteService
import ai.dokus.foundation.ktor.security.RequestAuthHolder
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.header
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi

/**
 * Creates an RPC authentication plugin that validates JWT tokens via AuthValidationRemoteService.
 *
 * This plugin extracts JWT from Authorization header, validates it by calling the Auth service via RPC,
 * and stores the authentication information in RequestAuthHolder for the duration of the request.
 *
 * @param validationService RPC client for Auth service
 * @param sourceModule Name of the calling module (e.g., "Cashflow", "Payment")
 * @return Ktor plugin configuration
 */
@OptIn(ExperimentalUuidApi::class)
fun createRpcAuthPlugin(
    validationService: AuthValidationRemoteService,
    sourceModule: String
) = createApplicationPlugin(name = "RpcAuthPlugin") {
    val logger = LoggerFactory.getLogger("RpcAuthPlugin")

    onCall { call ->
        try {
            val authHeader = call.request.header("Authorization")
            if (authHeader != null && authHeader.startsWith("Bearer ", ignoreCase = true)) {
                val token = authHeader.substring(7)

                // Validate token via Auth service RPC
                val userDto = validationService.validateSession(
                    token = token,
                    requestContext = AuthValidationRemoteService.Context(
                        sourceModule = sourceModule,
                        ipAddress = call.request.header("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
                            ?: call.request.header("X-Real-IP")
                            ?: call.request.local.remoteHost,
                        userAgent = call.request.header("User-Agent")
                    )
                )

                // Convert UserDto to AuthenticationInfo
                val authInfo = AuthenticationInfo(
                    userId = userDto.id,
                    email = userDto.email.value,
                    name = userDto.fullName,
                    tenantId = userDto.tenantId,
                    roles = setOf(userDto.role.dbValue)
                )

                logger.debug(
                    "Setting auth context for user: {}, tenant: {}",
                    authInfo.userId.value,
                    authInfo.tenantId.value
                )
                RequestAuthHolder.set(authInfo)
            }
        } catch (e: Exception) {
            logger.error("Error validating auth token via RPC", e)
            // Don't set auth context if validation fails
            // The endpoint will handle unauthorized access
        }
    }

    onCallRespond { call, _ ->
        // Clear auth context after request
        RequestAuthHolder.clear()
    }
}
