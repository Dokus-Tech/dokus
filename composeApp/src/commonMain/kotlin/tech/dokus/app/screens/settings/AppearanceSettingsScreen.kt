package tech.dokus.app.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.appearance_settings_title
import tech.dokus.aura.resources.appearance_theme
import tech.dokus.aura.resources.appearance_theme_dark
import tech.dokus.aura.resources.appearance_theme_light
import tech.dokus.aura.resources.appearance_theme_system
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.constrains.withContentPaddingForScrollable
import tech.dokus.foundation.aura.local.LocalThemeManager
import tech.dokus.foundation.aura.style.ThemeMode

/**
 * Appearance settings screen with top bar.
 * For mobile navigation flow.
 */
@Composable
fun AppearanceSettingsScreen() {
    Scaffold(
        topBar = {
            PTopAppBar(Res.string.appearance_settings_title)
        }
    ) { contentPadding ->
        AppearanceSettingsContent(
            modifier = Modifier.padding(contentPadding)
        )
    }
}

/**
 * Appearance settings content without scaffold.
 * Can be embedded in split-pane layout for desktop or used in full-screen for mobile.
 */
@Composable
fun AppearanceSettingsContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val themeManager = LocalThemeManager.current
    val currentTheme by themeManager.themeMode.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .withContentPaddingForScrollable()
    ) {
        DokusCard(
            modifier = Modifier.fillMaxWidth(),
            padding = DokusCardPadding.Default,
        ) {
            Column {
                Text(
                    text = stringResource(Res.string.appearance_theme),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(12.dp))

                ThemeOption.entries.forEach { option ->
                    ThemeOptionRow(
                        option = option,
                        isSelected = currentTheme == option.themeMode,
                        onClick = { themeManager.setThemeMode(option.themeMode) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    option: ThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null // Handled by selectable modifier
        )
        Text(
            text = stringResource(option.labelRes),
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * UI options for theme selection, mapped to [ThemeMode] values.
 */
private enum class ThemeOption(
    val themeMode: ThemeMode,
    val labelRes: StringResource
) {
    Light(ThemeMode.LIGHT, Res.string.appearance_theme_light),
    Dark(ThemeMode.DARK, Res.string.appearance_theme_dark),
    System(ThemeMode.SYSTEM, Res.string.appearance_theme_system)
}
