@file:Suppress("ReturnCount") // Deep link parsing requires multiple early returns for validation

package tech.dokus.domain.model.common

import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.model.common.DeepLink.Companion.APP_SCHEME
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
            .replace("$HTTPS_SCHEME://", "")

    val withAppScheme: String
        get() = if (path.startsWith(APP_SCHEME)) path else "$APP_SCHEME://$path"

    val withHttpsScheme: String
        get() = if (path.startsWith(HTTPS_SCHEME)) path else "$HTTPS_SCHEME://$path"

    override fun toString(): String = path

    companion object {
        const val APP_SCHEME = "dokus"
        const val HTTPS_SCHEME = "https"

        /**
         * Builds the string.
         * If for some reason [APP_SCHEME] or [HTTPS_SCHEME] is included, it will be removed.
         * Use [withHttpsScheme] or [withAppScheme] if needed
         */
        fun build(builder: StringBuilder.() -> Unit): DeepLink {
            val string = buildString(builder)
                .replace("$APP_SCHEME://", "")
                .replace("$HTTPS_SCHEME://", "")
            return DeepLink(string)
        }
    }
}

enum class KnownDeepLinks(val path: DeepLink, val pattern: DeepLink) {
    QrDecision(DeepLink("auth/qr/decision"), DeepLink("auth/qr/decision?s={sessionId}&t={token}")),
    ServerConnect(DeepLink("connect"), DeepLink("connect?host={host}&port={port}&protocol={protocol}"))
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
        val path = deepLink.path
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
        val path = deepLink.path
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
}
