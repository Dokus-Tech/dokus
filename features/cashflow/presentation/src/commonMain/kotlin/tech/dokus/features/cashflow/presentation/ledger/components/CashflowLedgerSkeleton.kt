package tech.dokus.features.cashflow.presentation.ledger.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import tech.dokus.features.cashflow.presentation.common.components.table.DokusTableDivider
import tech.dokus.foundation.aura.constrains.Constraints

private val SkeletonBorderRadius = Constraints.CornerRadius.badge

/**
 * A skeleton placeholder box with consistent styling.
 */
@Composable
private fun SkeletonBox(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(SkeletonBorderRadius))
            .background(MaterialTheme.colorScheme.outline)
    )
}

/**
 * Skeleton for the summary section.
 * Matches CashflowSummarySection layout exactly.
 */
@Composable
internal fun CashflowSummarySkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Constraints.Spacing.large)
    ) {
        // Period label skeleton
        SkeletonBox(
            width = Constraints.Spacing.large * 5f,
            height = Constraints.Spacing.medium
        )
        Spacer(Modifier.height(Constraints.Spacing.xSmall))
        // Net amount skeleton
        SkeletonBox(
            width = Constraints.Spacing.large * 10f,
            height = Constraints.Spacing.large * 2.5f
        )
        Spacer(Modifier.height(Constraints.Spacing.xSmall))
        // Breakdown line skeleton
        SkeletonBox(
            width = Constraints.Spacing.large * 12.5f,
            height = Constraints.Spacing.medium
        )
    }
}

/**
 * Skeleton for a single table row.
 * Matches CashflowLedgerTableRow layout exactly.
 */
@Composable
private fun CashflowRowSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Constraints.Height.input)
            .padding(horizontal = Constraints.Spacing.large),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left section: date and contact
        Row(
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xLarge)
        ) {
            // Date placeholder
            SkeletonBox(
                width = Constraints.Spacing.large * 3.75f,
                height = Constraints.Spacing.medium,
                modifier = Modifier.padding(vertical = Constraints.Spacing.xLarge)
            )
            // Contact placeholder
            SkeletonBox(
                width = Constraints.Spacing.large * 7.5f,
                height = Constraints.Spacing.medium,
                modifier = Modifier.padding(vertical = Constraints.Spacing.xLarge)
            )
        }
        // Right section: amount
        SkeletonBox(
            width = Constraints.Spacing.large * 5f,
            height = Constraints.Spacing.medium,
            modifier = Modifier.padding(vertical = Constraints.Spacing.xLarge)
        )
    }
}

/**
 * Full skeleton placeholder for the cashflow ledger.
 * Matches the real content layout exactly to prevent layout shift.
 */
@Composable
internal fun CashflowLedgerSkeleton(
    showHeader: Boolean = true,
    rowCount: Int = 5,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Summary skeleton
        CashflowSummarySkeleton()
        DokusTableDivider()

        // Filter placeholder (same height as real filters)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Constraints.AvatarSize.medium)
        )
        DokusTableDivider()

        // Header skeleton (desktop only)
        if (showHeader) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Constraints.Height.input)
            )
            DokusTableDivider()
        }

        // Row skeletons
        repeat(rowCount) { index ->
            CashflowRowSkeleton()
            if (index < rowCount - 1) {
                DokusTableDivider()
            }
        }
    }
}
