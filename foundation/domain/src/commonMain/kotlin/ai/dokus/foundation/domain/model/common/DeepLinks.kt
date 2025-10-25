package ai.dokus.foundation.domain.model.common

import ai.dokus.foundation.domain.SessionId
import ai.dokus.foundation.domain.model.common.DeepLink.Companion.APP_SCHEME
import ai.dokus.foundation.domain.model.common.DeepLink.Companion.HTTPS_SCHEME
import kotlinx.serialization.Serializable
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
    QrDecision(DeepLink("auth/qr/decision"), DeepLink("auth/qr/decision?s={sessionId}&t={token}"));
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
}