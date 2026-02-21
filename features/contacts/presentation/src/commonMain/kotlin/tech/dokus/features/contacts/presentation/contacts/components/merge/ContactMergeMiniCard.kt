package tech.dokus.features.contacts.presentation.contacts.components.merge

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
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
import tech.dokus.aura.resources.common_empty_value
import tech.dokus.aura.resources.contacts_company_number
import tech.dokus.aura.resources.contacts_contact_person
import tech.dokus.aura.resources.contacts_email
import tech.dokus.aura.resources.contacts_phone
import tech.dokus.aura.resources.contacts_vat_number
import tech.dokus.domain.model.contact.ContactDto

@Composable
internal fun ContactMergeMiniCard(
    contact: ContactDto,
    isSelected: Boolean,
    onClick: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = contact.name.value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        val vatNumber = contact.vatNumber?.value ?: stringResource(Res.string.common_empty_value)
        val companyNumber = contact.companyNumber ?: stringResource(Res.string.common_empty_value)
        val contactPerson = contact.contactPerson ?: stringResource(Res.string.common_empty_value)
        val phone = contact.phone?.value ?: stringResource(Res.string.common_empty_value)
        val email = contact.email?.value ?: stringResource(Res.string.common_empty_value)

        ContactMergeMiniRow(label = stringResource(Res.string.contacts_vat_number), value = vatNumber)
        ContactMergeMiniRow(label = stringResource(Res.string.contacts_company_number), value = companyNumber)
        ContactMergeMiniRow(label = stringResource(Res.string.contacts_contact_person), value = contactPerson)
        ContactMergeMiniRow(label = stringResource(Res.string.contacts_phone), value = phone)
        ContactMergeMiniRow(label = stringResource(Res.string.contacts_email), value = email)
    }
}

@Composable
private fun ContactMergeMiniRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ============================================================================
// PREVIEWS
// ============================================================================

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ContactMergeMiniCardPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    val now = kotlinx.datetime.LocalDateTime(2026, 1, 15, 10, 0)
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        ContactMergeMiniCard(
            contact = ContactDto(
                id = tech.dokus.domain.ids.ContactId.generate(),
                tenantId = tech.dokus.domain.ids.TenantId.generate(),
                name = tech.dokus.domain.Name("Acme Corporation"),
                email = tech.dokus.domain.Email("info@acme.be"),
                vatNumber = tech.dokus.domain.ids.VatNumber("BE0123456789"),
                companyNumber = "0123.456.789",
                contactPerson = "John Doe",
                phone = tech.dokus.domain.PhoneNumber("+32 2 123 45 67"),
                createdAt = now,
                updatedAt = now
            ),
            isSelected = true,
            onClick = {}
        )
    }
}
