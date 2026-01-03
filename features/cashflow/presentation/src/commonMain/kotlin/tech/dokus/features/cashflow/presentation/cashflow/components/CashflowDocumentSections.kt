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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_no_documents
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.ShimmerLine

// Section layout dimensions
private val SectionItemSpacing = 16.dp
private val DesktopErrorVerticalPadding = 48.dp
private val MobileErrorVerticalPadding = 32.dp

// Table skeleton dimensions
private val SkeletonRowSpacing = 8.dp
private val SkeletonHeaderVerticalPadding = 12.dp
private val SkeletonHeaderHorizontalSpacing = 16.dp
private val SkeletonHeaderLineHeight = 14.dp
private val SkeletonRowVerticalPadding = 16.dp
private val SkeletonRowLineHeight = 16.dp
private const val TableColumnCount = 5
private const val TableRowCount = 5

// Mobile skeleton dimensions
private const val MobileSkeletonRowCount = 6
private val MobileSkeletonHorizontalPadding = 16.dp
private val MobileSkeletonSpacerWidth = 16.dp
private val MobileSkeletonDateWidth = 60.dp
private val MobileSkeletonAmountWidth = 70.dp
private val MobileSkeletonAmountHeight = 22.dp

// Empty and loading states
private val EmptyStateVerticalPadding = 48.dp
private val LoadingMoreVerticalPadding = 16.dp

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
