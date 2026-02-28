package tech.dokus.features.cashflow.presentation.cashflow.components.invoice.create.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Calendar
import tech.dokus.features.cashflow.mvi.CreateInvoiceIntent
import tech.dokus.features.cashflow.mvi.CreateInvoiceState
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.constrains.limitWidthOperatorForm
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted

private val SectionSpacing = 26.dp
private val TopSpacing = 34.dp
private val FooterBottomPadding = 34.dp
private val LabelSpacing = 7.dp

@Composable
internal fun DesktopCreateInvoiceContent(
    state: CreateInvoiceState,
    onIntent: (CreateInvoiceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val formState = state.formState
    val uiState = state.uiState

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Constraints.Shell.contentPaddingH),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .limitWidthOperatorForm()
                .fillMaxWidth()
                .padding(top = TopSpacing, bottom = FooterBottomPadding),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing)
        ) {
            HeaderRow(
                invoiceNumber = state.invoiceNumberPreview,
                onPreview = { onIntent(CreateInvoiceIntent.SetPreviewVisible(true)) }
            )

            PartiesAndDates(
                state = state,
                onIntent = onIntent
            )

            uiState.latestInvoiceSuggestion?.let { suggestion ->
                ReuseSuggestionStrip(
                    issueDate = formatDate(suggestion.issueDate),
                    onReuse = { onIntent(CreateInvoiceIntent.ApplyLatestInvoiceLines) },
                    onDismiss = { onIntent(CreateInvoiceIntent.DismissLatestInvoiceSuggestion) }
                )
            }

            InvoiceLineItemsGrid(
                items = formState.items,
                subtotal = formState.subtotal,
                vatAmount = formState.vatAmount,
                total = formState.total,
                onAddItem = { onIntent(CreateInvoiceIntent.AddLineItem) },
                onRemoveItem = { onIntent(CreateInvoiceIntent.RemoveLineItem(it)) },
                onDescription = { id, value -> onIntent(CreateInvoiceIntent.UpdateItemDescription(id, value)) },
                onQuantity = { id, value -> onIntent(CreateInvoiceIntent.UpdateItemQuantity(id, value)) },
                onUnitPrice = { id, value -> onIntent(CreateInvoiceIntent.UpdateItemUnitPrice(id, value)) },
                onVatRate = { id, value -> onIntent(CreateInvoiceIntent.UpdateItemVatRate(id, value)) }
            )

            PaymentSection(
                state = state,
                onIntent = onIntent
            )

            NoteSection(
                note = formState.notes,
                onNote = { onIntent(CreateInvoiceIntent.UpdateNotes(it)) }
            )

            DeliverySection(
                state = state,
                onIntent = onIntent
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PButton(
                        text = "Save draft",
                        variant = PButtonVariant.Outline,
                        isEnabled = !formState.isSaving,
                        onClick = { onIntent(CreateInvoiceIntent.SaveAsDraft) }
                    )
                    PButton(
                        text = primaryActionLabel(uiState.resolvedDeliveryAction.action),
                        isEnabled = !formState.isSaving,
                        isLoading = formState.isSaving,
                        onClick = { onIntent(CreateInvoiceIntent.SubmitWithResolvedDelivery) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(
    invoiceNumber: String?,
    onPreview: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = invoiceNumber ?: "INV-0000-0000",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "New Invoice",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.textFaint
            )
        }

        PButton(
            text = "preview",
            variant = PButtonVariant.Outline,
            onClick = onPreview
        )
    }
}

@Composable
private fun PartiesAndDates(
    state: CreateInvoiceState,
    onIntent: (CreateInvoiceIntent) -> Unit
) {
    val uiState = state.uiState
    val formState = state.formState

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LabelSpacing)
            ) {
                Text(
                    text = "FROM",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.textMuted
                )
                Text(
                    text = uiState.senderCompanyName,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = uiState.senderCompanyVat ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }
            InvoiceClientLookup(
                lookupState = uiState.clientLookupState,
                onIntent = onIntent,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            DateInfoCell(
                label = "ISSUED",
                value = formatDate(formState.issueDate),
                onClick = { onIntent(CreateInvoiceIntent.OpenIssueDatePicker) },
                modifier = Modifier.weight(1f)
            )
            TermsCell(
                days = formState.paymentTermsDays,
                onUpdate = { onIntent(CreateInvoiceIntent.UpdatePaymentTermsDays(it)) },
                modifier = Modifier.weight(1f)
            )
            DateInfoCell(
                label = "DUE",
                value = formatDate(formState.dueDate),
                onClick = { onIntent(CreateInvoiceIntent.OpenDueDatePicker) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DateInfoCell(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.textMuted
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = FeatherIcons.Calendar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TermsCell(
    days: Int,
    onUpdate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(7, 14, 30, 45, 60)

    Column(
        modifier = modifier.padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "TERMS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.textMuted
        )

        Row(
            modifier = Modifier.clickable { expanded = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Net $days",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text("Net $option") },
                    onClick = {
                        onUpdate(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ReuseSuggestionStrip(
    issueDate: String,
    onReuse: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Last invoice ($issueDate)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textMuted
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reuse lines",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onReuse)
            )
            Text(
                text = "dismiss",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
                modifier = Modifier.clickable(onClick = onDismiss)
            )
        }
    }
}

internal fun formatDate(date: kotlinx.datetime.LocalDate?): String {
    if (date == null) return "--/--/----"
    val day = date.day.toString().padStart(2, '0')
    val month = date.monthNumber.toString().padStart(2, '0')
    return "$day/$month/${date.year}"
}
