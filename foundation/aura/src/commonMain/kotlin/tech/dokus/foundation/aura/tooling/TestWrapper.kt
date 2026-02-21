package tech.dokus.foundation.aura.tooling

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.navigation.compose.rememberNavController
import tech.dokus.domain.enums.Language
import tech.dokus.foundation.aura.local.ScreenSizeProvided
import tech.dokus.foundation.aura.local.ThemeManagerProvided
import tech.dokus.foundation.aura.style.FixedThemeManager
import tech.dokus.foundation.aura.style.Themed
import tech.dokus.navigation.local.LocalNavController

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
    // Use FixedThemeManager to avoid accessing persistence in test/preview context
    val themeManager = remember(parameters.isDarkMode) {
        FixedThemeManager(isDarkMode = parameters.isDarkMode)
    }

    PreviewWrapper {
        ThemeManagerProvided(themeManager) {
            Themed {
                ScreenSizeProvided {
                    CompositionLocalProvider(LocalNavController provides rememberNavController()) {
                        Surface {
                            content()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreviewWrapper(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalInspectionMode provides true, content = content)
