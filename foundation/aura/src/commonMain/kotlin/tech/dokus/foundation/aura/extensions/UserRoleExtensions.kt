package tech.dokus.foundation.aura.extensions

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.role_accountant
import tech.dokus.aura.resources.role_admin
import tech.dokus.aura.resources.role_editor
import tech.dokus.aura.resources.role_owner
import tech.dokus.aura.resources.role_viewer
import tech.dokus.domain.enums.UserRole

/**
 * Extension property to get a localized display name for a UserRole.
 */
val UserRole.localized: String
    @Composable get() = when (this) {
        UserRole.Owner -> stringResource(Res.string.role_owner)
        UserRole.Admin -> stringResource(Res.string.role_admin)
        UserRole.Accountant -> stringResource(Res.string.role_accountant)
        UserRole.Editor -> stringResource(Res.string.role_editor)
        UserRole.Viewer -> stringResource(Res.string.role_viewer)
    }
