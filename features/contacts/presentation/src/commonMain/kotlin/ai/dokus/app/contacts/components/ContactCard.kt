package ai.dokus.app.contacts.components

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.contacts_active
import ai.dokus.app.resources.generated.contacts_customer
import ai.dokus.app.resources.generated.contacts_inactive
import ai.dokus.app.resources.generated.contacts_peppol
import ai.dokus.app.resources.generated.contacts_supplier
import ai.dokus.app.resources.generated.contacts_vendor
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.DerivedContactRoles
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ContactCard(
    contact: ContactDto,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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

                // Peppol badge
                if (contact.peppolEnabled) {
                    ContactBadge(
                        text = stringResource(Res.string.contacts_peppol),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

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
