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
import androidx.compose.ui.unit.dp
import tech.dokus.features.cashflow.presentation.common.components.table.DokusTableDivider
import tech.dokus.foundation.aura.constrains.Constrains

private val SkeletonBorderRadius = 4.dp

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
            .padding(16.dp)
    ) {
        // Period label skeleton
        SkeletonBox(width = 80.dp, height = 12.dp)
        Spacer(Modifier.height(4.dp))
        // Net amount skeleton
        SkeletonBox(width = 160.dp, height = 40.dp)
        Spacer(Modifier.height(4.dp))
        // Breakdown line skeleton
        SkeletonBox(width = 200.dp, height = 12.dp)
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
            .height(56.dp)
            .padding(horizontal = Constrains.Spacing.large),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left section: date and contact
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Date placeholder
            SkeletonBox(
                width = 60.dp,
                height = 12.dp,
                modifier = Modifier.padding(vertical = 22.dp)
            )
            // Contact placeholder
            SkeletonBox(
                width = 120.dp,
                height = 12.dp,
                modifier = Modifier.padding(vertical = 22.dp)
            )
        }
        // Right section: amount
        SkeletonBox(
            width = 80.dp,
            height = 12.dp,
            modifier = Modifier.padding(vertical = 22.dp)
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
                .height(64.dp)
        )
        DokusTableDivider()

        // Header skeleton (desktop only)
        if (showHeader) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
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
