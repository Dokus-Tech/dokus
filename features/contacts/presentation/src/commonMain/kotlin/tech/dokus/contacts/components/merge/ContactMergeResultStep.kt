package tech.dokus.contacts.components.merge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_bills
import tech.dokus.aura.resources.contacts_expenses
import tech.dokus.aura.resources.contacts_invoices
import tech.dokus.aura.resources.contacts_merge_bills_reassigned
import tech.dokus.aura.resources.contacts_merge_expenses_reassigned
import tech.dokus.aura.resources.contacts_merge_invoices_reassigned
import tech.dokus.aura.resources.contacts_merge_notes_reassigned
import tech.dokus.aura.resources.contacts_merge_success_message
import tech.dokus.aura.resources.contacts_merge_summary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactMergeResult

@Composable
internal fun ContactMergeResultStep(
    result: ContactMergeResult,
    targetContact: ContactDto?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(Res.string.contacts_merge_success_message),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        targetContact?.let { target ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.contacts_merge_summary, target.name.value),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContactMergeReassignmentRow(
                    label = stringResource(Res.string.contacts_invoices),
                    count = result.invoicesReassigned
                )
                ContactMergeReassignmentRow(
                    label = stringResource(Res.string.contacts_bills),
                    count = result.billsReassigned
                )
                ContactMergeReassignmentRow(
                    label = stringResource(Res.string.contacts_expenses),
                    count = result.expensesReassigned
                )
                ContactMergeReassignmentRow(
                    label = stringResource(Res.string.contacts_merge_notes_reassigned),
                    count = result.notesReassigned
                )
            }
        }

        if (result.invoicesReassigned > 0 || result.billsReassigned > 0 || result.expensesReassigned > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (result.invoicesReassigned > 0) {
                    Text(
                        text = stringResource(Res.string.contacts_merge_invoices_reassigned, result.invoicesReassigned),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (result.billsReassigned > 0) {
                    Text(
                        text = stringResource(Res.string.contacts_merge_bills_reassigned, result.billsReassigned),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (result.expensesReassigned > 0) {
                    Text(
                        text = stringResource(Res.string.contacts_merge_expenses_reassigned, result.expensesReassigned),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactMergeReassignmentRow(
    label: String,
    count: Int,
) {
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
