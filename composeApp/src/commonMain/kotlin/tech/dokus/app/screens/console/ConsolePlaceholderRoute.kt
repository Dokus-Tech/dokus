package tech.dokus.app.screens.console

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.console_clients_subtitle
import tech.dokus.foundation.app.shell.LocalUserAccessContext
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateToTopLevelTab

@Composable
internal fun ConsolePlaceholderRoute(
    title: String,
    subtitle: String = stringResource(Res.string.console_clients_subtitle),
) {
    val accessContext = LocalUserAccessContext.current
    val navController = LocalNavController.current

    LaunchedEffect(accessContext.isSurfaceAvailabilityResolved, accessContext.canBookkeeperConsole) {
        if (isConsoleAccessDenied(accessContext)) {
            navController.navigateToTopLevelTab(HomeDestination.Today)
        }
    }

    if (!canRenderConsoleContent(accessContext)) return

    ConsolePlaceholderContent(
        title = title,
        subtitle = subtitle,
    )
}

@Composable
private fun ConsolePlaceholderContent(
    title: String,
    subtitle: String,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Constraints.Spacing.medium),
        contentAlignment = Alignment.TopCenter,
    ) {
        DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Constraints.Spacing.large),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(name = "Console Placeholder Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun ConsolePlaceholderDesktopPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ConsolePlaceholderContent(
            title = "Export",
            subtitle = stringResource(Res.string.console_clients_subtitle),
        )
    }
}

@Preview(name = "Console Placeholder Mobile", widthDp = 390, heightDp = 844)
@Composable
private fun ConsolePlaceholderMobilePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ConsolePlaceholderContent(
            title = "Export",
            subtitle = stringResource(Res.string.console_clients_subtitle),
        )
    }
}

@Preview(name = "Console Export Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun ConsoleExportDesktopPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ConsolePlaceholderContent(
            title = "Export",
            subtitle = stringResource(Res.string.console_clients_subtitle),
        )
    }
}
