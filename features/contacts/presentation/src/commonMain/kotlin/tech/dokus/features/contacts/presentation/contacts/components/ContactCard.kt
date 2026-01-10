package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_active
import tech.dokus.aura.resources.contacts_customer
import tech.dokus.aura.resources.contacts_inactive
import tech.dokus.aura.resources.contacts_supplier
import tech.dokus.aura.resources.contacts_vendor
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.DerivedContactRoles
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ContactCard(
    contact: ContactDto,
    modifier: Modifier = Modifier
) {
    DokusCard(
        modifier = modifier,
        padding = DokusCardPadding.Default,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Name and active status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contact.name.value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Active/Inactive badge
                ContactStatusBadge(isActive = contact.isActive)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Email (if available)
            contact.email?.let { email ->
                Text(
                    text = email.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Role badges and tags
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Derived role badges
                contact.derivedRoles?.let { roles ->
                    ContactRoleBadges(roles = roles)
                }

                // NOTE: PEPPOL badge removed - PEPPOL status is now in PeppolDirectoryCacheTable
                // To show PEPPOL status, use the /contacts/{id}/peppol-status endpoint

                // Tags
                contact.tags?.let { tagsString ->
                    tagsString.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .forEach { tag ->
                            ContactBadge(
                                text = tag,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun ContactStatusBadge(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val (color, text) = if (isActive) {
        MaterialTheme.colorScheme.tertiary to stringResource(Res.string.contacts_active)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant to stringResource(Res.string.contacts_inactive)
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
    )
}

@Composable
private fun ContactRoleBadges(
    roles: DerivedContactRoles,
    modifier: Modifier = Modifier
) {
    if (roles.isCustomer) {
        ContactBadge(
            text = stringResource(Res.string.contacts_customer),
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier
        )
    }
    if (roles.isSupplier) {
        ContactBadge(
            text = stringResource(Res.string.contacts_supplier),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = modifier
        )
    }
    if (roles.isVendor) {
        ContactBadge(
            text = stringResource(Res.string.contacts_vendor),
            color = MaterialTheme.colorScheme.secondary,
            modifier = modifier
        )
    }
}

@Composable
private fun ContactBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
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
