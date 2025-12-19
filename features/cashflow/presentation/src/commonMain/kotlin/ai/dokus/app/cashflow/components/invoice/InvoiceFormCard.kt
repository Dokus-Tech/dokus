package ai.dokus.app.cashflow.components.invoice

import ai.dokus.app.cashflow.viewmodel.CreateInvoiceFormState
import tech.dokus.foundation.app.state.DokusState
import ai.dokus.foundation.design.components.PButton
import ai.dokus.foundation.design.components.PButtonVariant
import ai.dokus.foundation.design.components.fields.PTextFieldStandard
import ai.dokus.foundation.domain.model.ContactDto
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Main form card for creating an invoice.
 * Contains client selector, notes, line items, and save action.
 */
@Composable
fun InvoiceFormCard(
    formState: CreateInvoiceFormState,
    clientsState: DokusState<List<ContactDto>>,
    saveState: DokusState<*>,
    onSelectClient: (ContactDto?) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onAddLineItem: () -> Unit,
    onRemoveLineItem: (String) -> Unit,
    onUpdateItemDescription: (String, String) -> Unit,
    onUpdateItemQuantity: (String, Double) -> Unit,
    onUpdateItemUnitPrice: (String, String) -> Unit,
    onUpdateItemVatRate: (String, Int) -> Unit,
    onSaveAsDraft: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Invoice Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Client selection
            InvoiceClientSelector(
                selectedClient = formState.selectedClient,
                clientsState = clientsState,
                onSelectClient = onSelectClient,
                error = formState.errors["client"]
            )

            // Notes
            PTextFieldStandard(
                fieldName = "Notes (optional)",
                value = formState.notes,
                onValueChange = onUpdateNotes,
                modifier = Modifier.fillMaxWidth()
            )

            // Line items section
            InvoiceLineItemsSection(
                items = formState.items,
                onAddItem = onAddLineItem,
                onRemoveItem = onRemoveLineItem,
                onUpdateDescription = onUpdateItemDescription,
                onUpdateQuantity = onUpdateItemQuantity,
                onUpdateUnitPrice = onUpdateItemUnitPrice,
                onUpdateVatRate = onUpdateItemVatRate,
                error = formState.errors["items"]
            )

            // Error message
            formState.errors["general"]?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (formState.isSaving || saveState is DokusState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 16.dp)
                    )
                }

                PButton(
                    text = "Save as Draft",
                    variant = PButtonVariant.Default,
                    onClick = onSaveAsDraft,
                    isEnabled = formState.isValid && !formState.isSaving
                )
            }
        }
    }
}
