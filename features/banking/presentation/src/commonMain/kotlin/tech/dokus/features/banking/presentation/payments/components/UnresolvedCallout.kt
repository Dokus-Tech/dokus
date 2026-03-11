package tech.dokus.features.banking.presentation.payments.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_unresolved_amount
import tech.dokus.aura.resources.banking_unresolved_callout
import tech.dokus.domain.Money
import tech.dokus.foundation.aura.components.common.CalloutTrailing
import tech.dokus.foundation.aura.components.common.DokusCalloutBanner
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun UnresolvedCallout(
    unresolvedCount: Int,
    unresolvedAmount: Money,
    modifier: Modifier = Modifier,
) {
    DokusCalloutBanner(
        title = stringResource(Res.string.banking_unresolved_callout, unresolvedCount),
        modifier = modifier,
        trailing = CalloutTrailing.Label(
            text = stringResource(
                Res.string.banking_unresolved_amount,
                "\u20ac${unresolvedAmount.toDisplayString()}",
            ),
        ),
    )
}

@Preview(name = "Unresolved Callout")
@Composable
private fun UnresolvedCalloutPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        UnresolvedCallout(
            unresolvedCount = 15,
            unresolvedAmount = Money.parseOrThrow("8420.50"),
        )
    }
}
