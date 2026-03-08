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
import coil3.ImageLoader
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_doc_count_plural
import tech.dokus.aura.resources.contacts_doc_count_single
import tech.dokus.aura.resources.contacts_no_docs
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.DerivedContactRoles
import tech.dokus.foundation.app.network.rememberResolvedApiUrl
import tech.dokus.foundation.aura.components.AvatarShape
import tech.dokus.foundation.aura.components.AvatarSize
import tech.dokus.foundation.aura.components.CompanyAvatarImage
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.badges.ContactRole as UiContactRole
import tech.dokus.foundation.aura.components.badges.RoleBadge
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.Name
import kotlinx.datetime.LocalDateTime

/**
 * Mobile contact card with monogram, metadata, and compact document count.
 */
@Composable
internal fun ContactCard(
    contact: ContactDto,
    imageLoader: ImageLoader? = null,
    modifier: Modifier = Modifier
) {
    val initials = remember(contact.name.value) { extractInitials(contact.name.value) }
    val uiRole = remember(contact.derivedRoles) { mapToUiRole(contact.derivedRoles) }
    val avatarUrl = rememberResolvedApiUrl(contact.avatar?.small)
    val docCount = contact.invoiceCount + contact.inboundInvoiceCount + contact.expenseCount
    val docLabel = if (docCount == 1L) {
        stringResource(Res.string.contacts_doc_count_single)
    } else {
        stringResource(Res.string.contacts_doc_count_plural)
    }

    DokusCardSurface(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
        ) {
            CompanyAvatarImage(
                avatarUrl = avatarUrl,
                initial = initials,
                size = AvatarSize.Medium,
                shape = AvatarShape.RoundedSquare,
                imageLoader = imageLoader
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
            ) {
                Text(
                    text = contact.name.value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
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

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (docCount > 0) "$docCount $docLabel" else stringResource(Res.string.contacts_no_docs),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                        fontSize = 9.sp,
                    ),
                    color = if (docCount > 0) {
                        MaterialTheme.colorScheme.textMuted
                    } else {
                        MaterialTheme.colorScheme.textFaint
                    },
                )
            }

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

internal fun extractInitials(name: String): String {
    val words = name.split(" ").filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}"
        words.size == 1 -> words[0].take(2)
        else -> "?"
    }.uppercase()
}

internal fun mapToUiRole(roles: DerivedContactRoles?): UiContactRole? {
    if (roles == null) return null
    return when {
        roles.isSupplier || roles.isVendor -> UiContactRole.Vendor
        else -> null
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ContactCardPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    val now = LocalDateTime(2026, 1, 15, 10, 0)
    TestWrapper(parameters) {
        ContactCard(
            contact = ContactDto(
                id = ContactId.generate(),
                tenantId = TenantId.generate(),
                name = Name("Acme Corporation"),
                vatNumber = VatNumber("BE0123456789"),
                derivedRoles = DerivedContactRoles(isSupplier = true),
                invoiceCount = 5,
                inboundInvoiceCount = 3,
                expenseCount = 2,
                createdAt = now,
                updatedAt = now
            )
        )
    }
}
