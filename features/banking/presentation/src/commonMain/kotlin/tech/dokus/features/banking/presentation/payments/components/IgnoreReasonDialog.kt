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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_cancel
import tech.dokus.aura.resources.banking_ignore_dialog_confirm
import tech.dokus.aura.resources.banking_ignore_dialog_prompt
import tech.dokus.aura.resources.banking_ignore_dialog_title
import tech.dokus.domain.enums.IgnoredReason
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.components.dialog.DokusDialogSurface
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Dialog content for selecting a reason to ignore a bank transaction.
 * Uses [DokusDialogSurface] without a [Dialog] wrapper, suitable for
 * Navigation Compose `dialog<>` routes.
 */
@Composable
internal fun IgnoreReasonDialogContent(
    selectedReason: IgnoredReason?,
    onReasonSelected: (IgnoredReason) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    DokusDialogSurface(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.banking_ignore_dialog_title),
        content = {
            Column {
                Text(
                    text = stringResource(Res.string.banking_ignore_dialog_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Constraints.Spacing.large))
                IgnoredReason.entries.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onReasonSelected(reason) }
                            .padding(vertical = Constraints.Spacing.xSmall),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = reason == selectedReason,
                            onClick = { onReasonSelected(reason) },
                        )
                        Text(
                            text = reason.localized,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = Constraints.Spacing.small),
                        )
                    }
                }
            }
        },
        primaryAction = DokusDialogAction(
            text = stringResource(Res.string.banking_ignore_dialog_confirm),
            onClick = onConfirm,
            enabled = selectedReason != null,
        ),
        secondaryAction = DokusDialogAction(
            text = stringResource(Res.string.action_cancel),
            onClick = onDismiss,
        ),
    )
}

@Preview(name = "Ignore Reason Dialog")
@Composable
private fun IgnoreReasonDialogPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        IgnoreReasonDialogContent(
            selectedReason = IgnoredReason.BankFee,
            onReasonSelected = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}
