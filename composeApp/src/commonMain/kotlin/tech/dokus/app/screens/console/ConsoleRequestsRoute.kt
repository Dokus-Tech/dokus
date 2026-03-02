package tech.dokus.app.screens.console

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import tech.dokus.foundation.app.shell.LocalUserAccessContext
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateToTopLevelTab

@Composable
internal fun ConsoleRequestsRoute() {
    val accessContext = LocalUserAccessContext.current
    val navController = LocalNavController.current

    LaunchedEffect(accessContext.isSurfaceAvailabilityResolved, accessContext.canBookkeeperConsole) {
        if (isConsoleAccessDenied(accessContext)) {
            navController.navigateToTopLevelTab(HomeDestination.Today)
        }
    }

    if (!canRenderConsoleContent(accessContext)) return

    ConsoleRequestsScreen()
}

