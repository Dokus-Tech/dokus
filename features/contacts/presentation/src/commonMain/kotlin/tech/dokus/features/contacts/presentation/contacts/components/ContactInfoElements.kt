package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_active
import tech.dokus.aura.resources.contacts_customer
import tech.dokus.aura.resources.contacts_inactive
import tech.dokus.aura.resources.contacts_supplier
import tech.dokus.aura.resources.contacts_vendor
import tech.dokus.domain.model.contact.DerivedContactRoles
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.constrains.Constraints

// UI dimension constants
private val InfoRowSpacing = Constraints.Spacing.medium
private val InfoIconSize = Constraints.IconSize.smallMedium
private val SkeletonSpacerHeight = Constraints.Spacing.xSmall
private val SkeletonTitleHeight = Constraints.IconSize.medium + Constraints.Spacing.xSmall
private val SkeletonIconWidth = Constraints.IconSize.smallMedium
private val SkeletonIconHeight = Constraints.IconSize.smallMedium
private val SkeletonLabelWidth = Constraints.IconSize.xxLarge - Constraints.Spacing.xSmall
private val SkeletonLabelHeight = Constraints.Spacing.medium
private val SkeletonValueWidth =
    Constraints.AvatarSize.large +
        Constraints.Spacing.large +
        Constraints.Spacing.xSmall +
        Constraints.Spacing.xxSmall
private val SkeletonValueHeight = Constraints.IconSize.xSmall
private val BadgeCornerRadius = Constraints.CornerRadius.badge
private val BadgePaddingHorizontal = Constraints.Spacing.small
private val BadgePaddingVertical = Constraints.Spacing.xSmall
private const val SkeletonRepeatCount = 4
private const val BadgeBackgroundAlpha = 0.1f
private const val SkeletonTitleWidthFraction = 0.6f

@Composable
internal fun ContactInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(InfoRowSpacing)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(InfoIconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
internal fun ContactInfoSkeleton() {
    Column(
        verticalArrangement = Arrangement.spacedBy(InfoRowSpacing)
    ) {
        ShimmerLine(modifier = Modifier.fillMaxWidth(SkeletonTitleWidthFraction), height = SkeletonTitleHeight)
        Spacer(modifier = Modifier.height(SkeletonSpacerHeight))

        repeat(SkeletonRepeatCount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(InfoRowSpacing)
            ) {
                ShimmerLine(modifier = Modifier.width(SkeletonIconWidth), height = SkeletonIconHeight)
                Column(verticalArrangement = Arrangement.spacedBy(SkeletonSpacerHeight)) {
                    ShimmerLine(modifier = Modifier.width(SkeletonLabelWidth), height = SkeletonLabelHeight)
                    ShimmerLine(modifier = Modifier.width(SkeletonValueWidth), height = SkeletonValueHeight)
                }
            }
        }
    }
}

@Composable
internal fun ContactStatusLabel(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val (color, text) = if (isActive) {
        MaterialTheme.colorScheme.primary to stringResource(Res.string.contacts_active)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant to stringResource(Res.string.contacts_inactive)
    }

    Surface(
        color = color.copy(alpha = BadgeBackgroundAlpha),
        shape = RoundedCornerShape(BadgeCornerRadius),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = BadgePaddingHorizontal, vertical = BadgePaddingVertical)
        )
    }
}

@Composable
internal fun ContactInfoRoleBadges(
    roles: DerivedContactRoles,
    modifier: Modifier = Modifier
) {
    if (roles.isCustomer) {
        ContactInfoRoleBadge(
            text = stringResource(Res.string.contacts_customer),
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier
        )
    }
    if (roles.isSupplier) {
        ContactInfoRoleBadge(
            text = stringResource(Res.string.contacts_supplier),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = modifier
        )
    }
    if (roles.isVendor) {
        ContactInfoRoleBadge(
            text = stringResource(Res.string.contacts_vendor),
            color = MaterialTheme.colorScheme.secondary,
            modifier = modifier
        )
    }
}

@Composable
internal fun ContactInfoRoleBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = BadgeBackgroundAlpha),
        shape = RoundedCornerShape(BadgeCornerRadius),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = BadgePaddingHorizontal, vertical = BadgePaddingVertical)
        )
    }
}

@Composable
internal fun ContactInfoTagBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.outline.copy(alpha = BadgeBackgroundAlpha),
        shape = RoundedCornerShape(BadgeCornerRadius),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = BadgePaddingHorizontal, vertical = BadgePaddingVertical)
        )
    }
}

// ============================================================================
// PREVIEWS
// ============================================================================

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ContactInfoRowPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        ContactInfoRow(
            icon = Icons.Filled.Email,
            label = "Email",
            value = "info@acme.be"
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ContactStatusLabelPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        Column(verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)) {
            ContactStatusLabel(isActive = true)
            ContactStatusLabel(isActive = false)
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ContactInfoRoleBadgesPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        Row(horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)) {
            ContactInfoRoleBadges(
                roles = DerivedContactRoles(
                    isCustomer = true,
                    isSupplier = true,
                    isVendor = true
                )
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ContactInfoTagBadgePreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        Row(horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)) {
            ContactInfoTagBadge(text = "client")
            ContactInfoTagBadge(text = "vip")
        }
    }
}
