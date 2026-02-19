package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.DerivedContactRoles
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.MonogramAvatar
import tech.dokus.foundation.aura.components.badges.ContactRole as UiContactRole
import tech.dokus.foundation.aura.components.badges.RoleBadge
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted

/**
 * Mobile contact card with MonogramAvatar + name + RoleBadge + VAT + doc count.
 * Bank/Accountant contacts get accent styling on the card.
 */
@Composable
internal fun ContactCard(
    contact: ContactDto,
    modifier: Modifier = Modifier
) {
    val initials = remember(contact.name.value) { extractInitials(contact.name.value) }
    val uiRole = remember(contact.derivedRoles) { mapToUiRole(contact.derivedRoles) }
    val docCount = contact.invoiceCount + contact.inboundInvoiceCount + contact.expenseCount
    val isAccent = uiRole == UiContactRole.Bank || uiRole == UiContactRole.Accountant

    DokusCardSurface(
        modifier = modifier,
        accent = isAccent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
        ) {
            MonogramAvatar(
                initials = initials,
                size = 38.dp,
                radius = 10.dp,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
            ) {
                Text(
                    text = contact.name.value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
                ) {
                    if (uiRole != null) {
                        RoleBadge(role = uiRole)
                    }
                    contact.vatNumber?.let { vat ->
                        Text(
                            text = vat.value,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                            ),
                            color = MaterialTheme.colorScheme.textMuted,
                            maxLines = 1,
                        )
                    }
                }
            }

            // Right side: doc count
            if (docCount > 0) {
                Text(
                    text = "$docCount docs",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                        fontSize = 9.sp,
                    ),
                    color = MaterialTheme.colorScheme.textMuted,
                )
            } else {
                Text(
                    text = "No docs",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                    ),
                    color = MaterialTheme.colorScheme.textFaint,
                )
            }

            // Chevron
            Text(
                text = "\u203A",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.textMuted,
            )
        }
    }
}

// =============================================================================
// Helpers (shared with ContactsList.kt)
// =============================================================================

/**
 * Extract initials from a contact name (first letter of first two words).
 */
internal fun extractInitials(name: String): String {
    val words = name.split(" ").filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}"
        words.size == 1 -> words[0].take(2)
        else -> "?"
    }.uppercase()
}

/**
 * Map domain DerivedContactRoles to aura ContactRole for badge display.
 * Only Vendor/Supplier maps to a badge; Customer has no UI badge.
 */
internal fun mapToUiRole(roles: DerivedContactRoles?): UiContactRole? {
    if (roles == null) return null
    return when {
        roles.isSupplier || roles.isVendor -> UiContactRole.Vendor
        else -> null
    }
}
