package ai.dokus.foundation.domain.config

/**
 * Result of validating a server connection.
 *
 * Used to communicate validation outcomes to the UI layer, allowing appropriate
 * error messages and user guidance based on the specific failure reason.
 */
sealed class ServerValidationResult {
    /**
     * Server is valid and ready for connection.
     *
     * @property serverInfo Details about the validated server
     */
    data class Valid(val serverInfo: ServerInfo) : ServerValidationResult()

    /**
     * Server validation failed.
     *
     * @property reason The specific reason for failure
     */
    data class Invalid(val reason: InvalidReason) : ServerValidationResult()

    /**
     * Specific reasons why server validation can fail.
     */
    enum class InvalidReason {
        /** Could not reach the server (network error, timeout, DNS failure) */
        UNREACHABLE,

        /** Server responded but is not a Dokus server (wrong endpoint format) */
        NOT_DOKUS_SERVER,

        /** Server version is incompatible with this app version */
        INCOMPATIBLE_VERSION,

        /** Server is in maintenance mode */
        MAINTENANCE,

        /** Invalid URL format provided */
        INVALID_URL
    }

    /**
     * Check if validation was successful.
     */
    val isValid: Boolean
        get() = this is Valid

    /**
     * Get the server info if validation was successful, null otherwise.
     */
    val serverInfoOrNull: ServerInfo?
        get() = (this as? Valid)?.serverInfo
}

/**
 * Exception thrown when server connection fails.
 *
 * @property reason The specific reason for the connection failure
 */
class ServerConnectionException(
    val reason: ServerValidationResult.InvalidReason,
    message: String = "Failed to connect to server: $reason"
) : Exception(message) {

    companion object {
        fun unreachable() = ServerConnectionException(
            ServerValidationResult.InvalidReason.UNREACHABLE,
            "Could not reach the server. Please check your connection and try again."
        )

        fun notDokusServer() = ServerConnectionException(
            ServerValidationResult.InvalidReason.NOT_DOKUS_SERVER,
            "The server is not a Dokus server or is not properly configured."
        )

        fun incompatibleVersion() = ServerConnectionException(
            ServerValidationResult.InvalidReason.INCOMPATIBLE_VERSION,
            "The server version is not compatible with this app version."
        )

        fun maintenance() = ServerConnectionException(
            ServerValidationResult.InvalidReason.MAINTENANCE,
            "The server is currently in maintenance mode. Please try again later."
        )

        fun invalidUrl() = ServerConnectionException(
            ServerValidationResult.InvalidReason.INVALID_URL,
            "The provided URL is invalid."
        )
    }
}
