package ai.dokus.app.screens.settings

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.team_coming_soon
import ai.dokus.app.resources.generated.team_settings_title
import ai.dokus.foundation.design.components.common.PTopAppBar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource

/**
 * Placeholder screen for Team Members settings.
 * Team management will be implemented in a future release.
 */
@Composable
fun TeamSettingsScreen() {
    Scaffold(
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.team_settings_title)
            )
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(Res.string.team_coming_soon),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
