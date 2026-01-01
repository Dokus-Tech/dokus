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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_active
import tech.dokus.aura.resources.contacts_customer
import tech.dokus.aura.resources.contacts_inactive
import tech.dokus.aura.resources.contacts_supplier
import tech.dokus.aura.resources.contacts_vendor
import tech.dokus.domain.model.contact.DerivedContactRoles
import tech.dokus.foundation.aura.components.common.ShimmerLine

@Composable
internal fun ContactInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerLine(modifier = Modifier.fillMaxWidth(0.6f), height = 28.dp)
        Spacer(modifier = Modifier.height(4.dp))

        repeat(4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerLine(modifier = Modifier.width(20.dp), height = 20.dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ShimmerLine(modifier = Modifier.width(60.dp), height = 12.dp)
                    ShimmerLine(modifier = Modifier.width(150.dp), height = 16.dp)
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
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
internal fun ContactInfoTagBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
