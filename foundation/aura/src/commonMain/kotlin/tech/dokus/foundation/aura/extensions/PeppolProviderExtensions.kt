package tech.dokus.foundation.aura.extensions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.peppol_provider_recommand_description
import tech.dokus.domain.model.PeppolProvider

val PeppolProvider.localized: String
    @Composable get() = when (this) {
        PeppolProvider.Recommand -> "Recommand"
    }

val PeppolProvider.description: String
    @Composable get() = when (this) {
        PeppolProvider.Recommand -> stringResource(Res.string.peppol_provider_recommand_description)
    }

val PeppolProvider.iconized: ImageVector
    get() = when (this) {
        PeppolProvider.Recommand -> Icons.Outlined.Receipt
    }

val PeppolProvider.websiteUrl: String
    get() = when (this) {
        PeppolProvider.Recommand -> "https://app.recommand.eu"
    }
