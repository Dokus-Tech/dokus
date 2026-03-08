package tech.dokus.app.screens.settings.components

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
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Generic skeleton placeholder for settings screens.
 * Shows shimmer cards matching typical collapsible-section layouts.
 */
@Composable
fun SettingsSkeleton(
    sectionCount: Int = 4,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Constraints.Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        repeat(sectionCount) {
            SettingsSectionSkeleton()
        }
    }
}

@Composable
private fun SettingsSectionSkeleton(
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Constraints.Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
        ) {
            // Section title
            ShimmerLine(modifier = Modifier.fillMaxWidth(0.4f))
            // Rows
            repeat(3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ShimmerLine(modifier = Modifier.fillMaxWidth(0.45f))
                    ShimmerLine(modifier = Modifier.fillMaxWidth(0.25f))
                }
            }
        }
    }
}

@Preview
@Composable
private fun SettingsSkeletonPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        SettingsSkeleton()
    }
}
