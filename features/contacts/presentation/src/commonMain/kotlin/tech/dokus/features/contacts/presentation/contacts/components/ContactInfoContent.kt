package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import tech.dokus.aura.resources.contacts_business_info
import tech.dokus.aura.resources.contacts_company_number
import tech.dokus.aura.resources.contacts_contact_person
import tech.dokus.aura.resources.contacts_default_vat_rate
import tech.dokus.aura.resources.contacts_email
import tech.dokus.aura.resources.contacts_payment_defaults
import tech.dokus.aura.resources.contacts_payment_terms
import tech.dokus.aura.resources.contacts_payment_terms_value
import tech.dokus.aura.resources.contacts_peppol_enabled
import tech.dokus.aura.resources.contacts_peppol_settings
import tech.dokus.aura.resources.contacts_phone
import tech.dokus.aura.resources.contacts_tags
import tech.dokus.aura.resources.contacts_vat_number
import tech.dokus.domain.model.contact.ContactDto

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ContactInfoContent(
    contact: ContactDto,
    onPeppolToggle: (Boolean) -> Unit,
    isTogglingPeppol: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = contact.name.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

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
                icon = Icons.Default.Email,
                label = stringResource(Res.string.contacts_email),
                value = it.value
            )
        }

        contact.phone?.let {
            ContactInfoRow(
                icon = Icons.Default.Phone,
                label = stringResource(Res.string.contacts_phone),
                value = it
            )
        }

        contact.contactPerson?.let {
            ContactInfoRow(
                icon = Icons.Default.Person,
                label = stringResource(Res.string.contacts_contact_person),
                value = it
            )
        }

        if (contact.vatNumber != null || contact.companyNumber != null) {
            HorizontalDivider()

            Text(
                text = stringResource(Res.string.contacts_business_info),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            contact.vatNumber?.let {
                ContactInfoRow(
                    icon = Icons.Default.Receipt,
                    label = stringResource(Res.string.contacts_vat_number),
                    value = it.value
                )
            }

            contact.companyNumber?.let {
                ContactInfoRow(
                    icon = Icons.Default.Business,
                    label = stringResource(Res.string.contacts_company_number),
                    value = it
                )
            }
        }

        val hasAddress = listOfNotNull(
            contact.addressLine1,
            contact.addressLine2,
            contact.city,
            contact.postalCode,
            contact.country
        ).isNotEmpty()

        if (hasAddress) {
            HorizontalDivider()

            Text(
                text = stringResource(Res.string.contacts_address),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                icon = Icons.Default.LocationOn,
                label = stringResource(Res.string.contacts_address),
                value = addressLines.joinToString("\n")
            )
        }

        HorizontalDivider()

        Text(
            text = stringResource(Res.string.contacts_peppol_settings),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column {
                    Text(
                        text = stringResource(Res.string.contacts_peppol_enabled),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    contact.peppolId?.let { id ->
                        Text(
                            text = id,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Switch(
                checked = contact.peppolEnabled,
                onCheckedChange = onPeppolToggle,
                enabled = !isTogglingPeppol && contact.peppolId != null
            )
        }

        HorizontalDivider()

        Text(
            text = stringResource(Res.string.contacts_payment_defaults),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ContactInfoRow(
            icon = Icons.Default.Schedule,
            label = stringResource(Res.string.contacts_payment_terms),
            value = stringResource(Res.string.contacts_payment_terms_value, contact.defaultPaymentTerms)
        )

        contact.defaultVatRate?.let { rate ->
            ContactInfoRow(
                icon = Icons.Default.Payments,
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
                    style = MaterialTheme.typography.labelLarge,
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
