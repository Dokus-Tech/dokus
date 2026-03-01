package tech.dokus.foundation.app.shell

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import tech.dokus.domain.enums.UserRole

@Immutable
data class UserAccessContext(
    val canWorkspace: Boolean = true,
    val canConsole: Boolean = false,
    val isSurfaceAvailabilityResolved: Boolean = false,
    val currentTenantRole: UserRole? = null,
    val isConsoleDrillDown: Boolean = false,
) {
    val isAccountantReadOnly: Boolean
        get() = currentTenantRole == UserRole.Accountant

    val isStage2ReadOnly: Boolean
        get() = isAccountantReadOnly || (isConsoleDrillDown && currentTenantRole == null)

    val isConsoleOnlySurface: Boolean
        get() = canConsole && !canWorkspace
}

val LocalUserAccessContext = staticCompositionLocalOf { UserAccessContext() }
