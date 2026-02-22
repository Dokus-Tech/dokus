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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
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
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record payment") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                Text(
                    text = "Payment date: ${sheetState.paidAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
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
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
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
                Text(
                    text = "Payment date: ${sheetState.paidAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
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

@Preview
@Composable
private fun RecordPaymentDialogPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        RecordPaymentDialogContent(
            sheetState = previewPaymentSheetState(),
            currencySign = "\u20AC",
            onAmountChange = {},
            onNoteChange = {},
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
            onAmountChange = {},
            onNoteChange = {},
            onSubmit = {},
            onDismiss = {},
        )
    }
}
