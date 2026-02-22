package tech.dokus.features.cashflow.presentation.cashflow.components.invoice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.invoice_details_title
import tech.dokus.aura.resources.invoice_notes_optional
import tech.dokus.aura.resources.invoice_save_as_draft
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.cashflow.mvi.model.CreateInvoiceFormState
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

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
    DokusCard(
        modifier = modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.invoice_details_title),
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
                fieldName = stringResource(Res.string.invoice_notes_optional),
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
                    text = error.localized,
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
                    text = stringResource(Res.string.invoice_save_as_draft),
                    variant = PButtonVariant.Default,
                    onClick = onSaveAsDraft,
                    isEnabled = formState.isValid && !formState.isSaving
                )
            }
        }
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun InvoiceFormCardPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        InvoiceFormCard(
            formState = Mocks.sampleFormState,
            clientsState = DokusState.success<List<ContactDto>>(listOf(Mocks.sampleClient)),
            saveState = DokusState.idle<Unit>(),
            onSelectClient = {},
            onUpdateNotes = {},
            onAddLineItem = {},
            onRemoveLineItem = {},
            onUpdateItemDescription = { _, _ -> },
            onUpdateItemQuantity = { _, _ -> },
            onUpdateItemUnitPrice = { _, _ -> },
            onUpdateItemVatRate = { _, _ -> },
            onSaveAsDraft = {}
        )
    }
}
