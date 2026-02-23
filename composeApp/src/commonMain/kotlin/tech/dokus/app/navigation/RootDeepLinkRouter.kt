package tech.dokus.app.navigation

import tech.dokus.domain.model.common.DeepLink
import tech.dokus.domain.model.common.DeepLinks
import tech.dokus.domain.model.common.KnownDeepLinks
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.NavigationDestination

internal enum class DeepLinkTargetOwner {
    Root,
    Home,
}

internal sealed interface DeepLinkNavigationTarget {
    val owner: DeepLinkTargetOwner

    data class RootDestination(val destination: NavigationDestination) : DeepLinkNavigationTarget {
        override val owner: DeepLinkTargetOwner = DeepLinkTargetOwner.Root
    }

    data class RootUri(val uri: String) : DeepLinkNavigationTarget {
        override val owner: DeepLinkTargetOwner = DeepLinkTargetOwner.Root
    }

    data class HomeCommand(val command: HomeNavigationCommand) : DeepLinkNavigationTarget {
        override val owner: DeepLinkTargetOwner = DeepLinkTargetOwner.Home
    }
}

internal fun resolveDeepLinkNavigationTarget(deepLink: DeepLink): DeepLinkNavigationTarget {
    DeepLinks.extractResetPasswordToken(deepLink)?.let { token ->
        return DeepLinkNavigationTarget.RootDestination(AuthDestination.ResetPassword(token))
    }

    DeepLinks.extractVerifyEmailToken(deepLink)?.let { token ->
        return DeepLinkNavigationTarget.RootDestination(AuthDestination.VerifyEmail(token))
    }

    DeepLinks.extractServerConnect(deepLink)?.let { (host, port, protocol) ->
        return DeepLinkNavigationTarget.RootDestination(
            AuthDestination.ServerConnection(
                host = host,
                port = port,
                protocol = protocol
            )
        )
    }

    if (deepLink.path.contains(KnownDeepLinks.ServerConnect.path.path)) {
        return DeepLinkNavigationTarget.RootDestination(AuthDestination.ServerConnection())
    }

    DeepLinks.extractDocumentReviewId(deepLink)?.let { documentId ->
        return DeepLinkNavigationTarget.HomeCommand(
            HomeNavigationCommand.OpenDocumentReview(documentId)
        )
    }

    return DeepLinkNavigationTarget.RootUri(deepLink.withAppScheme)
}
