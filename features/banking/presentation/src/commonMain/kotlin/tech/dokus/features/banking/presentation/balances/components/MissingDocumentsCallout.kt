package tech.dokus.features.banking.presentation.balances.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_balances_missing_callout
import tech.dokus.aura.resources.banking_balances_review_now
import tech.dokus.foundation.aura.components.common.DokusCalloutBanner
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun MissingDocumentsCallout(
    count: Int,
    modifier: Modifier = Modifier,
) {
    DokusCalloutBanner(
        title = stringResource(Res.string.banking_balances_missing_callout, count),
        modifier = modifier,
        trailing = {
            TextButton(onClick = { /* TODO: navigate to Payments */ }) {
                Text(
                    text = stringResource(Res.string.banking_balances_review_now),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        },
    )
}

@Preview(name = "Missing Documents Callout")
@Composable
private fun MissingDocumentsCalloutPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        MissingDocumentsCallout(count = 3)
    }
}
