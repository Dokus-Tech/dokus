package tech.dokus.features.cashflow.presentation.cashflow.components.invoice.create.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import tech.dokus.domain.enums.InvoiceDeliveryMethod
import tech.dokus.features.cashflow.mvi.CreateInvoiceIntent
import tech.dokus.features.cashflow.mvi.CreateInvoiceState
import tech.dokus.features.cashflow.mvi.model.InvoiceResolvedAction
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.*
import tech.dokus.foundation.aura.components.common.PSelectableCommandCard
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted

@Composable
internal fun PaymentSection(
    state: CreateInvoiceState,
    onIntent: (CreateInvoiceIntent) -> Unit
) {
    val form = state.formState
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SectionLabel(stringResource(Res.string.invoice_payment_label))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SectionLabel(stringResource(Res.string.invoice_bank_label))
                BankSelectorField(
                    value = if (form.senderIban.isBlank()) stringResource(Res.string.invoice_bank_default) else stringResource(Res.string.invoice_bank_primary)
                )
                Text(
                    text = form.senderIban.ifBlank { "-" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.textMuted
                )
                Text(
                    text = "BIC: ${form.senderBic.ifBlank { "-" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textFaint
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SectionLabel(stringResource(Res.string.invoice_structured_reference_label))
                FlatInputField(
                    value = form.structuredCommunication,
                    onValueChange = { onIntent(CreateInvoiceIntent.UpdateStructuredCommunication(it)) },
                    singleLine = true,
                    placeholder = stringResource(Res.string.invoice_structured_reference_placeholder)
                )
                Text(
                    text = stringResource(Res.string.invoice_belgian_structured_communication),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textFaint
                )
            }
        }
    }
}

@Composable
internal fun NoteSection(
    note: String,
    onNote: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel(stringResource(Res.string.invoice_note_label))
            Text(
                text = stringResource(Res.string.button_reset),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textFaint,
                modifier = Modifier.clickable { onNote("") }
            )
        }

        FlatInputField(
            value = note,
            onValueChange = onNote,
            singleLine = false,
            placeholder = stringResource(Res.string.invoice_payment_terms_placeholder),
            minHeight = 88.dp
        )
    }
}

@Composable
internal fun DeliverySection(
    state: CreateInvoiceState,
    onIntent: (CreateInvoiceIntent) -> Unit
) {
    val uiState = state.uiState
    var showOtherMethods by remember(uiState.selectedDeliveryPreference) {
        mutableStateOf(uiState.selectedDeliveryPreference == InvoiceDeliveryMethod.PdfExport)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SectionLabel(stringResource(Res.string.invoice_delivery_label))

        PSelectableCommandCard(
            title = stringResource(Res.string.delivery_send_peppol),
            subtitle = stringResource(Res.string.delivery_peppol_description),
            selected = uiState.selectedDeliveryPreference == InvoiceDeliveryMethod.Peppol,
            onClick = {
                onIntent(CreateInvoiceIntent.SelectDeliveryPreference(InvoiceDeliveryMethod.Peppol))
            },
            badge = stringResource(Res.string.invoice_recommended),
            reason = if (
                uiState.selectedDeliveryPreference == InvoiceDeliveryMethod.Peppol &&
                    uiState.resolvedDeliveryAction.action == InvoiceResolvedAction.PdfExport
            ) {
                uiState.resolvedDeliveryAction.reason
            } else {
                null
            }
        )

        Text(
            text = "\u203a " + if (showOtherMethods) stringResource(Res.string.invoice_hide_other_methods) else stringResource(Res.string.invoice_other_methods),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.textFaint,
            modifier = Modifier.clickable { showOtherMethods = !showOtherMethods }
        )

        if (showOtherMethods) {
            PSelectableCommandCard(
                title = stringResource(Res.string.delivery_export_pdf),
                subtitle = stringResource(Res.string.delivery_pdf_description),
                selected = uiState.selectedDeliveryPreference == InvoiceDeliveryMethod.PdfExport,
                onClick = {
                    onIntent(CreateInvoiceIntent.SelectDeliveryPreference(InvoiceDeliveryMethod.PdfExport))
                }
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.textMuted
    )
}

@Composable
private fun BankSelectorField(value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FlatInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    minHeight: androidx.compose.ui.unit.Dp = 48.dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.TopStart
    ) {
        if (value.isBlank()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.textFaint
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

internal fun primaryActionLabel(action: InvoiceResolvedAction): String {
    return when (action) {
        InvoiceResolvedAction.Peppol -> "Send via PEPPOL"
        InvoiceResolvedAction.PdfExport -> "Export PDF"
    }
}
