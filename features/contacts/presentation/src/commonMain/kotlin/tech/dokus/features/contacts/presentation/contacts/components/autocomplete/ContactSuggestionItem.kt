package tech.dokus.features.contacts.presentation.contacts.components.autocomplete

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_vat_value
import tech.dokus.aura.resources.contacts_customer
import tech.dokus.aura.resources.contacts_supplier
import tech.dokus.aura.resources.contacts_vendor
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.DerivedContactRoles
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.Name
import tech.dokus.domain.Email
import kotlinx.datetime.LocalDateTime

/**
 * Individual contact item in the autocomplete dropdown.
 * Shows contact name, email, and role badges.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ContactSuggestionItem(
    contact: ContactDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DropdownItemPadding)
        ) {
            // Name
            Text(
                text = contact.name.value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Email (if available)
            contact.email?.let { email ->
                Text(
                    text = email.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // VAT number (if available)
            contact.vatNumber?.let { vat ->
                Text(
                    text = stringResource(Res.string.common_vat_value, vat.value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Role badges
            contact.derivedRoles?.let { roles ->
                if (roles.isCustomer || roles.isSupplier || roles.isVendor) {
                    Spacer(modifier = Modifier.height(RoleBadgeTopSpacing))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(RoleBadgeSpacing),
                        verticalArrangement = Arrangement.spacedBy(RoleBadgeSpacing)
                    ) {
                        if (roles.isCustomer) {
                            ContactRoleBadge(
                                text = stringResource(Res.string.contacts_customer),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (roles.isSupplier) {
                            ContactRoleBadge(
                                text = stringResource(Res.string.contacts_supplier),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        if (roles.isVendor) {
                            ContactRoleBadge(
                                text = stringResource(Res.string.contacts_vendor),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Small badge showing contact role.
 */
@Composable
internal fun ContactRoleBadge(
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

// ============================================================================
// PREVIEWS
// ============================================================================

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ContactSuggestionItemPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    val now = LocalDateTime(2026, 1, 15, 10, 0)
    TestWrapper(parameters) {
        ContactSuggestionItem(
            contact = ContactDto(
                id = ContactId.generate(),
                tenantId = TenantId.generate(),
                name = Name("Acme Corporation"),
                email = Email("info@acme.be"),
                vatNumber = VatNumber("BE0123456789"),
                derivedRoles = DerivedContactRoles(
                    isCustomer = true,
                    isSupplier = true
                ),
                createdAt = now,
                updatedAt = now
            ),
            onClick = {}
        )
    }
}
