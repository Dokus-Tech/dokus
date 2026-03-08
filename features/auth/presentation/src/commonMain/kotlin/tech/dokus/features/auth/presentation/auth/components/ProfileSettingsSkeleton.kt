package tech.dokus.features.auth.presentation.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
 * Skeleton placeholder for the profile settings screen.
 * Matches the Viewing layout structure: avatar hero + settings cards.
 */
@Composable
internal fun ProfileSettingsSkeleton(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        // Avatar hero skeleton
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShimmerCircle(size = Constraints.AvatarSize.large)
            ShimmerLine(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .padding(top = Constraints.Spacing.medium),
            )
            ShimmerLine(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .padding(top = Constraints.Spacing.small),
            )
        }

        // Account card skeleton
        SettingsCardSkeleton(rowCount = 3)

        // Security card skeleton
        SettingsCardSkeleton(rowCount = 2)

        // Server card skeleton
        SettingsCardSkeleton(rowCount = 2)

        // Danger zone card skeleton
        SettingsCardSkeleton(rowCount = 1)
    }
}

/**
 * Generic skeleton for a settings-style card with shimmer rows.
 */
@Composable
internal fun SettingsCardSkeleton(
    rowCount: Int,
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Constraints.Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
        ) {
            // Card title
            ShimmerLine(modifier = Modifier.fillMaxWidth(0.35f))
            // Rows
            repeat(rowCount) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShimmerLine(modifier = Modifier.fillMaxWidth(0.4f))
                    ShimmerLine(
                        modifier = Modifier
                            .fillMaxWidth(0.3f)
                            .height(Constraints.Height.shimmerLine),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ProfileSettingsSkeletonPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ProfileSettingsSkeleton()
    }
}
