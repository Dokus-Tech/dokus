package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_no_documents
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.constrains.Constraints
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

// Section layout dimensions
private val SectionItemSpacing = Constraints.Spacing.large
private val DesktopErrorVerticalPadding = Constraints.Spacing.xxxLarge
private val MobileErrorVerticalPadding = Constraints.Spacing.xxLarge

// Table skeleton dimensions
private val SkeletonRowSpacing = Constraints.Spacing.small
private val SkeletonHeaderVerticalPadding = Constraints.Spacing.medium
private val SkeletonHeaderHorizontalSpacing = Constraints.Spacing.large
private val SkeletonHeaderLineHeight = Constraints.Height.shimmerLine
private val SkeletonRowVerticalPadding = Constraints.Spacing.large
private val SkeletonRowLineHeight = Constraints.Spacing.large
private const val TableColumnCount = 5
private const val TableRowCount = 5

// Mobile skeleton dimensions
private const val MobileSkeletonRowCount = 6
private val MobileSkeletonHorizontalPadding = Constraints.Spacing.large
private val MobileSkeletonSpacerWidth = Constraints.Spacing.large
private val MobileSkeletonDateWidth = Constraints.IconSize.xxLarge - Constraints.Spacing.xSmall
private val MobileSkeletonAmountWidth =
    Constraints.IconSize.xxLarge + Constraints.Spacing.small - Constraints.Spacing.xxSmall
private val MobileSkeletonAmountHeight = Constraints.IconSize.smallMedium + Constraints.Spacing.xxSmall

// Empty and loading states
private val EmptyStateVerticalPadding = Constraints.Spacing.xxxLarge
private val LoadingMoreVerticalPadding = Constraints.Spacing.large

/**
 * Documents table section with its own loading/error handling.
 * Used on desktop layout to display financial documents in a table format.
 *
 * @param state The DokusState containing pagination state for documents
 * @param onDocumentClick Callback when a document row is clicked
 * @param onMoreClick Callback when the more actions button is clicked
 * @param modifier Optional modifier
 */
@Composable
fun CashflowDocumentsTableSection(
    state: DokusState<PaginationState<FinancialDocumentDto>>,
    onDocumentClick: (FinancialDocumentDto) -> Unit,
    onMoreClick: (FinancialDocumentDto) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SectionItemSpacing)
    ) {
        when (state) {
            is DokusState.Loading, is DokusState.Idle -> {
                DocumentsTableSkeleton()
            }

            is DokusState.Success -> {
                val paginationState = state.data
                if (paginationState.data.isEmpty()) {
                    EmptyDocumentsState()
                } else {
                    FinancialDocumentTable(
                        documents = paginationState.data,
                        onDocumentClick = onDocumentClick,
                        onMoreClick = onMoreClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (paginationState.isLoadingMore) {
                    LoadingMoreIndicator()
                }
            }

            is DokusState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = DesktopErrorVerticalPadding),
                    contentAlignment = Alignment.Center
                ) {
                    DokusErrorContent(
                        exception = state.exception,
                        retryHandler = state.retryHandler
                    )
                }
            }
        }
    }
}

/**
 * Mobile documents list section with its own loading/error handling.
 * Used on mobile layout to display financial documents in a list format.
 *
 * @param state The DokusState containing pagination state for documents
 * @param onDocumentClick Callback when a document is clicked
 * @param modifier Optional modifier
 */
@Composable
fun CashflowMobileDocumentsSection(
    state: DokusState<PaginationState<FinancialDocumentDto>>,
    onDocumentClick: (FinancialDocumentDto) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SectionItemSpacing)
    ) {
        when (state) {
            is DokusState.Loading, is DokusState.Idle -> {
                MobileDocumentsListSkeleton()
            }

            is DokusState.Success -> {
                val paginationState = state.data
                if (paginationState.data.isEmpty()) {
                    EmptyDocumentsState()
                } else {
                    FinancialDocumentList(
                        documents = paginationState.data,
                        onDocumentClick = onDocumentClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (paginationState.isLoadingMore) {
                    LoadingMoreIndicator()
                }
            }

            is DokusState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MobileErrorVerticalPadding),
                    contentAlignment = Alignment.Center
                ) {
                    DokusErrorContent(
                        exception = state.exception,
                        retryHandler = state.retryHandler
                    )
                }
            }
        }
    }
}

/**
 * Skeleton for documents table during loading.
 */
@Composable
private fun DocumentsTableSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SkeletonRowSpacing)
    ) {
        // Table header skeleton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = SkeletonHeaderVerticalPadding),
            horizontalArrangement = Arrangement.spacedBy(SkeletonHeaderHorizontalSpacing)
        ) {
            repeat(TableColumnCount) {
                ShimmerLine(
                    modifier = Modifier.weight(1f),
                    height = SkeletonHeaderLineHeight
                )
            }
        }

        // Table rows skeleton
        repeat(TableRowCount) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SkeletonRowVerticalPadding),
                horizontalArrangement = Arrangement.spacedBy(SkeletonHeaderHorizontalSpacing)
            ) {
                repeat(TableColumnCount) {
                    ShimmerLine(
                        modifier = Modifier.weight(1f),
                        height = SkeletonRowLineHeight
                    )
                }
            }
        }
    }
}

/**
 * Skeleton for mobile documents list during loading.
 */
@Composable
private fun MobileDocumentsListSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SkeletonRowSpacing)
    ) {
        repeat(MobileSkeletonRowCount) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SkeletonHeaderVerticalPadding, horizontal = MobileSkeletonHorizontalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerLine(
                    modifier = Modifier.weight(1f),
                    height = SkeletonRowLineHeight
                )
                Spacer(modifier = Modifier.width(MobileSkeletonSpacerWidth))
                ShimmerLine(
                    modifier = Modifier.width(MobileSkeletonDateWidth),
                    height = SkeletonRowLineHeight
                )
                Spacer(modifier = Modifier.width(MobileSkeletonSpacerWidth))
                ShimmerLine(
                    modifier = Modifier.width(MobileSkeletonAmountWidth),
                    height = MobileSkeletonAmountHeight
                )
            }
        }
    }
}

/**
 * Empty state when no documents exist.
 */
@Composable
fun EmptyDocumentsState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = EmptyStateVerticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(Res.string.cashflow_no_documents),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Loading indicator for infinite scroll.
 */
@Composable
fun LoadingMoreIndicator(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = LoadingMoreVerticalPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator()
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun EmptyDocumentsStatePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        EmptyDocumentsState()
    }
}

@Preview
@Composable
private fun LoadingMoreIndicatorPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        LoadingMoreIndicator()
    }
}
