package ai.dokus.app.cashflow.components

import ai.dokus.app.resources.generated.Res
import tech.dokus.foundation.app.state.DokusState
import ai.dokus.foundation.design.components.common.DokusErrorContent
import ai.dokus.foundation.design.components.common.ShimmerLine
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.common.PaginationState
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        .padding(vertical = 48.dp),
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        .padding(vertical = 32.dp),
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Table header skeleton
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(5) {
                ShimmerLine(
                    modifier = Modifier.weight(1f),
                    height = 14.dp
                )
            }
        }

        // Table rows skeleton
        repeat(5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(5) {
                    ShimmerLine(
                        modifier = Modifier.weight(1f),
                        height = 16.dp
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerLine(
                    modifier = Modifier.weight(1f),
                    height = 16.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                ShimmerLine(
                    modifier = Modifier.width(60.dp),
                    height = 16.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                ShimmerLine(
                    modifier = Modifier.width(70.dp),
                    height = 22.dp
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
            .padding(vertical = 48.dp),
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
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator()
    }
}
