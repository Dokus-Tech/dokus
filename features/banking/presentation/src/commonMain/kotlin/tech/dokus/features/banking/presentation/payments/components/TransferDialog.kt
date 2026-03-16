package tech.dokus.features.banking.presentation.payments.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.model.BankAccountDto
import tech.dokus.foundation.aura.components.dialog.DokusDialog
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

/**
 * Dialog for selecting the destination account when marking a transaction as an internal transfer.
 * Lists all owned accounts except the source account.
 */
@Composable
internal fun TransferDialog(
    availableAccounts: List<BankAccountDto>,
    selectedAccountId: BankAccountId?,
    isSubmitting: Boolean,
    onAccountSelected: (BankAccountId) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    DokusDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = "Mark as transfer",
        content = {
            Column {
                Text(
                    text = "Select the destination account for this internal transfer:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Constraints.Spacing.large))

                if (availableAccounts.isEmpty()) {
                    Text(
                        text = "No other accounts available. Upload a bank statement first to create accounts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textMuted,
                    )
                } else {
                    availableAccounts.forEach { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isSubmitting) { onAccountSelected(account.id) }
                                .padding(vertical = Constraints.Spacing.xSmall),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = account.id == selectedAccountId,
                                onClick = { onAccountSelected(account.id) },
                                enabled = !isSubmitting,
                            )
                            Column(modifier = Modifier.padding(start = Constraints.Spacing.small)) {
                                Text(
                                    text = account.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                account.iban?.let { iban ->
                                    Text(
                                        text = iban.value,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.textMuted,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        primaryAction = DokusDialogAction(
            text = "Confirm transfer",
            onClick = onConfirm,
            enabled = selectedAccountId != null && !isSubmitting,
            isLoading = isSubmitting,
        ),
        secondaryAction = DokusDialogAction(
            text = "Cancel",
            onClick = onDismiss,
        ),
        dismissOnBackPress = !isSubmitting,
        dismissOnClickOutside = !isSubmitting,
    )
}
