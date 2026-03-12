package tech.dokus.foundation.aura.components.tiles

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.text.font.FontWeight
import coil3.ImageLoader
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.workspace_add
import tech.dokus.foundation.aura.components.AvatarShape
import tech.dokus.foundation.aura.components.AvatarSize
import tech.dokus.foundation.aura.components.CompanyAvatarImage
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Small square tile showing a company's avatar (or initial if no avatar) with its name below.
 * Uses Material theme shapes and color scheme.
 *
 * @param initial The initial letter to display as fallback (typically first letter of company name)
 * @param label The company name to display below the avatar
 * @param avatarUrl Optional URL of the company avatar image
 * @param badge Optional secondary text shown below the label
 * @param imageLoader Optional authenticated image loader
 * @param onClick Click handler for the tile
 * @param modifier Modifier to be applied to the tile
 */
@Composable
fun CompanyTile(
    modifier: Modifier = Modifier,
    initial: String,
    label: String,
    avatarUrl: String? = null,
    badge: String? = null,
    imageLoader: ImageLoader? = null,
    onClick: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        DokusCardSurface(
            onClick = onClick,
        ) {
            CompanyAvatarImage(
                avatarUrl = avatarUrl,
                initial = initial,
                size = AvatarSize.Medium,
                shape = AvatarShape.RoundedSquare,
                imageLoader = imageLoader,
                modifier = Modifier.size(Constraints.AvatarSize.tile)
            )
        }

        Spacer(modifier = Modifier.height(Constraints.Spacing.medium))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        if (badge != null) {
            Text(
                text = badge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Preview
@Composable
private fun CompanyTilePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CompanyTile(
            initial = "D",
            label = "Dokus Tech",
            onClick = {}
        )
    }
}

/**
 * Outlined variant with a plus icon for creating a new workspace/company.
 */
@Composable
fun AddCompanyTile(
    modifier: Modifier = Modifier,
    label: String = stringResource(Res.string.workspace_add),
    onClick: () -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        DokusCardSurface(
            onClick = onClick,
        ) {
            Box(
                modifier = Modifier.size(Constraints.AvatarSize.tile),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.Plus,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(Constraints.Spacing.medium))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
