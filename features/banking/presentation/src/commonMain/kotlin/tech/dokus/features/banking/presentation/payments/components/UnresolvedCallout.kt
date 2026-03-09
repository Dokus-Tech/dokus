package tech.dokus.features.banking.presentation.payments.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_unresolved_amount
import tech.dokus.aura.resources.banking_unresolved_callout
import tech.dokus.domain.Money
import tech.dokus.foundation.aura.components.common.DokusCalloutBanner
import tech.dokus.foundation.aura.style.statusWarning

@Composable
internal fun UnresolvedCallout(
    unresolvedCount: Int,
    unresolvedAmount: Money,
    modifier: Modifier = Modifier,
) {
    DokusCalloutBanner(modifier = modifier) {
        Text(
            text = stringResource(Res.string.banking_unresolved_callout, unresolvedCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(
                Res.string.banking_unresolved_amount,
                "€${unresolvedAmount.toDisplayString()}",
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.statusWarning,
        )
    }
}
