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
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_cancel
import tech.dokus.aura.resources.banking_transfer_confirm
import tech.dokus.aura.resources.banking_transfer_description
import tech.dokus.aura.resources.banking_transfer_empty
import tech.dokus.aura.resources.banking_transfer_title
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
        title = stringResource(Res.string.banking_transfer_title),
        content = {
            Column {
                Text(
                    text = stringResource(Res.string.banking_transfer_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Constraints.Spacing.large))

                if (availableAccounts.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.banking_transfer_empty),
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
            text = stringResource(Res.string.banking_transfer_confirm),
            onClick = onConfirm,
            enabled = selectedAccountId != null && !isSubmitting,
            isLoading = isSubmitting,
        ),
        secondaryAction = DokusDialogAction(
            text = stringResource(Res.string.action_cancel),
            onClick = onDismiss,
        ),
        dismissOnBackPress = !isSubmitting,
        dismissOnClickOutside = !isSubmitting,
    )
}
