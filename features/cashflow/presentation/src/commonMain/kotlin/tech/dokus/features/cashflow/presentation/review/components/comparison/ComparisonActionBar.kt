package tech.dokus.features.cashflow.presentation.review.components.comparison

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.comparison_different_document
import tech.dokus.aura.resources.comparison_same_document
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.constrains.Constraints

@Composable
internal fun ComparisonActionBar(
    onSameDocument: () -> Unit,
    onDifferentDocument: () -> Unit,
    isResolving: Boolean,
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
        Spacer(modifier = Modifier.weight(1f))

        PButton(
            text = stringResource(Res.string.comparison_different_document),
            onClick = onDifferentDocument,
            variant = PButtonVariant.Outline,
            isEnabled = !isResolving,
        )

        PButton(
            text = stringResource(Res.string.comparison_same_document),
            onClick = onSameDocument,
            isEnabled = !isResolving,
            isLoading = isResolving,
        )
    }
}
