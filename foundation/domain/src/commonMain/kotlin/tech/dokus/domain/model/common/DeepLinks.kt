@file:Suppress("ReturnCount") // Deep link parsing requires multiple early returns for validation

package tech.dokus.domain.model.common

import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.model.common.DeepLink.Companion.APP_SCHEME
import tech.dokus.domain.model.common.DeepLink.Companion.HTTP_SCHEME
import tech.dokus.domain.model.common.DeepLink.Companion.HTTPS_SCHEME
import kotlin.jvm.JvmInline

/**
 * Simplify and reduce possible mistakes with deep links by having a universal [DeepLink] type
 * Path is always a clear path without any schemas, but with query params
 */
@Serializable
@JvmInline
value class DeepLink(val value: String) {
    val path: String
        get() = value
            .replace("$APP_SCHEME://", "")
            .replace("$HTTP_SCHEME://", "")
            .replace("$HTTPS_SCHEME://", "")

    val withAppScheme: String
        get() = if (path.startsWith(APP_SCHEME)) path else "$APP_SCHEME://$path"

    val withHttpsScheme: String
        get() = if (path.startsWith(HTTPS_SCHEME)) path else "$HTTPS_SCHEME://$path"

    override fun toString(): String = path

    companion object {
        const val APP_SCHEME = "dokus"
        const val HTTP_SCHEME = "http"
        const val HTTPS_SCHEME = "https"

        /**
         * Builds the string.
         * If for some reason [APP_SCHEME], [HTTP_SCHEME], or [HTTPS_SCHEME] is included, it will be removed.
         * Use [withHttpsScheme] or [withAppScheme] if needed
         */
        fun build(builder: StringBuilder.() -> Unit): DeepLink {
            val string = buildString(builder)
                .replace("$APP_SCHEME://", "")
                .replace("$HTTP_SCHEME://", "")
                .replace("$HTTPS_SCHEME://", "")
            return DeepLink(string)
        }
    }
}

enum class KnownDeepLinks(val path: DeepLink, val pattern: DeepLink) {
    QrDecision(DeepLink("auth/qr/decision"), DeepLink("auth/qr/decision?s={sessionId}&t={token}")),
    ServerConnect(DeepLink("connect"), DeepLink("connect?host={host}&port={port}&protocol={protocol}")),
    ShareImport(DeepLink("share/import"), DeepLink("share/import?batch={batchId}")),
    AuthResetPassword(
        DeepLink("auth/reset-password"),
        DeepLink("auth/reset-password?token={token}")
    ),
    AuthVerifyEmail(
        DeepLink("auth/verify-email"),
        DeepLink("auth/verify-email?token={token}")
    ),
    DocumentReview(
        DeepLink("documents/review"),
        DeepLink("documents/review?documentId={documentId}")
    ),
}

object DeepLinks {
    fun buildQrLogin(sessionId: SessionId, token: String): DeepLink {
        return DeepLink.build {
            append(KnownDeepLinks.QrDecision.path)
            append("?s=")
            append(sessionId)
            append("&t=")
            append(token)
        }
    }

    fun extractQrLogin(deepLink: DeepLink): Pair<SessionId, String>? {
        val path = normalizeRoutePath(deepLink.path)
        if (!path.startsWith(KnownDeepLinks.QrDecision.path.path)) return null
        val query = path.substringAfter("?")
        val params = query.split("&")
        val sessionId =
            params.firstOrNull { it.startsWith("s=") }?.substringAfter("s=") ?: return null
        val token = params.firstOrNull { it.startsWith("t=") }?.substringAfter("t=") ?: return null
        return SessionId(sessionId) to token
    }

    /**
     * Builds a server connect deep link with host, port, and protocol parameters.
     */
    fun buildServerConnect(host: String, port: Int, protocol: String): DeepLink {
        return DeepLink.build {
            append(KnownDeepLinks.ServerConnect.path)
            append("?host=")
            append(host)
            append("&port=")
            append(port)
            append("&protocol=")
            append(protocol)
        }
    }

    /**
     * Extracts server connection parameters from a deep link.
     * Returns a Triple of (host, port, protocol) or null if invalid.
     */
    fun extractServerConnect(deepLink: DeepLink): Triple<String, Int, String>? {
        val path = normalizeRoutePath(deepLink.path)
        if (!path.startsWith(KnownDeepLinks.ServerConnect.path.path)) return null
        val query = path.substringAfter("?", "")
        if (query.isEmpty()) return null
        val params = query.split("&")
        val host = params.firstOrNull { it.startsWith("host=") }?.substringAfter("host=") ?: return null
        val portStr = params.firstOrNull { it.startsWith("port=") }?.substringAfter("port=") ?: return null
        val port = portStr.toIntOrNull() ?: return null
        val protocol = params.firstOrNull { it.startsWith("protocol=") }?.substringAfter("protocol=") ?: "https"
        return Triple(host, port, protocol)
    }

    /**
     * Extracts an optional iOS share-import batch ID from a deep link.
     *
     * Example: dokus://share/import?batch=abc123
     */
    fun extractShareImportBatchId(deepLink: DeepLink): String? {
        val path = normalizeRoutePath(deepLink.path)
        if (!path.startsWith(KnownDeepLinks.ShareImport.path.path)) return null
        val query = path.substringAfter("?", "")
        if (query.isEmpty()) return null
        val params = query.split("&")
        return params
            .firstOrNull { it.startsWith("batch=") }
            ?.substringAfter("batch=")
            ?.takeIf { it.isNotBlank() }
    }

    fun extractResetPasswordToken(deepLink: DeepLink): String? {
        val path = normalizeRoutePath(deepLink.path)
        if (!path.startsWith(KnownDeepLinks.AuthResetPassword.path.path)) return null
        return extractQueryParam(path, "token")
    }

    fun extractVerifyEmailToken(deepLink: DeepLink): String? {
        val path = normalizeRoutePath(deepLink.path)
        if (!path.startsWith(KnownDeepLinks.AuthVerifyEmail.path.path)) return null
        return extractQueryParam(path, "token")
    }

    fun extractDocumentReviewId(deepLink: DeepLink): String? {
        val path = normalizeRoutePath(deepLink.path)
        val matchesKnownPath = path.startsWith(KnownDeepLinks.DocumentReview.path.path)
        val matchesLegacyPath = path.startsWith("cashflow/document_review")
        if (!matchesKnownPath && !matchesLegacyPath) return null

        return extractQueryParam(path, "documentId")
            ?: extractQueryParam(path, "id")
    }

    /**
     * Normalizes deep link paths so both custom scheme links and absolute http/https links
     * can be matched against KnownDeepLinks.
     *
     * Examples:
     * - auth/reset-password?token=abc -> auth/reset-password?token=abc
     * - dokus.ai/auth/reset-password?token=abc -> auth/reset-password?token=abc
     * - http://localhost:8081/auth/verify-email?token=abc -> auth/verify-email?token=abc
     * - localhost:8080/auth/verify-email?token=abc -> auth/verify-email?token=abc
     */
    private fun normalizeRoutePath(path: String): String {
        val trimmed = path.trimStart('/')
        val firstSlash = trimmed.indexOf('/')
        if (firstSlash <= 0) return trimmed

        val firstSegment = trimmed.substring(0, firstSlash)
        return if (firstSegment.contains('.') || firstSegment.contains(':')) {
            trimmed.substring(firstSlash + 1)
        } else {
            trimmed
        }
    }

    private fun extractQueryParam(path: String, key: String): String? {
        val query = path.substringAfter("?", "")
        if (query.isEmpty()) return null
        val prefix = "$key="
        return query
            .split("&")
            .firstOrNull { it.startsWith(prefix) }
            ?.substringAfter(prefix)
            ?.takeIf { it.isNotBlank() }
    }
}
