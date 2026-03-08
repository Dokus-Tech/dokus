package tech.dokus.app.screens.accountant.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.common.ShimmerCircle
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.constrains.Constraints

/**
 * Skeleton placeholder for the console clients list.
 */
@Composable
fun ConsoleClientsSkeleton(
    rowCount: Int = 4,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        // Search bar skeleton
        ShimmerLine(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Constraints.Spacing.small),
        )

        // Client row skeletons
        repeat(rowCount) {
            DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Constraints.Spacing.large),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
                ) {
                    ShimmerCircle(size = Constraints.AvatarSize.small)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
                    ) {
                        ShimmerLine(modifier = Modifier.fillMaxWidth(0.6f))
                        ShimmerLine(modifier = Modifier.fillMaxWidth(0.35f))
                    }
                }
            }
        }
    }
}
