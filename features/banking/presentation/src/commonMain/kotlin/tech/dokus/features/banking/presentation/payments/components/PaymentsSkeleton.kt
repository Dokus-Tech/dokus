package tech.dokus.features.banking.presentation.payments.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import tech.dokus.foundation.aura.components.layout.DokusTableCell
import tech.dokus.foundation.aura.components.layout.DokusTableRow
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val SkeletonRadius = Constraints.CornerRadius.badge

@Composable
private fun SkeletonBox(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(SkeletonRadius))
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
    )
}

@Composable
private fun SkeletonHeaderRow(modifier: Modifier = Modifier) {
    DokusTableRow(
        modifier = modifier,
        minHeight = Constraints.CropGuide.cornerLength,
        contentPadding = PaddingValues(horizontal = Constraints.Spacing.large),
    ) {
        DokusTableCell(PaymentsTableColumns.Date) {
            SkeletonBox(width = Constraints.Spacing.large * 3f, height = Constraints.Spacing.small)
        }
        DokusTableCell(PaymentsTableColumns.Description) {
            SkeletonBox(width = Constraints.Spacing.large * 5f, height = Constraints.Spacing.small)
        }
        DokusTableCell(PaymentsTableColumns.Counterparty) {
            SkeletonBox(width = Constraints.Spacing.large * 4f, height = Constraints.Spacing.small)
        }
        DokusTableCell(PaymentsTableColumns.Account) {
            SkeletonBox(width = Constraints.Spacing.large * 2f, height = Constraints.Spacing.small)
        }
        DokusTableCell(PaymentsTableColumns.Status) {
            SkeletonBox(width = Constraints.Spacing.large * 3f, height = Constraints.Spacing.small)
        }
        DokusTableCell(PaymentsTableColumns.Document) {
            SkeletonBox(width = Constraints.Spacing.large * 3f, height = Constraints.Spacing.small)
        }
        DokusTableCell(PaymentsTableColumns.Amount) {
            SkeletonBox(width = Constraints.Spacing.large * 4f, height = Constraints.Spacing.small)
        }
    }
}

@Composable
private fun SkeletonTableRow(modifier: Modifier = Modifier) {
    DokusTableRow(
        modifier = modifier,
        minHeight = Constraints.Height.input,
        contentPadding = PaddingValues(horizontal = Constraints.Spacing.large),
    ) {
        DokusTableCell(PaymentsTableColumns.Date) {
            SkeletonBox(width = Constraints.Spacing.large * 2.5f, height = Constraints.Spacing.medium)
        }
        DokusTableCell(PaymentsTableColumns.Description) {
            SkeletonBox(width = Constraints.Spacing.large * 7f, height = Constraints.Spacing.medium)
        }
        DokusTableCell(PaymentsTableColumns.Counterparty) {
            SkeletonBox(width = Constraints.Spacing.large * 5f, height = Constraints.Spacing.medium)
        }
        DokusTableCell(PaymentsTableColumns.Account) {
            SkeletonBox(width = Constraints.Spacing.large * 2f, height = Constraints.Spacing.medium)
        }
        DokusTableCell(PaymentsTableColumns.Status) {
            SkeletonBox(width = Constraints.Spacing.large * 4f, height = Constraints.Spacing.medium)
        }
        DokusTableCell(PaymentsTableColumns.Document) {
            SkeletonBox(width = Constraints.Spacing.large * 3f, height = Constraints.Spacing.medium)
        }
        DokusTableCell(PaymentsTableColumns.Amount) {
            SkeletonBox(width = Constraints.Spacing.large * 3.5f, height = Constraints.Spacing.medium)
        }
    }
}

@Composable
internal fun PaymentsSkeleton(
    rowCount: Int = 6,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SkeletonHeaderRow()
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        repeat(rowCount) { index ->
            SkeletonTableRow()
            if (index < rowCount - 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Preview
@Composable
private fun PaymentsSkeletonPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        PaymentsSkeleton(rowCount = 3)
    }
}
