package tech.dokus.foundation.aura.tooling

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider
import tech.dokus.domain.enums.Language
import tech.dokus.foundation.aura.local.ScreenSizeProvided
import tech.dokus.foundation.aura.local.ThemeManagerProvided
import tech.dokus.foundation.aura.style.ThemeManager
import tech.dokus.foundation.aura.style.ThemeMode
import tech.dokus.foundation.aura.style.Themed

data class PreviewParameters(
    val isDarkMode: Boolean,
    val language: Language,
)

class PreviewParametersProvider : PreviewParameterProvider<PreviewParameters> {
    override val values: Sequence<PreviewParameters> = sequence {
        val darkModes = listOf(false, true)

        for (isDarkMode in darkModes) {
            yield(
                PreviewParameters(
                    isDarkMode = isDarkMode,
                    language = Language.En,
                )
            )
//            for (language in languages) {
//                yield(
//                    PreviewParameters(
//                        isDarkMode = isDarkMode,
//                        language = language
//                    )
//                )
//            }
        }
    }
}

@Composable
fun TestWrapper(parameters: PreviewParameters, content: @Composable () -> Unit) {
    // Create a ThemeManager with explicit mode for previews
    val themeManager = remember(parameters.isDarkMode) {
        ThemeManager().apply {
            setThemeMode(if (parameters.isDarkMode) ThemeMode.DARK else ThemeMode.LIGHT)
        }
    }

    PreviewWrapper {
        ThemeManagerProvided(themeManager) {
            Themed {
                ScreenSizeProvided {
                    Surface {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun PreviewWrapper(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalInspectionMode provides true, content = content)
