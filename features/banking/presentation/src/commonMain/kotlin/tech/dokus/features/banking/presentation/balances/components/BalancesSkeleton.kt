package tech.dokus.features.banking.presentation.balances.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.components.common.ShimmerBox
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun BalancesSkeleton(
    rowCount: Int = 3,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Header row skeleton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Constraints.Spacing.large,
                    vertical = Constraints.Spacing.medium,
                ),
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .height(Constraints.Spacing.small),
            )
            ShimmerBox(
                modifier = Modifier
                    .weight(0.5f)
                    .height(Constraints.Spacing.small),
            )
            ShimmerBox(
                modifier = Modifier
                    .weight(0.7f)
                    .height(Constraints.Spacing.small),
            )
            ShimmerBox(
                modifier = Modifier
                    .weight(0.5f)
                    .height(Constraints.Spacing.small),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Data rows
        repeat(rowCount) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Constraints.Spacing.large,
                        vertical = Constraints.Spacing.medium,
                    ),
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(Constraints.Spacing.medium),
                )
                ShimmerBox(
                    modifier = Modifier
                        .weight(0.5f)
                        .height(Constraints.Spacing.medium),
                )
                ShimmerBox(
                    modifier = Modifier
                        .weight(0.7f)
                        .height(Constraints.Spacing.medium),
                )
                ShimmerBox(
                    modifier = Modifier
                        .weight(0.5f)
                        .height(Constraints.Spacing.medium),
                )
            }
            if (index < rowCount - 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Preview(name = "Balances Skeleton")
@Composable
private fun BalancesSkeletonPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        BalancesSkeleton()
    }
}
