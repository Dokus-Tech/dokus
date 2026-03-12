package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import com.composables.icons.lucide.Banknote
import com.composables.icons.lucide.Building2
import com.composables.icons.lucide.Clock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mail
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.Phone
import com.composables.icons.lucide.Receipt
import com.composables.icons.lucide.User
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_percent_value
import tech.dokus.aura.resources.contacts_address
import tech.dokus.aura.resources.contacts_company_number
import tech.dokus.aura.resources.contacts_contact_person
import tech.dokus.aura.resources.contacts_default_vat_rate
import tech.dokus.aura.resources.contacts_email
import tech.dokus.aura.resources.contacts_payment_terms
import tech.dokus.aura.resources.contacts_payment_terms_value
import tech.dokus.aura.resources.contacts_phone
import tech.dokus.aura.resources.contacts_tags
import tech.dokus.aura.resources.contacts_vat_number
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.foundation.app.network.rememberAuthenticatedImageLoader
import tech.dokus.foundation.app.network.rememberResolvedApiUrl
import tech.dokus.foundation.aura.components.AvatarShape
import tech.dokus.foundation.aura.components.AvatarSize
import tech.dokus.foundation.aura.components.CompanyAvatarImage
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.Name
import tech.dokus.domain.Email
import tech.dokus.domain.PhoneNumber
import tech.dokus.domain.model.contact.DerivedContactRoles
import kotlinx.datetime.LocalDateTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ContactInfoContent(
    contact: ContactDto
) {
    val imageLoader = rememberAuthenticatedImageLoader()
    val avatarUrl = rememberResolvedApiUrl(contact.avatar?.medium ?: contact.avatar?.small)
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompanyAvatarImage(
                avatarUrl = avatarUrl,
                initial = contact.name.value.take(1),
                size = AvatarSize.Medium,
                shape = AvatarShape.RoundedSquare,
                imageLoader = imageLoader
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = contact.name.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))
            ContactStatusLabel(isActive = contact.isActive)
        }

        contact.derivedRoles?.let { roles ->
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ContactInfoRoleBadges(roles = roles)
            }
        }

        HorizontalDivider()

        contact.email?.let {
            ContactInfoRow(
                icon = Lucide.Mail,
                label = stringResource(Res.string.contacts_email),
                value = it.value
            )
        }

        contact.phone?.let {
            ContactInfoRow(
                icon = Lucide.Phone,
                label = stringResource(Res.string.contacts_phone),
                value = it.value
            )
        }

        contact.contactPerson?.let {
            ContactInfoRow(
                icon = Lucide.User,
                label = stringResource(Res.string.contacts_contact_person),
                value = it
            )
        }

        contact.vatNumber?.let {
            ContactInfoRow(
                icon = Lucide.Receipt,
                label = stringResource(Res.string.contacts_vat_number),
                value = it.value
            )
        }

        contact.companyNumber?.let {
            ContactInfoRow(
                icon = Lucide.Building2,
                label = stringResource(Res.string.contacts_company_number),
                value = it
            )
        }

        val hasAddress = listOfNotNull(
            contact.addressLine1,
            contact.addressLine2,
            contact.city,
            contact.postalCode,
            contact.country
        ).isNotEmpty()

        if (hasAddress) {
            val addressLines = buildList {
                contact.addressLine1?.let { add(it) }
                contact.addressLine2?.let { add(it) }
                val cityPostal = listOfNotNull(
                    contact.postalCode,
                    contact.city
                ).joinToString(" ")
                if (cityPostal.isNotBlank()) add(cityPostal)
                contact.country?.let { add(it) }
            }

            ContactInfoRow(
                icon = Lucide.MapPin,
                label = stringResource(Res.string.contacts_address),
                value = addressLines.joinToString("\n")
            )
        }

        // NOTE: PEPPOL Settings section removed - PEPPOL status is now discovery data
        // in PeppolDirectoryCacheTable, resolved via /contacts/{id}/peppol-status endpoint

        ContactInfoRow(
            icon = Lucide.Clock,
            label = stringResource(Res.string.contacts_payment_terms),
            value = stringResource(Res.string.contacts_payment_terms_value, contact.defaultPaymentTerms)
        )

        contact.defaultVatRate?.let { rate ->
            ContactInfoRow(
                icon = Lucide.Banknote,
                label = stringResource(Res.string.contacts_default_vat_rate),
                value = stringResource(Res.string.common_percent_value, rate.toDisplayString())
            )
        }

        contact.tags?.let { tagsString ->
            val tags = tagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (tags.isNotEmpty()) {
                HorizontalDivider()

                Text(
                    text = stringResource(Res.string.contacts_tags),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.forEach { tag ->
                        ContactInfoTagBadge(text = tag)
                    }
                }
            }
        }
    }
}

// ============================================================================
// PREVIEWS
// ============================================================================

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ContactInfoContentPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    val now = LocalDateTime(2026, 1, 15, 10, 0)
    TestWrapper(parameters) {
        ContactInfoContent(
            contact = ContactDto(
                id = ContactId.generate(),
                tenantId = TenantId.generate(),
                name = Name("Acme Corporation"),
                email = Email("info@acme.be"),
                phone = PhoneNumber("+32 2 123 45 67"),
                vatNumber = VatNumber("BE0123456789"),
                companyNumber = "0123.456.789",
                contactPerson = "John Doe",
                defaultPaymentTerms = 30,
                tags = "client,vip",
                isActive = true,
                derivedRoles = DerivedContactRoles(
                    isCustomer = true,
                    isSupplier = true
                ),
                createdAt = now,
                updatedAt = now
            )
        )
    }
}
