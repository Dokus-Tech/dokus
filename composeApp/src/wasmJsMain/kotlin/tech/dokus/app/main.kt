package tech.dokus.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import kotlinx.browser.window
import tech.dokus.app.navigation.ExternalUriHandler
import tech.dokus.domain.model.common.DeepLink
import tech.dokus.domain.model.common.DeepLinks

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    emitStartupAuthDeepLink()

    ComposeViewport(document.body!!) {
        App()
    }
}

private fun emitStartupAuthDeepLink() {
    val currentUrl = window.location.href
    if (currentUrl.isBlank()) return

    val deepLink = DeepLink(currentUrl)
    val hasTokenizedAuthRoute = DeepLinks.extractVerifyEmailToken(deepLink) != null ||
        DeepLinks.extractResetPasswordToken(deepLink) != null

    if (hasTokenizedAuthRoute) {
        ExternalUriHandler.onNewUri(currentUrl)
        window.history.replaceState(null, "", window.location.pathname)
    }
}
