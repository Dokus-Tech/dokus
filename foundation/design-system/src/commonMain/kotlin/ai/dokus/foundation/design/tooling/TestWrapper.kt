package ai.dokus.foundation.design.tooling

import ai.dokus.foundation.design.local.ScreenSizeProvided
import ai.dokus.foundation.design.local.ThemeManagerProvided
import ai.dokus.foundation.design.style.ThemeManager
import ai.dokus.foundation.design.style.ThemeMode
import ai.dokus.foundation.design.style.Themed
import tech.dokus.domain.enums.Language
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import org.jetbrains.compose.ui.tooling.preview.PreviewParameterProvider

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

    CompositionLocalProvider(
        LocalInspectionMode provides true,
    ) {
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