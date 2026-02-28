package tech.dokus.features.cashflow.presentation.cashflow.components.invoice.create.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import tech.dokus.domain.enums.InvoiceDeliveryMethod
import tech.dokus.features.cashflow.mvi.CreateInvoiceIntent
import tech.dokus.features.cashflow.mvi.CreateInvoiceState
import tech.dokus.features.cashflow.mvi.model.InvoiceResolvedAction
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
        SectionLabel("PAYMENT")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SectionLabel("BANK")
                BankSelectorField(
                    value = if (form.senderIban.isBlank()) "Default bank account" else "Primary account"
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
                SectionLabel("STRUCTURED REFERENCE")
                FlatInputField(
                    value = form.structuredCommunication,
                    onValueChange = { onIntent(CreateInvoiceIntent.UpdateStructuredCommunication(it)) },
                    singleLine = true,
                    placeholder = "+++000/000/000/00+++"
                )
                Text(
                    text = "Belgian structured communication",
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
            SectionLabel("NOTE")
            Text(
                text = "reset",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textFaint,
                modifier = Modifier.clickable { onNote("") }
            )
        }

        FlatInputField(
            value = note,
            onValueChange = onNote,
            singleLine = false,
            placeholder = "Payment terms...",
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
        SectionLabel("DELIVERY")

        PSelectableCommandCard(
            title = "Send via PEPPOL",
            subtitle = "E-invoice to client's accounting system",
            selected = uiState.selectedDeliveryPreference == InvoiceDeliveryMethod.Peppol,
            onClick = {
                onIntent(CreateInvoiceIntent.SelectDeliveryPreference(InvoiceDeliveryMethod.Peppol))
            },
            badge = "RECOMMENDED",
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
            text = if (showOtherMethods) "\u203a Hide other methods" else "\u203a Other methods",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.textFaint,
            modifier = Modifier.clickable { showOtherMethods = !showOtherMethods }
        )

        if (showOtherMethods) {
            PSelectableCommandCard(
                title = "Export PDF",
                subtitle = "Download invoice PDF",
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
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface
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
            .height(minHeight)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
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
