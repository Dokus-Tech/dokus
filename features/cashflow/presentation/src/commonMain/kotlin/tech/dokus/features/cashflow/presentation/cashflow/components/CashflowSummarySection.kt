package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.common.OfflineOverlay

// UI dimensions
private val SummaryRowHeight = 340.dp
private val CardSpacing = 24.dp
private val BusinessHealthMinHeight = 120.dp

// Layout weights
private const val LeftColumnWeight = 3f
private const val RightColumnWeight = 2f

/**
 * Summary cards section for the Cashflow screen desktop layout.
 *
 * Displays a horizontal row with summary cards matching Figma layout:
 * - Left column: VAT Summary (top) + Business Health (bottom)
 * - Right side: Pending Documents card
 *
 * Each card handles its own loading/error state independently.
 *
 * @param vatSummaryState The DokusState containing VAT summary data
 * @param businessHealthState The DokusState containing business health data
 * @param pendingDocumentsState The DokusState containing pending documents with pagination
 * @param onPendingDocumentClick Callback when a pending document is clicked
 * @param onPendingLoadMore Callback to load more pending documents
 * @param modifier Optional modifier for the section
 */
@Composable
fun CashflowSummarySection(
    vatSummaryState: DokusState<VatSummaryData>,
    businessHealthState: DokusState<BusinessHealthData>,
    pendingDocumentsState: DokusState<PaginationState<DocumentRecordDto>>,
    onPendingDocumentClick: (DocumentRecordDto) -> Unit,
    onPendingLoadMore: () -> Unit,
    isOnline: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Fixed height for the row - LazyColumn doesn't support intrinsic measurements
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SummaryRowHeight),
        horizontalArrangement = Arrangement.spacedBy(CardSpacing)
    ) {
        // Left column: VAT Summary + Business Health stacked vertically
        Column(
            modifier = Modifier
                .weight(LeftColumnWeight)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(CardSpacing)
        ) {
            // VAT Summary Card at top (requires network connection)
            // When offline with error, show loading skeleton behind overlay instead of error
            OfflineOverlay(isOffline = !isOnline) {
                VatSummaryCard(
                    state = if (!isOnline && vatSummaryState is DokusState.Error) {
                        DokusState.loading()
                    } else {
                        vatSummaryState
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Business Health Card below - fills remaining space (requires network connection)
            // When offline with error, show loading skeleton behind overlay instead of error
            OfflineOverlay(
                isOffline = !isOnline,
                modifier = Modifier
                    .defaultMinSize(minHeight = BusinessHealthMinHeight)
                    .weight(1f)
            ) {
                BusinessHealthCard(
                    state = if (!isOnline && businessHealthState is DokusState.Error) {
                        DokusState.loading()
                    } else {
                        businessHealthState
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Right side: Pending Documents Card (requires network connection)
        // When offline with error, show loading skeleton behind overlay instead of error
        OfflineOverlay(
            isOffline = !isOnline,
            modifier = Modifier
                .weight(RightColumnWeight)
                .fillMaxHeight()
        ) {
            PendingDocumentsCard(
                state = if (!isOnline && pendingDocumentsState is DokusState.Error) {
                    DokusState.loading()
                } else {
                    pendingDocumentsState
                },
                onDocumentClick = onPendingDocumentClick,
                onLoadMore = onPendingLoadMore,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
