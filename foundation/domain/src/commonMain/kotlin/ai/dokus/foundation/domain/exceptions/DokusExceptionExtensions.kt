package ai.dokus.foundation.domain.exceptions

/**
 * Converts a Throwable to a DokusException.
 * If the Throwable is already a DokusException, it returns it as-is.
 * Otherwise, it attempts to match the error message to a known exception type,
 * or returns Unknown if no match is found.
 */
val Throwable?.asDokusException: DokusException
    get() {
        val message = this?.message?.lowercase()
        return when (this) {
            is DokusException -> this
            null -> DokusException.Unknown(this)
            else -> {
                if (message == null) return DokusException.Unknown(this)

                if (message.contains("failed to connect to")) DokusException.ConnectionError(message)
                else if (message.contains("connection refused")) DokusException.ConnectionError(message)
                else if (message.contains("connection reset")) DokusException.ConnectionError(message)
                else if (message.contains("could not connect to the server")) DokusException.ConnectionError(message)
                else if (message.contains("websocket connection")) DokusException.ConnectionError(message)
                else if (message.contains("network is unreachable")) DokusException.ConnectionError(message)
                else if (message.contains("io.ktor.serialization.jsonconvertexception")) DokusException.InternalError("Serialization error")
                else DokusException.Unknown(this)
            }
        }
    }

/**
 * Converts a Result to a DokusException.
 * If the Result is successful, it returns InternalError.
 * If the Result is a failure, it converts the exception using asDokusException.
 */
val Result<*>.asDokusException: DokusException
    get() {
        return if (isSuccess) {
            DokusException.InternalError("Result is success. You should not call asDokusException")
        } else {
            exceptionOrNull().asDokusException
        }
    }
