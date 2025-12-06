package ai.dokus.app.cashflow.components

import ai.dokus.app.core.state.DokusState
import ai.dokus.foundation.design.tooling.PreviewParameters
import ai.dokus.foundation.design.tooling.PreviewParametersProvider
import ai.dokus.foundation.design.tooling.TestWrapper
import ai.dokus.foundation.domain.asbtractions.RetryHandler
import ai.dokus.foundation.domain.enums.MediaDocumentType
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.ExtractedInvoiceData
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.MediaExtraction
import ai.dokus.foundation.domain.model.common.PaginationState
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
fun getSamplePendingDocuments(): List<MediaDto> {
    val now = LocalDateTime(2024, 5, 25, 12, 0)

    return listOf(
        // Invoice with extraction data
        MediaDto(
            id = MediaId.generate(),
            tenantId = TenantId.generate(),
            filename = "invoice-2024-001.pdf",
            mimeType = "application/pdf",
            sizeBytes = 125000,
            status = MediaStatus.Pending,
            extraction = MediaExtraction(
                documentType = MediaDocumentType.Invoice,
                invoice = ExtractedInvoiceData(
                    invoiceNumber = "INV-3006-4400"
                )
            ),
            createdAt = now,
            updatedAt = now
        ),
        // Bill with extraction data
        MediaDto(
            id = MediaId.generate(),
            tenantId = TenantId.generate(),
            filename = "supplier-bill.pdf",
            mimeType = "application/pdf",
            sizeBytes = 98000,
            status = MediaStatus.Pending,
            extraction = MediaExtraction(
                documentType = MediaDocumentType.Bill,
                bill = ai.dokus.foundation.domain.model.ExtractedBillData(
                    invoiceNumber = "BILL-2024-123",
                    supplierName = "Office Supplies Inc."
                )
            ),
            createdAt = now,
            updatedAt = now
        ),
        // Expense without invoice number (falls back to filename)
        MediaDto(
            id = MediaId.generate(),
            tenantId = TenantId.generate(),
            filename = "receipt-lunch-meeting.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 45000,
            status = MediaStatus.Pending,
            extraction = MediaExtraction(
                documentType = MediaDocumentType.Expense,
                expense = ai.dokus.foundation.domain.model.ExtractedExpenseData(
                    merchant = "Restaurant ABC"
                )
            ),
            createdAt = now,
            updatedAt = now
        ),
        // Document without extraction (unknown type)
        MediaDto(
            id = MediaId.generate(),
            tenantId = TenantId.generate(),
            filename = "scan-20240525.pdf",
            mimeType = "application/pdf",
            sizeBytes = 200000,
            status = MediaStatus.Pending,
            extraction = null,
            createdAt = now,
            updatedAt = now
        ),
        // Another invoice
        MediaDto(
            id = MediaId.generate(),
            tenantId = TenantId.generate(),
            filename = "invoice-client-abc.pdf",
            mimeType = "application/pdf",
            sizeBytes = 150000,
            status = MediaStatus.Pending,
            extraction = MediaExtraction(
                documentType = MediaDocumentType.Invoice,
                invoice = ExtractedInvoiceData(
                    invoiceNumber = "INV-3006-4401"
                )
            ),
            createdAt = now,
            updatedAt = now
        )
    )
}
