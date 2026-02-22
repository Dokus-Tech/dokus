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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tech.dokus.features.cashflow.presentation.review.PaymentSheetState
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

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
                            Text(error)
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
