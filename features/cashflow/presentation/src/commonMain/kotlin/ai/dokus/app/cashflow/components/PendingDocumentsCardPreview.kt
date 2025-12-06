package ai.dokus.app.cashflow.components

import ai.dokus.app.core.state.DokusState
import ai.dokus.foundation.design.tooling.PreviewParameters
import ai.dokus.foundation.design.tooling.PreviewParametersProvider
import ai.dokus.foundation.design.tooling.TestWrapper
import ai.dokus.foundation.domain.asbtractions.RetryHandler
import ai.dokus.foundation.domain.enums.DocumentType
import ai.dokus.foundation.domain.enums.ProcessingStatus
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.DocumentId
import ai.dokus.foundation.domain.ids.DocumentProcessingId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.DocumentDto
import ai.dokus.foundation.domain.model.DocumentProcessingDto
import ai.dokus.foundation.domain.model.ExtractedBillData
import ai.dokus.foundation.domain.model.ExtractedDocumentData
import ai.dokus.foundation.domain.model.ExtractedExpenseData
import ai.dokus.foundation.domain.model.ExtractedInvoiceData
import ai.dokus.foundation.domain.model.common.PaginationState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
                    pageSize = 5,
                    hasMorePages = true
                )
            ),
            onDocumentClick = {},
            onPreviousClick = {},
            onNextClick = {},
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
            onPreviousClick = {},
            onNextClick = {},
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
                    pageSize = 5,
                    hasMorePages = false
                )
            ),
            onDocumentClick = {},
            onPreviousClick = {},
            onNextClick = {},
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
            onPreviousClick = {},
            onNextClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        )
    }
}

/**
 * Preview for PendingDocumentsCard with pagination enabled.
 */
@Preview
@Composable
fun PendingDocumentsCardWithPaginationPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PendingDocumentsCard(
            state = DokusState.success(
                PaginationState(
                    data = getSamplePendingDocuments().take(3),
                    currentPage = 1,
                    pageSize = 3,
                    hasMorePages = true
                )
            ),
            onDocumentClick = {},
            onPreviousClick = {},
            onNextClick = {},
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
private fun getSamplePendingDocuments(): List<DocumentProcessingDto> {
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val tenantId = TenantId.generate()

    return listOf(
        // Invoice with extraction data - Processed status
        DocumentProcessingDto(
            id = DocumentProcessingId.generate(),
            documentId = DocumentId.generate(),
            tenantId = tenantId,
            status = ProcessingStatus.Processed,
            documentType = DocumentType.Invoice,
            extractedData = ExtractedDocumentData(
                invoice = ExtractedInvoiceData(invoiceNumber = "INV-3006-4400")
            ),
            confidence = 0.95,
            createdAt = now,
            updatedAt = now,
            document = DocumentDto(
                id = DocumentId.generate(),
                tenantId = tenantId,
                filename = "invoice-2024-001.pdf",
                contentType = "application/pdf",
                sizeBytes = 125000,
                storageKey = "documents/invoice-2024-001.pdf",
                uploadedAt = now
            )
        ),
        // Bill with extraction data - Processing status
        DocumentProcessingDto(
            id = DocumentProcessingId.generate(),
            documentId = DocumentId.generate(),
            tenantId = tenantId,
            status = ProcessingStatus.Processing,
            documentType = DocumentType.Bill,
            extractedData = ExtractedDocumentData(
                bill = ExtractedBillData(
                    invoiceNumber = "BILL-2024-123",
                    supplierName = "Office Supplies Inc."
                )
            ),
            confidence = 0.87,
            createdAt = now,
            updatedAt = now,
            document = DocumentDto(
                id = DocumentId.generate(),
                tenantId = tenantId,
                filename = "supplier-bill.pdf",
                contentType = "application/pdf",
                sizeBytes = 98000,
                storageKey = "documents/supplier-bill.pdf",
                uploadedAt = now
            )
        ),
        // Expense - Pending status (just uploaded)
        DocumentProcessingDto(
            id = DocumentProcessingId.generate(),
            documentId = DocumentId.generate(),
            tenantId = tenantId,
            status = ProcessingStatus.Pending,
            documentType = DocumentType.Expense,
            extractedData = ExtractedDocumentData(
                expense = ExtractedExpenseData(merchant = "Restaurant ABC")
            ),
            confidence = null,
            createdAt = now,
            updatedAt = now,
            document = DocumentDto(
                id = DocumentId.generate(),
                tenantId = tenantId,
                filename = "receipt-lunch-meeting.jpg",
                contentType = "image/jpeg",
                sizeBytes = 45000,
                storageKey = "documents/receipt-lunch-meeting.jpg",
                uploadedAt = now
            )
        ),
        // Document without extraction (unknown type) - Queued status
        DocumentProcessingDto(
            id = DocumentProcessingId.generate(),
            documentId = DocumentId.generate(),
            tenantId = tenantId,
            status = ProcessingStatus.Queued,
            documentType = null,
            extractedData = null,
            confidence = null,
            createdAt = now,
            updatedAt = now,
            document = DocumentDto(
                id = DocumentId.generate(),
                tenantId = tenantId,
                filename = "scan-20240525.pdf",
                contentType = "application/pdf",
                sizeBytes = 200000,
                storageKey = "documents/scan-20240525.pdf",
                uploadedAt = now
            )
        ),
        // Another invoice - Processed status
        DocumentProcessingDto(
            id = DocumentProcessingId.generate(),
            documentId = DocumentId.generate(),
            tenantId = tenantId,
            status = ProcessingStatus.Processed,
            documentType = DocumentType.Invoice,
            extractedData = ExtractedDocumentData(
                invoice = ExtractedInvoiceData(invoiceNumber = "INV-3006-4401")
            ),
            confidence = 0.92,
            createdAt = now,
            updatedAt = now,
            document = DocumentDto(
                id = DocumentId.generate(),
                tenantId = tenantId,
                filename = "invoice-client-abc.pdf",
                contentType = "application/pdf",
                sizeBytes = 150000,
                storageKey = "documents/invoice-client-abc.pdf",
                uploadedAt = now
            )
        )
    )
}
