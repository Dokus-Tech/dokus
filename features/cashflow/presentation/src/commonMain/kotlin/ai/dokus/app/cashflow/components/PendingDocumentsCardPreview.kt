package ai.dokus.app.cashflow.components

import tech.dokus.foundation.app.state.DokusState
import ai.dokus.foundation.design.tooling.PreviewParameters
import ai.dokus.foundation.design.tooling.PreviewParametersProvider
import ai.dokus.foundation.design.tooling.TestWrapper
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.ExtractedBillFields
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.model.ExtractedExpenseFields
import tech.dokus.domain.model.ExtractedInvoiceFields
import tech.dokus.domain.model.common.PaginationState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

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
                    currentPage = 0,
                    pageSize = 4,
                    hasMorePages = true
                )
            ),
            onDocumentClick = {},
            onLoadMore = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
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
                .padding(24.dp)
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
                    currentPage = 0,
                    pageSize = 4,
                    hasMorePages = false
                )
            ),
            onDocumentClick = {},
            onLoadMore = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
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
                .padding(24.dp)
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
                    data = getSamplePendingDocuments().take(4),
                    currentPage = 1,
                    pageSize = 4,
                    hasMorePages = true
                )
            ),
            onDocumentClick = {},
            onLoadMore = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        )
    }
}

/**
 * Generates sample pending documents for preview.
 */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
private fun getSamplePendingDocuments(): List<DocumentRecordDto> {
    // Use static date for preview stability
    val now = LocalDateTime(2024, 5, 25, 10, 30, 0, 0)
    val tenantId = TenantId.generate()

    return listOf(
        // Invoice with extraction data - NeedsReview status
        DocumentRecordDto(
            document = DocumentDto(
                id = DocumentId.generate(),
                tenantId = tenantId,
                filename = "invoice-2024-001.pdf",
                contentType = "application/pdf",
                sizeBytes = 125000,
                storageKey = "documents/invoice-2024-001.pdf",
                uploadedAt = now
            ),
            draft = DocumentDraftDto(
                documentId = DocumentId.generate(),
                tenantId = tenantId,
                draftStatus = DraftStatus.NeedsReview,
                documentType = DocumentType.Invoice,
                extractedData = ExtractedDocumentData(
                    invoice = ExtractedInvoiceFields(invoiceNumber = "INV-3006-4400")
                ),
                aiDraftData = null,
                aiDraftSourceRunId = null,
                draftVersion = 1,
                draftEditedAt = null,
                draftEditedBy = null,
                suggestedContactId = null,
                contactSuggestionConfidence = null,
                contactSuggestionReason = null,
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
                sizeBytes = 98000,
                storageKey = "documents/supplier-bill.pdf",
                uploadedAt = now
            ),
            draft = DocumentDraftDto(
                documentId = DocumentId.generate(),
                tenantId = tenantId,
                draftStatus = DraftStatus.NeedsReview,
                documentType = DocumentType.Bill,
                extractedData = ExtractedDocumentData(
                    bill = ExtractedBillFields(
                        invoiceNumber = "BILL-2024-123",
                        supplierName = "Office Supplies Inc."
                    )
                ),
                aiDraftData = null,
                aiDraftSourceRunId = null,
                draftVersion = 1,
                draftEditedAt = null,
                draftEditedBy = null,
                suggestedContactId = null,
                contactSuggestionConfidence = null,
                contactSuggestionReason = null,
                lastSuccessfulRunId = null,
                createdAt = now,
                updatedAt = now
            ),
            latestIngestion = null,
            confirmedEntity = null
        ),
        // Expense - NeedsReview status
        DocumentRecordDto(
            document = DocumentDto(
                id = DocumentId.generate(),
                tenantId = tenantId,
                filename = "receipt-lunch-meeting.jpg",
                contentType = "image/jpeg",
                sizeBytes = 45000,
                storageKey = "documents/receipt-lunch-meeting.jpg",
                uploadedAt = now
            ),
            draft = DocumentDraftDto(
                documentId = DocumentId.generate(),
                tenantId = tenantId,
                draftStatus = DraftStatus.NeedsReview,
                documentType = DocumentType.Expense,
                extractedData = ExtractedDocumentData(
                    expense = ExtractedExpenseFields(merchant = "Restaurant ABC")
                ),
                aiDraftData = null,
                aiDraftSourceRunId = null,
                draftVersion = 1,
                draftEditedAt = null,
                draftEditedBy = null,
                suggestedContactId = null,
                contactSuggestionConfidence = null,
                contactSuggestionReason = null,
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
                sizeBytes = 200000,
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
                sizeBytes = 150000,
                storageKey = "documents/invoice-client-abc.pdf",
                uploadedAt = now
            ),
            draft = DocumentDraftDto(
                documentId = DocumentId.generate(),
                tenantId = tenantId,
                draftStatus = DraftStatus.NeedsReview,
                documentType = DocumentType.Invoice,
                extractedData = ExtractedDocumentData(
                    invoice = ExtractedInvoiceFields(invoiceNumber = "INV-3006-4401")
                ),
                aiDraftData = null,
                aiDraftSourceRunId = null,
                draftVersion = 1,
                draftEditedAt = null,
                draftEditedBy = null,
                suggestedContactId = null,
                contactSuggestionConfidence = null,
                contactSuggestionReason = null,
                lastSuccessfulRunId = null,
                createdAt = now,
                updatedAt = now
            ),
            latestIngestion = null,
            confirmedEntity = null
        )
    )
}
