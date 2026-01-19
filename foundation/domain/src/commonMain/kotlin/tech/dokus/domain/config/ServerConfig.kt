@file:Suppress(
    "ReturnCount" // URL parsing requires multiple early returns for validation
)

package tech.dokus.domain.config

import kotlinx.serialization.Serializable

// Network port constants
private const val HttpsDefaultPort = 443
private const val HttpDefaultPort = 80
private const val PortRangeMin = 1
private const val PortRangeMax = 65535

// Parsing constants
private const val ParamSplitLimit = 2

/**
 * Configuration for connecting to a Dokus server.
 *
 * Supports both cloud (api.dokus.tech) and self-hosted instances.
 * This config is persisted and loaded at runtime, allowing users to switch
 * between servers without recompiling the app.
 *
 * @property host Server hostname or IP address
 * @property port Server port number
 * @property protocol Connection protocol ("http" or "https")
 * @property name Display name for the server (fetched from server info)
 * @property version Server version (fetched from server info)
 * @property isCloud Whether this is the official Dokus cloud server
 */
@Serializable
data class ServerConfig(
    val host: String,
    val port: Int,
    val protocol: String,
    val name: String? = null,
    val version: String? = null,
    val isCloud: Boolean = false
) {
    /**
     * Full base URL for API requests.
     * Examples:
     * - "https://app.dokus.tech:443"
     * - "http://192.168.1.100:8000"
     */
    val baseUrl: String
        get() = when (protocol) {
            "https" if port == HttpsDefaultPort -> "$protocol://$host"
            // Standard HTTP port doesn't need explicit port
            "http" if port == HttpDefaultPort -> "$protocol://$host"
            // Non-standard ports need explicit port
            else -> "$protocol://$host:$port"
        }

    companion object {
        /**
         * Default cloud server configuration.
         * Used when the app is first installed or when reset to default.
         */
        val Cloud = ServerConfig(
            host = "dokus.invoid.vision",
            port = HttpsDefaultPort,
            protocol = "https",
            name = "Dokus Cloud",
            isCloud = true
        )

        /**
         * Parse a server config from a deep link URL.
         *
         * Expected format: dokus://connect?host=...&port=...&protocol=...
         *
         * The deep link is scanned via device camera and opens the Dokus app directly.
         *
         * @param url The deep link URL to parse
         * @return ServerConfig if valid, null if invalid or missing required parameters
         */
        @Suppress("TooGenericExceptionCaught", "SwallowedException") // URL parsing can throw various exceptions
        fun fromDeepLink(url: String): ServerConfig? {
            return try {
                // Parse query parameters
                val queryStart = url.indexOf('?')
                if (queryStart == -1) return null

                val queryString = url.substring(queryStart + 1)
                val params = queryString.split('&')
                    .mapNotNull { param ->
                        val parts = param.split('=', limit = ParamSplitLimit)
                        if (parts.size == ParamSplitLimit) parts[0] to parts[1] else null
                    }
                    .toMap()

                val host = params["host"] ?: return null
                val port = params["port"]?.toIntOrNull() ?: return null
                val protocol = params["protocol"] ?: "http"
                val name = params["name"]

                // Validate protocol
                if (protocol !in listOf("http", "https")) return null

                // Validate port range
                if (port !in PortRangeMin..PortRangeMax) return null

                ServerConfig(
                    host = host,
                    port = port,
                    protocol = protocol,
                    name = name,
                    isCloud = false
                )
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Create a server config from manually entered values.
         *
         * @param host Server hostname or IP address
         * @param port Server port number
         * @param protocol Connection protocol ("http" or "https")
         * @return ServerConfig with the specified values
         */
        fun fromManualEntry(host: String, port: Int, protocol: String): ServerConfig {
            return ServerConfig(
                host = host.trim(),
                port = port,
                protocol = protocol.lowercase().trim(),
                isCloud = false
            )
        }
    }
}

/**
 * Extension function to generate a deep link URL for a server config.
 *
 * Uses the dokus:// custom URL scheme so the device camera can open
 * the Dokus app directly when the QR code is scanned.
 */
fun ServerConfig.toDeepLink(): String {
    val nameParam = name?.let { "&name=$it" } ?: ""
    return "dokus://connect?host=$host&port=$port&protocol=$protocol$nameParam"
}
