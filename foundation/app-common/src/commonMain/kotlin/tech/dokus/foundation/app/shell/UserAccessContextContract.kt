package tech.dokus.foundation.app.shell

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import tech.dokus.domain.enums.UserRole

@Immutable
data class UserAccessContext(
    val canCompanyManager: Boolean = true,
    val canBookkeeperConsole: Boolean = false,
    val isSurfaceAvailabilityResolved: Boolean = false,
    val currentTenantRole: UserRole? = null,
    val isBookkeeperConsoleDrillDown: Boolean = false,
) {
    val isAccountantReadOnly: Boolean
        get() = currentTenantRole == UserRole.Accountant

    val isStage2ReadOnly: Boolean
        get() = isAccountantReadOnly || (isBookkeeperConsoleDrillDown && currentTenantRole == null)

    val isBookkeeperConsoleOnly: Boolean
        get() = canBookkeeperConsole && !canCompanyManager
}

val LocalUserAccessContext = staticCompositionLocalOf { UserAccessContext() }
