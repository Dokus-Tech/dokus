package tech.dokus.app.screens.tomorrow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.koin.compose.koinInject
import tech.dokus.app.navigation.local.LocalHomeNavController
import tech.dokus.app.screens.AiChatPlaceholder
import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.navigateTo

@Composable
internal fun TomorrowRoute(
    tokenManager: TokenManager = koinInject(),
) {
    val homeNavController = LocalHomeNavController.current
    var userTier by remember { mutableStateOf<SubscriptionTier?>(null) }

    LaunchedEffect(Unit) {
        userTier = tokenManager.getCurrentClaims()?.tenant?.subscriptionTier
            ?: SubscriptionTier.Core
    }

    when {
        userTier == null -> {
            // Still loading tier
        }

        !SubscriptionTier.hasTomorrowAccess(userTier!!) -> {
            LaunchedEffect(Unit) {
                homeNavController.navigateTo(HomeDestination.Today) {
                    popUpTo(HomeDestination.Tomorrow::class) { inclusive = true }
                }
            }
        }

        else -> {
            AiChatPlaceholder()
        }
    }
}
