package ai.dokus.features.auth.backend.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class ApiKey(
    val key: String,
    val serviceName: String,
    val permissions: Set<String> = emptySet(),
    val isActive: Boolean = true
)

data class ApiPrincipal(
    val serviceName: String,
    val permissions: Set<String>
)

class ApiKeyAuthenticationProvider(
    configuration: Configuration
) : AuthenticationProvider(configuration) {

    private val authenticationFunction = configuration.authenticationFunction

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val apiKey = call.request.header("X-API-Key")
            ?: call.request.queryParameters["api_key"]

        if (apiKey == null) {
            context.challenge("ApiKeyAuth", AuthenticationFailedCause.NoCredentials) { challenge, call ->
                call.respond(HttpStatusCode.Unauthorized, "API key required")
                challenge.complete()
            }
            return
        }

        val principal = authenticationFunction(call, apiKey)
        if (principal != null) {
            context.principal(principal)
        } else {
            context.challenge("ApiKeyAuth", AuthenticationFailedCause.InvalidCredentials) { challenge, call ->
                call.respond(HttpStatusCode.Unauthorized, "Invalid API key")
                challenge.complete()
            }
        }
    }

    class Configuration(name: String) : Config(name) {
        internal var authenticationFunction: suspend (ApplicationCall, String) -> ApiPrincipal? = { _, _ -> null }

        fun validate(body: suspend (ApplicationCall, String) -> ApiPrincipal?) {
            authenticationFunction = body
        }
    }
}

fun AuthenticationConfig.apiKey(
    name: String? = null,
    configure: ApiKeyAuthenticationProvider.Configuration.() -> Unit
) {
    val provider = ApiKeyAuthenticationProvider(
        ApiKeyAuthenticationProvider.Configuration(name ?: "api-key").apply(configure)
    )
    register(provider)
}

object ApiKeyService {
    private val logger = LoggerFactory.getLogger(ApiKeyService::class.java)
    private val apiKeys = ConcurrentHashMap<String, ApiKey>()

    init {
        // Initialize with service API keys from config/env
        // In production, these should be loaded from secure storage
        val defaultKeys = listOf(
            ApiKey(
                key = hashApiKey(System.getenv("MONITORING_API_KEY") ?: "monitoring-secret-key"),
                serviceName = "monitoring-service",
                permissions = setOf("metrics.read", "health.read")
            ),
            ApiKey(
                key = hashApiKey(System.getenv("ADMIN_API_KEY") ?: "admin-secret-key"),
                serviceName = "admin-service",
                permissions = setOf("users.read", "users.write", "sessions.manage")
            ),
            ApiKey(
                key = hashApiKey(System.getenv("INTEGRATION_API_KEY") ?: "integration-secret-key"),
                serviceName = "integration-service",
                permissions = setOf("users.read", "auth.validate")
            )
        )

        defaultKeys.forEach { apiKey ->
            apiKeys[apiKey.key] = apiKey
        }
    }

    fun validateApiKey(key: String): ApiPrincipal? {
        val hashedKey = hashApiKey(key)
        val apiKey = apiKeys[hashedKey]

        return if (apiKey != null && apiKey.isActive) {
            logger.info("API key authenticated for service: ${apiKey.serviceName}")
            ApiPrincipal(apiKey.serviceName, apiKey.permissions)
        } else {
            logger.warn("Invalid or inactive API key attempted")
            null
        }
    }

    fun addApiKey(apiKey: ApiKey) {
        apiKeys[apiKey.key] = apiKey
    }

    fun revokeApiKey(key: String) {
        apiKeys.remove(hashApiKey(key))
    }

    fun deactivateApiKey(key: String) {
        val hashedKey = hashApiKey(key)
        apiKeys[hashedKey]?.let {
            apiKeys[hashedKey] = it.copy(isActive = false)
        }
    }

    private fun hashApiKey(key: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}