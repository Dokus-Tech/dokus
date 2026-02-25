package tech.dokus.peppol.service

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import tech.dokus.peppol.provider.client.RecommandApiException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Stable classification for outbound provider failures.
 * Codes are persisted and used by retry logic + operator diagnostics.
 */
data class OutboundFailureClassification(
    val retryable: Boolean,
    val errorCode: String,
    val humanMessage: String
)

class PeppolOutboundErrorClassifier {

    fun classify(throwable: Throwable): OutboundFailureClassification {
        val root = throwable.unwrap()

        return when (root) {
            is RecommandApiException -> classifyRecommand(root)
            is HttpRequestTimeoutException,
            is SocketTimeoutException -> OutboundFailureClassification(
                retryable = true,
                errorCode = "NETWORK_TIMEOUT",
                humanMessage = "Provider request timed out"
            )

            is IOException -> OutboundFailureClassification(
                retryable = true,
                errorCode = "NETWORK_IO",
                humanMessage = "Temporary network error while sending"
            )

            is IllegalArgumentException -> OutboundFailureClassification(
                retryable = false,
                errorCode = "VALIDATION_ERROR",
                humanMessage = root.message ?: "Outbound validation failed"
            )

            else -> OutboundFailureClassification(
                retryable = true,
                errorCode = "UNEXPECTED_ERROR",
                humanMessage = root.message ?: "Unexpected outbound processing error"
            )
        }
    }

    private fun classifyRecommand(exception: RecommandApiException): OutboundFailureClassification {
        return when (exception.statusCode) {
            429 -> OutboundFailureClassification(
                retryable = true,
                errorCode = "PROVIDER_RATE_LIMIT",
                humanMessage = "Provider rate limit reached"
            )

            in 500..599 -> OutboundFailureClassification(
                retryable = true,
                errorCode = "PROVIDER_SERVER_ERROR",
                humanMessage = "Provider temporary server error"
            )

            400 -> OutboundFailureClassification(
                retryable = false,
                errorCode = "PROVIDER_BAD_REQUEST",
                humanMessage = "Provider rejected the document payload"
            )

            401 -> OutboundFailureClassification(
                retryable = false,
                errorCode = "PROVIDER_UNAUTHORIZED",
                humanMessage = "Provider credentials are invalid"
            )

            403 -> OutboundFailureClassification(
                retryable = false,
                errorCode = "PROVIDER_FORBIDDEN",
                humanMessage = "Provider denied access for this tenant"
            )

            404 -> OutboundFailureClassification(
                retryable = false,
                errorCode = "PROVIDER_NOT_FOUND",
                humanMessage = "Provider endpoint or resource not found"
            )

            422 -> OutboundFailureClassification(
                retryable = false,
                errorCode = "PROVIDER_UNPROCESSABLE",
                humanMessage = "Provider validation failed for this document"
            )

            else -> OutboundFailureClassification(
                retryable = false,
                errorCode = "PROVIDER_HTTP_${exception.statusCode}",
                humanMessage = "Provider rejected the outbound request"
            )
        }
    }

    private fun Throwable.unwrap(): Throwable {
        var current: Throwable = this
        while (current.cause != null &&
            current.cause !== current &&
            current is ResponseException
        ) {
            current = current.cause ?: break
        }
        return current
    }
}
