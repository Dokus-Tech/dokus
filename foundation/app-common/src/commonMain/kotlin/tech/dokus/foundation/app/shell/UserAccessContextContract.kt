package tech.dokus.foundation.app.shell

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class UserAccessContext(
    val canCompanyManager: Boolean = true,
    val canBookkeeperConsole: Boolean = false,
    val isSurfaceAvailabilityResolved: Boolean = false,
    val isBookkeeperConsoleDrillDown: Boolean = false,
) {
    val isBookkeeperConsoleOnly: Boolean
        get() = canBookkeeperConsole && !canCompanyManager
}

val LocalUserAccessContext = staticCompositionLocalOf { UserAccessContext() }
