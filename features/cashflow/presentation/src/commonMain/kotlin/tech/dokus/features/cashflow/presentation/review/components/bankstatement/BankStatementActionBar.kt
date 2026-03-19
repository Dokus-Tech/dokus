package tech.dokus.features.cashflow.presentation.review.components.bankstatement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.bank_statement_confirm_import
import tech.dokus.aura.resources.bank_statement_importing_summary
import tech.dokus.aura.resources.bank_statement_reject
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.constrains.Constraints

@Composable
internal fun BankStatementActionBar(
    includedCount: Int,
    netAmount: String,
    onReject: () -> Unit,
    onConfirm: () -> Unit,
    isConfirming: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Constraints.Spacing.large,
                vertical = Constraints.Spacing.medium,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        Text(
            text = stringResource(Res.string.bank_statement_importing_summary, includedCount) + " · net $netAmount",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.weight(1f))

        PButton(
            text = stringResource(Res.string.bank_statement_reject),
            onClick = onReject,
            variant = PButtonVariant.Outline,
            isEnabled = !isConfirming,
        )

        PButton(
            text = stringResource(Res.string.bank_statement_confirm_import, includedCount),
            onClick = onConfirm,
            isEnabled = includedCount > 0 && !isConfirming,
            isLoading = isConfirming,
        )
    }
}
