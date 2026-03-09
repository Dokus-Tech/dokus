package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_cancel
import tech.dokus.aura.resources.common_unknown
import tech.dokus.aura.resources.payment_choose_different
import tech.dokus.aura.resources.payment_choose_from_transactions
import tech.dokus.aura.resources.payment_close_list
import tech.dokus.aura.resources.payment_counterparty
import tech.dokus.aura.resources.payment_reference
import tech.dokus.aura.resources.payment_transaction_date_amount
import tech.dokus.aura.resources.payment_confirm
import tech.dokus.aura.resources.payment_date_label
import tech.dokus.aura.resources.payment_imported_transaction
import tech.dokus.aura.resources.payment_loading_transactions
import tech.dokus.aura.resources.payment_no_transactions
import tech.dokus.aura.resources.payment_note_label
import tech.dokus.aura.resources.payment_record_title
import tech.dokus.aura.resources.payment_suggested_match
import tech.dokus.aura.resources.payment_use_manual_entry
import tech.dokus.aura.resources.state_saving
import tech.dokus.aura.resources.payment_amount_label
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.features.cashflow.presentation.review.PaymentSheetState
import tech.dokus.foundation.aura.components.PDatePickerDialog
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
    onSelectTransaction: (BankTransactionId) -> Unit,
    onClearSelectedTransaction: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.payment_record_title)) },
        text = {
            RecordPaymentDialogBody(
                sheetState = sheetState,
                currencySign = currencySign,
                onPaidAtChange = onPaidAtChange,
                onAmountChange = onAmountChange,
                onNoteChange = onNoteChange,
                onOpenTransactionPicker = onOpenTransactionPicker,
                onCloseTransactionPicker = onCloseTransactionPicker,
                onSelectTransaction = onSelectTransaction,
                onClearSelectedTransaction = onClearSelectedTransaction,
            )
        },
        confirmButton = {
            Row(
                modifier = Modifier.padding(end = Constraints.Spacing.small),
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                OutlinedButton(onClick = onDismiss, enabled = !sheetState.isSubmitting) {
                    Text(stringResource(Res.string.action_cancel))
                }
                Button(onClick = onSubmit, enabled = !sheetState.isSubmitting) {
                    Text(if (sheetState.isSubmitting) stringResource(Res.string.state_saving) else stringResource(Res.string.payment_confirm))
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
    onSelectTransaction: (BankTransactionId) -> Unit,
    onClearSelectedTransaction: () -> Unit,
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
            Text(stringResource(Res.string.payment_record_title), style = MaterialTheme.typography.headlineSmall)
            RecordPaymentDialogBody(
                sheetState = sheetState,
                currencySign = currencySign,
                onPaidAtChange = onPaidAtChange,
                onAmountChange = onAmountChange,
                onNoteChange = onNoteChange,
                onOpenTransactionPicker = onOpenTransactionPicker,
                onCloseTransactionPicker = onCloseTransactionPicker,
                onSelectTransaction = onSelectTransaction,
                onClearSelectedTransaction = onClearSelectedTransaction,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small, Alignment.End),
            ) {
                OutlinedButton(onClick = onDismiss, enabled = !sheetState.isSubmitting) {
                    Text(stringResource(Res.string.action_cancel))
                }
                Button(onClick = onSubmit, enabled = !sheetState.isSubmitting) {
                    Text(if (sheetState.isSubmitting) stringResource(Res.string.state_saving) else stringResource(Res.string.payment_confirm))
                }
            }
        }
    }
}

@Composable
private fun RecordPaymentDialogBody(
    sheetState: PaymentSheetState,
    currencySign: String,
    onPaidAtChange: (LocalDate) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onOpenTransactionPicker: () -> Unit,
    onCloseTransactionPicker: () -> Unit,
    onSelectTransaction: (BankTransactionId) -> Unit,
    onClearSelectedTransaction: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        OutlinedTextField(
            value = sheetState.paidAt.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(Res.string.payment_date_label)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth(),
            interactionSource = remember { MutableInteractionSource() }
                .also { source ->
                    LaunchedEffect(source) {
                        source.interactions.collect { interaction ->
                            if (interaction is PressInteraction.Release) {
                                showDatePicker = true
                            }
                        }
                    }
                },
        )

        if (showDatePicker) {
            PDatePickerDialog(
                initialDate = sheetState.paidAt,
                onDateSelected = { date ->
                    if (date != null) {
                        onPaidAtChange(date)
                    }
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false },
            )
        }

        OutlinedTextField(
            value = sheetState.amountText,
            onValueChange = onAmountChange,
            label = { Text(stringResource(Res.string.payment_amount_label, currencySign)) },
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
            label = { Text(stringResource(Res.string.payment_note_label)) },
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
}

@Composable
private fun PaymentTransactionSection(
    sheetState: PaymentSheetState,
    onOpenTransactionPicker: () -> Unit,
    onCloseTransactionPicker: () -> Unit,
    onSelectTransaction: (BankTransactionId) -> Unit,
    onClearSelectedTransaction: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.payment_imported_transaction),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.textMuted,
            )
            if (
                sheetState.suggestedTransaction != null &&
                sheetState.selectedTransaction == sheetState.suggestedTransaction
            ) {
                Text(
                    text = stringResource(Res.string.payment_suggested_match),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (sheetState.isLoadingTransactions) {
            Text(
                text = stringResource(Res.string.payment_loading_transactions),
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
                text = stringResource(
                    Res.string.payment_transaction_date_amount,
                    selected.transactionDate.toString(),
                    selected.signedAmount.toDisplayString(),
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            selected.counterpartyName?.takeIf { it.isNotBlank() }?.let {
                Text(stringResource(Res.string.payment_counterparty, it), style = MaterialTheme.typography.bodySmall)
            }
            selected.structuredCommunicationRaw?.takeIf { it.isNotBlank() }?.let {
                Text(stringResource(Res.string.payment_reference, it), style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)) {
                if (sheetState.selectableTransactions.isNotEmpty()) {
                    OutlinedButton(onClick = onOpenTransactionPicker) {
                        Text(stringResource(Res.string.payment_choose_different))
                    }
                }
                OutlinedButton(onClick = onClearSelectedTransaction) {
                    Text(stringResource(Res.string.payment_use_manual_entry))
                }
            }
        } else if (sheetState.selectableTransactions.isNotEmpty()) {
            OutlinedButton(onClick = onOpenTransactionPicker) {
                Text(stringResource(Res.string.payment_choose_from_transactions))
            }
        } else {
            Text(
                text = stringResource(Res.string.payment_no_transactions),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
        }

        if (sheetState.showTransactionPicker && sheetState.selectableTransactions.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(sheetState.selectableTransactions) { transaction ->
                        OutlinedButton(
                            onClick = { onSelectTransaction(transaction.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "${transaction.transactionDate} \u2022 ${transaction.signedAmount.toDisplayString()} \u2022 " +
                                    (transaction.counterpartyName ?: stringResource(Res.string.common_unknown))
                            )
                        }
                    }
                }
                OutlinedButton(onClick = onCloseTransactionPicker) {
                    Text(stringResource(Res.string.payment_close_list))
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

@Preview
@Composable
private fun RecordPaymentDialogSuggestedTransactionPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        RecordPaymentDialogContent(
            sheetState = previewPaymentSheetState(withSuggestedTransaction = true),
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
private fun RecordPaymentDialogTransactionPickerPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        RecordPaymentDialogContent(
            sheetState = previewPaymentSheetState(withTransactionPicker = true),
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
