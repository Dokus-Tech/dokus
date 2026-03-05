package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import kotlinx.datetime.LocalDate
import tech.dokus.domain.ids.ImportedBankTransactionId
import tech.dokus.features.cashflow.presentation.review.PaymentSheetState
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun RecordPaymentDialog(
    sheetState: PaymentSheetState,
    currencySign: String,
    onPaidAtChange: (LocalDate) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onOpenTransactionPicker: () -> Unit,
    onCloseTransactionPicker: () -> Unit,
    onSelectTransaction: (ImportedBankTransactionId) -> Unit,
    onClearSelectedTransaction: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    var paidAtText by remember(sheetState.paidAt) { mutableStateOf(sheetState.paidAt.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record payment") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                OutlinedTextField(
                    value = paidAtText,
                    onValueChange = { value ->
                        paidAtText = value
                        runCatching { LocalDate.parse(value) }
                            .getOrNull()
                            ?.let(onPaidAtChange)
                    },
                    label = { Text("Payment date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = sheetState.amountText,
                    onValueChange = onAmountChange,
                    label = { Text("Amount ($currencySign)") },
                    isError = sheetState.amountError != null,
                    supportingText = {
                        val error = sheetState.amountError
                        if (error != null) {
                            Text(error.localized)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = sheetState.note,
                    onValueChange = onNoteChange,
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
                PaymentTransactionSection(
                    sheetState = sheetState,
                    onOpenTransactionPicker = onOpenTransactionPicker,
                    onCloseTransactionPicker = onCloseTransactionPicker,
                    onSelectTransaction = onSelectTransaction,
                    onClearSelectedTransaction = onClearSelectedTransaction,
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.padding(end = Constraints.Spacing.small),
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                OutlinedButton(onClick = onDismiss, enabled = !sheetState.isSubmitting) {
                    Text("Cancel")
                }
                Button(onClick = onSubmit, enabled = !sheetState.isSubmitting) {
                    Text(if (sheetState.isSubmitting) "Saving\u2026" else "Confirm payment")
                }
            }
        },
    )
}

@Composable
private fun RecordPaymentDialogContent(
    sheetState: PaymentSheetState,
    currencySign: String,
    onPaidAtChange: (LocalDate) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onOpenTransactionPicker: () -> Unit,
    onCloseTransactionPicker: () -> Unit,
    onSelectTransaction: (ImportedBankTransactionId) -> Unit,
    onClearSelectedTransaction: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    var paidAtText by remember(sheetState.paidAt) { mutableStateOf(sheetState.paidAt.toString()) }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = Constraints.Elevation.modal,
    ) {
        Column(
            modifier = Modifier.padding(Constraints.Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
        ) {
            Text("Record payment", style = MaterialTheme.typography.headlineSmall)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                OutlinedTextField(
                    value = paidAtText,
                    onValueChange = { value ->
                        paidAtText = value
                        runCatching { LocalDate.parse(value) }
                            .getOrNull()
                            ?.let(onPaidAtChange)
                    },
                    label = { Text("Payment date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = sheetState.amountText,
                    onValueChange = onAmountChange,
                    label = { Text("Amount ($currencySign)") },
                    isError = sheetState.amountError != null,
                    supportingText = {
                        val error = sheetState.amountError
                        if (error != null) {
                            Text(error.localized)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = sheetState.note,
                    onValueChange = onNoteChange,
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
                PaymentTransactionSection(
                    sheetState = sheetState,
                    onOpenTransactionPicker = onOpenTransactionPicker,
                    onCloseTransactionPicker = onCloseTransactionPicker,
                    onSelectTransaction = onSelectTransaction,
                    onClearSelectedTransaction = onClearSelectedTransaction,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small, Alignment.End),
            ) {
                OutlinedButton(onClick = onDismiss, enabled = !sheetState.isSubmitting) {
                    Text("Cancel")
                }
                Button(onClick = onSubmit, enabled = !sheetState.isSubmitting) {
                    Text(if (sheetState.isSubmitting) "Saving\u2026" else "Confirm payment")
                }
            }
        }
    }
}

@Composable
private fun PaymentTransactionSection(
    sheetState: PaymentSheetState,
    onOpenTransactionPicker: () -> Unit,
    onCloseTransactionPicker: () -> Unit,
    onSelectTransaction: (ImportedBankTransactionId) -> Unit,
    onClearSelectedTransaction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
    ) {
        Text(
            text = "Imported transaction",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.textMuted,
        )

        if (sheetState.isLoadingTransactions) {
            Text(
                text = "Loading imported transactions...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
            return
        }

        sheetState.transactionsError?.let { error ->
            Text(
                text = error.localized,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        val selected = sheetState.selectedTransaction
        if (selected != null) {
            Text(
                text = "Date: ${selected.transactionDate}  Amount: ${selected.signedAmount.toDisplayString()}",
                style = MaterialTheme.typography.bodySmall,
            )
            selected.counterpartyName?.takeIf { it.isNotBlank() }?.let {
                Text("Counterparty: $it", style = MaterialTheme.typography.bodySmall)
            }
            selected.structuredCommunicationRaw?.takeIf { it.isNotBlank() }?.let {
                Text("Reference: $it", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)) {
                if (sheetState.selectableTransactions.isNotEmpty()) {
                    OutlinedButton(onClick = onOpenTransactionPicker) {
                        Text("Choose different")
                    }
                }
                OutlinedButton(onClick = onClearSelectedTransaction) {
                    Text("Use manual entry")
                }
            }
        } else if (sheetState.selectableTransactions.isNotEmpty()) {
            OutlinedButton(onClick = onOpenTransactionPicker) {
                Text("Choose from imported transactions")
            }
        } else {
            Text(
                text = "No imported transactions available.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
        }

        if (sheetState.showTransactionPicker && sheetState.selectableTransactions.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
            ) {
                sheetState.selectableTransactions.forEach { transaction ->
                    OutlinedButton(
                        onClick = { onSelectTransaction(transaction.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "${transaction.transactionDate} • ${transaction.signedAmount.toDisplayString()} • " +
                                (transaction.counterpartyName ?: "Unknown")
                        )
                    }
                }
                OutlinedButton(onClick = onCloseTransactionPicker) {
                    Text("Close list")
                }
            }
        }
    }
}

@Preview
@Composable
private fun RecordPaymentDialogPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        RecordPaymentDialogContent(
            sheetState = previewPaymentSheetState(),
            currencySign = "\u20AC",
            onPaidAtChange = {},
            onAmountChange = {},
            onNoteChange = {},
            onOpenTransactionPicker = {},
            onCloseTransactionPicker = {},
            onSelectTransaction = {},
            onClearSelectedTransaction = {},
            onSubmit = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun RecordPaymentDialogErrorPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        RecordPaymentDialogContent(
            sheetState = previewPaymentSheetState(withError = true),
            currencySign = "\u20AC",
            onPaidAtChange = {},
            onAmountChange = {},
            onNoteChange = {},
            onOpenTransactionPicker = {},
            onCloseTransactionPicker = {},
            onSelectTransaction = {},
            onClearSelectedTransaction = {},
            onSubmit = {},
            onDismiss = {},
        )
    }
}
