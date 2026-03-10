package tech.dokus.foundation.aura.extensions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_resolution_document
import tech.dokus.aura.resources.banking_resolution_transfer
import tech.dokus.domain.enums.ResolutionType

val ResolutionType.localized: String
    @Composable get() = when (this) {
        ResolutionType.Document -> stringResource(Res.string.banking_resolution_document)
        ResolutionType.Transfer -> stringResource(Res.string.banking_resolution_transfer)
    }

val ResolutionType.iconized: ImageVector
    get() = when (this) {
        ResolutionType.Document -> Icons.Default.Description
        ResolutionType.Transfer -> Icons.Default.SwapHoriz
    }
