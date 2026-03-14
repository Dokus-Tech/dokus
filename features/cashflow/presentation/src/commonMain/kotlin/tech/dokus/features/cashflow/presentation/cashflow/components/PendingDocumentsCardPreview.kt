@file:Suppress("LongMethod") // Mock data and long setup functions

package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentListItemDto
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

// Preview layout constants
private val PreviewPadding = 24.dp

// Pagination constants
private const val PreviewPageSize = 4
private const val PreviewFirstPage = 0
private const val PreviewSecondPage = 1

// Preview mock data date constants
private const val PreviewYear = 2024
private const val PreviewMonth = 5
private const val PreviewDay = 25
private const val PreviewHour = 10
private const val PreviewMinute = 30
private const val PreviewSecond = 0
private const val PreviewNanosecond = 0

/**
 * Preview for PendingDocumentsCard component with documents.
 */
@Preview
@Composable
fun PendingDocumentsCardPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PendingDocumentsCard(
            state = DokusState.success(
                PaginationState(
                    data = getSamplePendingDocuments(),
                    currentPage = PreviewFirstPage,
                    pageSize = PreviewPageSize,
                    hasMorePages = true
                )
            ),
            onDocumentClick = {},
            onLoadMore = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(PreviewPadding)
        )
    }
}

/**
 * Preview for PendingDocumentsCard in loading state.
 */
@Preview
@Composable
fun PendingDocumentsCardLoadingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PendingDocumentsCard(
            state = DokusState.loading(),
            onDocumentClick = {},
            onLoadMore = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(PreviewPadding)
        )
    }
}

/**
 * Preview for PendingDocumentsCard in empty state.
 */
@Preview
@Composable
fun PendingDocumentsCardEmptyPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PendingDocumentsCard(
            state = DokusState.success(
                PaginationState(
                    data = emptyList(),
                    currentPage = PreviewFirstPage,
                    pageSize = PreviewPageSize,
                    hasMorePages = false
                )
            ),
            onDocumentClick = {},
            onLoadMore = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(PreviewPadding)
        )
    }
}

/**
 * Preview for PendingDocumentsCard in error state.
 */
@Preview
@Composable
fun PendingDocumentsCardErrorPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PendingDocumentsCard(
            state = DokusState.error(
                DokusException.ConnectionError("Connection refused"),
                RetryHandler {}
            ),
            onDocumentClick = {},
            onLoadMore = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(PreviewPadding)
        )
    }
}

/**
 * Preview for PendingDocumentsCard with lazy loading (has more items).
 */
@Preview
@Composable
fun PendingDocumentsCardWithMoreItemsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PendingDocumentsCard(
            state = DokusState.success(
                PaginationState(
                    data = getSamplePendingDocuments().take(PreviewPageSize),
                    currentPage = PreviewSecondPage,
                    pageSize = PreviewPageSize,
                    hasMorePages = true
                )
            ),
            onDocumentClick = {},
            onLoadMore = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(PreviewPadding)
        )
    }
}

/**
 * Generates sample pending documents for preview.
 */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
private fun getSamplePendingDocuments(): List<DocumentListItemDto> {
    val now = LocalDateTime(
        PreviewYear,
        PreviewMonth,
        PreviewDay,
        PreviewHour,
        PreviewMinute,
        PreviewSecond,
        PreviewNanosecond
    )
    val tenantId = TenantId.generate()

    return listOf(
        DocumentListItemDto(
            documentId = DocumentId.generate(),
            tenantId = tenantId,
            filename = "invoice-2024-001.pdf",
            documentType = DocumentType.Invoice,
            direction = null,
            documentStatus = DocumentStatus.NeedsReview,
            ingestionStatus = null,
            effectiveOrigin = DocumentSource.Upload,
            uploadedAt = now,
            sortDate = now.date,
            counterpartyDisplayName = null,
            purposeRendered = "INV-3006-4400",
            totalAmount = null,
            currency = null,
        ),
        DocumentListItemDto(
            documentId = DocumentId.generate(),
            tenantId = tenantId,
            filename = "supplier-inbound-invoice.pdf",
            documentType = DocumentType.Invoice,
            direction = null,
            documentStatus = DocumentStatus.NeedsReview,
            ingestionStatus = null,
            effectiveOrigin = DocumentSource.Upload,
            uploadedAt = now,
            sortDate = now.date,
            counterpartyDisplayName = "Office Supplies Inc.",
            purposeRendered = "INV-2024-123",
            totalAmount = null,
            currency = null,
        ),
        DocumentListItemDto(
            documentId = DocumentId.generate(),
            tenantId = tenantId,
            filename = "receipt-lunch-meeting.jpg",
            documentType = DocumentType.Receipt,
            direction = null,
            documentStatus = DocumentStatus.NeedsReview,
            ingestionStatus = null,
            effectiveOrigin = DocumentSource.Upload,
            uploadedAt = now,
            sortDate = now.date,
            counterpartyDisplayName = "Restaurant ABC",
            purposeRendered = null,
            totalAmount = null,
            currency = null,
        ),
        DocumentListItemDto(
            documentId = DocumentId.generate(),
            tenantId = tenantId,
            filename = "scan-20240525.pdf",
            documentType = null,
            direction = null,
            documentStatus = null,
            ingestionStatus = null,
            effectiveOrigin = DocumentSource.Upload,
            uploadedAt = now,
            sortDate = now.date,
            counterpartyDisplayName = null,
            purposeRendered = null,
            totalAmount = null,
            currency = null,
        ),
        DocumentListItemDto(
            documentId = DocumentId.generate(),
            tenantId = tenantId,
            filename = "invoice-client-abc.pdf",
            documentType = DocumentType.Invoice,
            direction = null,
            documentStatus = DocumentStatus.NeedsReview,
            ingestionStatus = null,
            effectiveOrigin = DocumentSource.Upload,
            uploadedAt = now,
            sortDate = now.date,
            counterpartyDisplayName = null,
            purposeRendered = "INV-3006-4401",
            totalAmount = null,
            currency = null,
        ),
    )
}
