package tech.dokus.app.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import tech.dokus.app.navigation.ExternalUriHandler.onNewUri
import tech.dokus.domain.model.common.DeepLink

/**
 * Singleton handler for external URIs/deep links following the official KMP pattern.
 *
 * Pattern from https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-navigation-deep-links.html
 *
 * Platform-specific code calls [onNewUri] when a deep link is received.
 *
 * If a URI arrives before bootstrap completes, it's cached.
 */
object ExternalUriHandler {
    // Cache the URI if bootstrap hasn't completed yet
    private val deeplinkFlow = MutableStateFlow<DeepLink?>(null)
    val deeplinkState = deeplinkFlow.asStateFlow()

    /**
     * Called by platform-specific code when a new URI/deep link is received.
     *
     * @param uri The full URI string (e.g., "dokus://auth/qr/decision?s=123&t=abc")
     *
     * If the listener is already set (app running in background), processes immediately.
     */
    fun onNewUri(uri: String) {
        println("[ExternalUriHandler] Received URI: $uri")
        deeplinkFlow.tryEmit(DeepLink(uri))
    }
}
