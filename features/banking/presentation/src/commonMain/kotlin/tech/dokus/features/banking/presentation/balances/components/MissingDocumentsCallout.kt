package tech.dokus.features.banking.presentation.balances.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_balances_missing_callout
import tech.dokus.aura.resources.banking_balances_review_now
import tech.dokus.foundation.aura.components.common.DokusCalloutBanner

@Composable
internal fun MissingDocumentsCallout(
    count: Int,
    modifier: Modifier = Modifier,
) {
    DokusCalloutBanner(modifier = modifier) {
        Text(
            text = stringResource(Res.string.banking_balances_missing_callout, count),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { /* TODO: navigate to Payments */ }) {
            Text(
                text = stringResource(Res.string.banking_balances_review_now),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
