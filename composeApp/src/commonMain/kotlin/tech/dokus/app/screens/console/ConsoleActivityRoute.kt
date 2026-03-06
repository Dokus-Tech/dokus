package tech.dokus.app.screens.console

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.console_activity_subtitle
import tech.dokus.aura.resources.console_activity_title
import tech.dokus.aura.resources.console_requests_period_label
import tech.dokus.foundation.app.shell.HomeShellTopBarAction
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.shell.LocalUserAccessContext
import tech.dokus.foundation.app.shell.RegisterHomeShellTopBar
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.route
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateToTopLevelTab

@Composable
internal fun ConsoleActivityRoute() {
    val accessContext = LocalUserAccessContext.current
    val navController = LocalNavController.current

    LaunchedEffect(accessContext.isSurfaceAvailabilityResolved, accessContext.canBookkeeperConsole) {
        if (isConsoleAccessDenied(accessContext)) {
            navController.navigateToTopLevelTab(HomeDestination.Today)
        }
    }

    if (!canRenderConsoleContent(accessContext)) return

    val title = stringResource(Res.string.console_activity_title)
    val subtitle = stringResource(Res.string.console_activity_subtitle)
    val periodLabel = stringResource(Res.string.console_requests_period_label)

    RegisterHomeShellTopBar(
        route = HomeDestination.ConsoleActivity.route,
        config = HomeShellTopBarConfig(
            mode = HomeShellTopBarMode.Title(
                title = title,
                subtitle = subtitle,
            ),
            actions = listOf(
                HomeShellTopBarAction.Text(
                    label = periodLabel,
                    onClick = {},
                )
            )
        ),
    )

    ConsoleActivityScreen()
}

