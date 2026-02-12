package tech.dokus.app.screens.settings.screenshot

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tech.dokus.app.screens.settings.NotificationPreferencesContent
import tech.dokus.app.viewmodel.NotificationPreferencesState
import tech.dokus.domain.enums.NotificationType
import tech.dokus.domain.model.NotificationPreferenceDto
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.ScreenSize
import tech.dokus.foundation.aura.local.ThemeManagerProvided
import tech.dokus.foundation.aura.style.FixedThemeManager
import tech.dokus.foundation.aura.style.Themed

@RunWith(Parameterized::class)
class NotificationPreferencesScreenshotTest(
    private val viewport: NotificationPreferencesViewport
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = NotificationPreferencesViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = viewport.deviceConfig,
        showSystemUi = false,
        maxPercentDifference = 0.1
    )

    @Test
    fun notificationPreferencesContent() {
        val state = NotificationPreferencesState.Content(
            preferences = NotificationType.entries.associateWith { type ->
                NotificationPreferenceDto(
                    type = type,
                    emailEnabled = when (type) {
                        NotificationType.PeppolReceived -> false
                        NotificationType.PeppolSendConfirmed -> false
                        NotificationType.VatWarning -> true
                        NotificationType.PaymentConfirmed -> true
                        NotificationType.SubscriptionChanged -> true
                        else -> type.defaultEmailEnabled
                    },
                    emailLocked = type.emailLocked
                )
            }
        )

        paparazzi.snapshotAllThemes("NotificationPreferencesScreen", viewport) {
            NotificationPreferencesContent(
                state = state,
                onIntent = {},
                modifier = Modifier
            )
        }
    }

    @Test
    fun notificationPreferencesContentUpdating() {
        val state = NotificationPreferencesState.Content(
            preferences = NotificationType.entries.associateWith { type ->
                NotificationPreferenceDto(
                    type = type,
                    emailEnabled = type.defaultEmailEnabled,
                    emailLocked = type.emailLocked
                )
            },
            updatingTypes = setOf(NotificationType.VatWarning)
        )

        paparazzi.snapshotAllThemes("NotificationPreferencesScreen_updating", viewport) {
            NotificationPreferencesContent(
                state = state,
                onIntent = {},
                modifier = Modifier
            )
        }
    }
}

enum class NotificationPreferencesViewport(
    val deviceConfig: DeviceConfig,
    val displayName: String,
    val screenSize: ScreenSize
) {
    COMPACT(
        deviceConfig = DeviceConfig(
            screenWidth = 360,
            screenHeight = 740,
            density = Density.XXHIGH,
            softButtons = false
        ),
        displayName = "compact",
        screenSize = ScreenSize.SMALL
    ),
    EXPANDED(
        deviceConfig = DeviceConfig(
            screenWidth = 1280,
            screenHeight = 900,
            density = Density.MEDIUM,
            softButtons = false
        ),
        displayName = "expanded",
        screenSize = ScreenSize.LARGE
    )
}

@Composable
private fun NotificationPreferencesThemeWrapper(
    isDarkMode: Boolean,
    screenSize: ScreenSize,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalInspectionMode provides true,
        LocalScreenSize provides screenSize
    ) {
        ThemeManagerProvided(themeManager = FixedThemeManager(isDarkMode)) {
            Themed {
                Surface(tonalElevation = 0.dp) {
                    content()
                }
            }
        }
    }
}

private fun Paparazzi.snapshotAllThemes(
    baseName: String,
    viewport: NotificationPreferencesViewport,
    content: @Composable () -> Unit
) {
    snapshot("${baseName}_${viewport.displayName}_light") {
        NotificationPreferencesThemeWrapper(
            isDarkMode = false,
            screenSize = viewport.screenSize
        ) {
            content()
        }
    }
    snapshot("${baseName}_${viewport.displayName}_dark") {
        NotificationPreferencesThemeWrapper(
            isDarkMode = true,
            screenSize = viewport.screenSize
        ) {
            content()
        }
    }
}
