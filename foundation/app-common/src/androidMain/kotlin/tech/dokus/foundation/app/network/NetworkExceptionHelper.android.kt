package tech.dokus.foundation.app.network

import io.ktor.client.network.sockets.ConnectTimeoutException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Android implementation of network exception detection.
 * Handles java.net.* and javax.net.ssl.* exceptions.
 */
actual fun isNetworkException(throwable: Throwable): Boolean {
    return when (throwable) {
        is ConnectException -> true
        is ConnectTimeoutException -> true
        is SocketTimeoutException -> true
        is UnknownHostException -> true
        is NoRouteToHostException -> true
        is SocketException -> true
        is SSLException -> true
        else -> {
            // Check cause chain
            throwable.cause?.let { isNetworkException(it) }
                ?: hasNetworkExceptionMessage(throwable)
        }
    }
}
