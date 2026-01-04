package tech.dokus.domain.exceptions

/**
 * Converts a Throwable to a DokusException.
 * If the Throwable is already a DokusException, it returns it as-is.
 * Otherwise, it attempts to match the error message to a known exception type,
 * or returns Unknown if no match is found.
 */
val Throwable.asDokusException: DokusException
    get() = when {
        this is DokusException -> this
        else -> {
            val message = this.message?.lowercase()
            when {
                message == null -> DokusException.Unknown(this)
                message.contains("failed to connect to") -> DokusException.ConnectionError(message)
                message.contains("connection refused") -> DokusException.ConnectionError(message)
                message.contains("connection reset") -> DokusException.ConnectionError(message)
                message.contains("could not connect to the server") -> DokusException.ConnectionError(message)
                message.contains("websocket connection") -> DokusException.ConnectionError(message)
                message.contains("network is unreachable") -> DokusException.ConnectionError(message)
                message.contains("io.ktor.serialization.jsonconvertexception") ->
                    DokusException.InternalError("Serialization error")
                else -> DokusException.Unknown(this)
            }
        }
    }

/**
 * Converts a Result to a DokusException.
 * If the Result is successful, it returns InternalError.
 * If the Result is a failure, it converts the exception using asDokusException.
 */
val Result<*>.asDokusException: DokusException
    get() = if (isSuccess) {
        DokusException.InternalError("Result is success. You should not call asDokusException")
    } else {
        exceptionOrNull()?.asDokusException ?: DokusException.Unknown(null)
    }
