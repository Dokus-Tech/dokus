package tech.dokus.features.auth.presentation.auth.components

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
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.common.ShimmerCircle
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Skeleton placeholder for the sessions list screen.
 * Shows shimmer cards matching the session row layout.
 */
@Composable
internal fun MySessionsSkeleton(
    rowCount: Int = 3,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Constraints.Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        repeat(rowCount) {
            DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Constraints.Spacing.large),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
                ) {
                    ShimmerCircle(size = Constraints.IconSize.large)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                    ) {
                        ShimmerLine(modifier = Modifier.fillMaxWidth(0.6f))
                        ShimmerLine(modifier = Modifier.fillMaxWidth(0.4f))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun MySessionsSkeletonPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        MySessionsSkeleton()
    }
}
