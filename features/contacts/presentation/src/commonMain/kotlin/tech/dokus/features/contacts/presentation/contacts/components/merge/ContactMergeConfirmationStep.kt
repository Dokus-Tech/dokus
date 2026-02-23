package tech.dokus.features.contacts.presentation.contacts.components.merge

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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_action_irreversible
import tech.dokus.aura.resources.contacts_inbound_invoices
import tech.dokus.aura.resources.contacts_expenses
import tech.dokus.aura.resources.contacts_invoices
import tech.dokus.aura.resources.contacts_merge_items_to_target
import tech.dokus.aura.resources.contacts_merge_source_archived
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.extensions.localized

@Composable
internal fun ContactMergeConfirmationStep(
    sourceContact: ContactDto,
    targetContact: ContactDto,
    sourceActivity: ContactActivitySummary?,
    mergeError: DokusException?,
) {
    Column {
        DokusCardSurface(
            modifier = Modifier.fillMaxWidth(),
            variant = DokusCardVariant.Soft,
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(Res.string.common_action_irreversible),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            Res.string.contacts_merge_source_archived,
                            sourceContact.name.value
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (sourceActivity != null) {
            DokusCardSurface(
                modifier = Modifier.fillMaxWidth(),
                variant = DokusCardVariant.Soft,
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(
                            Res.string.contacts_merge_items_to_target,
                            targetContact.name.value
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ContactMergeCountRow(
                        label = stringResource(Res.string.contacts_invoices),
                        count = sourceActivity.invoiceCount
                    )
                    ContactMergeCountRow(
                        label = stringResource(Res.string.contacts_inbound_invoices),
                        count = sourceActivity.inboundInvoiceCount
                    )
                    ContactMergeCountRow(
                        label = stringResource(Res.string.contacts_expenses),
                        count = sourceActivity.expenseCount
                    )
                }
            }
        }

        if (mergeError != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = mergeError.localized,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactMergeCountRow(label: String, count: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============================================================================
// PREVIEWS
// ============================================================================

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ContactMergeConfirmationStepPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    val now = kotlinx.datetime.LocalDateTime(2026, 1, 15, 10, 0)
    val sourceId = tech.dokus.domain.ids.ContactId.generate()
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        ContactMergeConfirmationStep(
            sourceContact = tech.dokus.domain.model.contact.ContactDto(
                id = sourceId,
                tenantId = tech.dokus.domain.ids.TenantId.generate(),
                name = tech.dokus.domain.Name("Old Company"),
                createdAt = now,
                updatedAt = now
            ),
            targetContact = tech.dokus.domain.model.contact.ContactDto(
                id = tech.dokus.domain.ids.ContactId.generate(),
                tenantId = tech.dokus.domain.ids.TenantId.generate(),
                name = tech.dokus.domain.Name("Acme Corporation"),
                createdAt = now,
                updatedAt = now
            ),
            sourceActivity = ContactActivitySummary(
                contactId = sourceId,
                invoiceCount = 5,
                invoiceTotal = "10,000.00",
                inboundInvoiceCount = 2,
                inboundInvoiceTotal = "3,000.00",
                expenseCount = 3,
                expenseTotal = "500.00"
            ),
            mergeError = null
        )
    }
}
