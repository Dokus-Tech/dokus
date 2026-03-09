package tech.dokus.features.cashflow.presentation.documents.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.features.cashflow.presentation.common.components.table.DokusTableDivider
import tech.dokus.foundation.aura.components.common.ShimmerBox
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Skeleton placeholder for the documents list screen.
 * Matches the Content layout: filter tabs row + document rows.
 */
@Composable
internal fun DocumentsSkeleton(
    rowCount: Int = 5,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Filter tabs skeleton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.large),
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
        ) {
            repeat(3) {
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = Constraints.Spacing.small),
                )
            }
        }

        DokusTableDivider()

        // Document row skeletons
        repeat(rowCount) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Constraints.Spacing.large,
                        vertical = Constraints.Spacing.medium,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
                ) {
                    ShimmerLine(modifier = Modifier.fillMaxWidth(0.5f))
                    ShimmerLine(modifier = Modifier.fillMaxWidth(0.3f))
                }
                ShimmerLine(modifier = Modifier.fillMaxWidth(0.15f))
            }
            if (index < rowCount - 1) {
                DokusTableDivider()
            }
        }
    }
}

@Preview
@Composable
private fun DocumentsSkeletonPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DocumentsSkeleton()
    }
}
