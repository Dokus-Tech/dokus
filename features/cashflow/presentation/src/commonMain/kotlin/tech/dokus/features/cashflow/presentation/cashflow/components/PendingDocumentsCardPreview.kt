@file:Suppress("LongMethod") // Mock data and long setup functions

package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BillDraftData
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
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

// Preview mock data file size constants (in bytes)
private const val InvoiceSizeBytes = 125000L
private const val BillSizeBytes = 98000L
private const val ExpenseSizeBytes = 45000L
private const val ScanSizeBytes = 200000L
private const val ClientInvoiceSizeBytes = 150000L

// Draft version constant
private const val InitialDraftVersion = 1

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
private fun getSamplePendingDocuments(): List<DocumentRecordDto> {
    // Use static date for preview stability
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
        // Invoice with extraction data - NeedsReview status
        DocumentRecordDto(
            document = DocumentDto(
                id = DocumentId.generate(),
                tenantId = tenantId,
                filename = "invoice-2024-001.pdf",
                contentType = "application/pdf",
                sizeBytes = InvoiceSizeBytes,
                storageKey = "documents/invoice-2024-001.pdf",
                uploadedAt = now
            ),
            draft = DocumentDraftDto(
                documentId = DocumentId.generate(),
                tenantId = tenantId,
                documentStatus = DocumentStatus.NeedsReview,
                documentType = DocumentType.Invoice,
                extractedData = InvoiceDraftData(invoiceNumber = "INV-3006-4400"),
                aiDraftData = null,
                aiDraftSourceRunId = null,
                draftVersion = InitialDraftVersion,
                draftEditedAt = null,
                draftEditedBy = null,
                linkedContactId = null,
                counterpartyIntent = tech.dokus.domain.enums.CounterpartyIntent.None,
                rejectReason = null,
                lastSuccessfulRunId = null,
                createdAt = now,
                updatedAt = now
            ),
            latestIngestion = null,
            confirmedEntity = null
        ),
        // Bill with extraction data - NeedsReview status
        DocumentRecordDto(
            document = DocumentDto(
                id = DocumentId.generate(),
                tenantId = tenantId,
                filename = "supplier-bill.pdf",
                contentType = "application/pdf",
                sizeBytes = BillSizeBytes,
                storageKey = "documents/supplier-bill.pdf",
                uploadedAt = now
            ),
            draft = DocumentDraftDto(
                documentId = DocumentId.generate(),
                tenantId = tenantId,
                documentStatus = DocumentStatus.NeedsReview,
                documentType = DocumentType.Bill,
                extractedData = BillDraftData(
                    invoiceNumber = "BILL-2024-123",
                    supplierName = "Office Supplies Inc."
                ),
                aiDraftData = null,
                aiDraftSourceRunId = null,
                draftVersion = InitialDraftVersion,
                draftEditedAt = null,
                draftEditedBy = null,
                linkedContactId = null,
                counterpartyIntent = tech.dokus.domain.enums.CounterpartyIntent.None,
                rejectReason = null,
                lastSuccessfulRunId = null,
                createdAt = now,
                updatedAt = now
            ),
            latestIngestion = null,
            confirmedEntity = null
        ),
        // Receipt - NeedsReview status
        DocumentRecordDto(
            document = DocumentDto(
                id = DocumentId.generate(),
                tenantId = tenantId,
                filename = "receipt-lunch-meeting.jpg",
                contentType = "image/jpeg",
                sizeBytes = ExpenseSizeBytes,
                storageKey = "documents/receipt-lunch-meeting.jpg",
                uploadedAt = now
            ),
            draft = DocumentDraftDto(
                documentId = DocumentId.generate(),
                tenantId = tenantId,
                documentStatus = DocumentStatus.NeedsReview,
                documentType = DocumentType.Receipt,
                extractedData = ReceiptDraftData(merchantName = "Restaurant ABC"),
                aiDraftData = null,
                aiDraftSourceRunId = null,
                draftVersion = InitialDraftVersion,
                draftEditedAt = null,
                draftEditedBy = null,
                linkedContactId = null,
                counterpartyIntent = tech.dokus.domain.enums.CounterpartyIntent.None,
                rejectReason = null,
                lastSuccessfulRunId = null,
                createdAt = now,
                updatedAt = now
            ),
            latestIngestion = null,
            confirmedEntity = null
        ),
        // Document without draft yet (still processing)
        DocumentRecordDto(
            document = DocumentDto(
                id = DocumentId.generate(),
                tenantId = tenantId,
                filename = "scan-20240525.pdf",
                contentType = "application/pdf",
                sizeBytes = ScanSizeBytes,
                storageKey = "documents/scan-20240525.pdf",
                uploadedAt = now
            ),
            draft = null,
            latestIngestion = null,
            confirmedEntity = null
        ),
        // Another invoice - NeedsReview status
        DocumentRecordDto(
            document = DocumentDto(
                id = DocumentId.generate(),
                tenantId = tenantId,
                filename = "invoice-client-abc.pdf",
                contentType = "application/pdf",
                sizeBytes = ClientInvoiceSizeBytes,
                storageKey = "documents/invoice-client-abc.pdf",
                uploadedAt = now
            ),
            draft = DocumentDraftDto(
                documentId = DocumentId.generate(),
                tenantId = tenantId,
                documentStatus = DocumentStatus.NeedsReview,
                documentType = DocumentType.Invoice,
                extractedData = InvoiceDraftData(invoiceNumber = "INV-3006-4401"),
                aiDraftData = null,
                aiDraftSourceRunId = null,
                draftVersion = InitialDraftVersion,
                draftEditedAt = null,
                draftEditedBy = null,
                linkedContactId = null,
                counterpartyIntent = tech.dokus.domain.enums.CounterpartyIntent.None,
                rejectReason = null,
                lastSuccessfulRunId = null,
                createdAt = now,
                updatedAt = now
            ),
            latestIngestion = null,
            confirmedEntity = null
        )
    )
}
