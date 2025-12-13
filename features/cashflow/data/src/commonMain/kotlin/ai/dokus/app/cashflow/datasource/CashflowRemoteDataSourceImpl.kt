package ai.dokus.app.cashflow.datasource

import ai.dokus.foundation.domain.enums.BillStatus
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.enums.PeppolStatus
import ai.dokus.foundation.domain.enums.PeppolTransmissionDirection
import ai.dokus.foundation.domain.enums.ProcessingStatus
import ai.dokus.foundation.domain.ids.AttachmentId
import ai.dokus.foundation.domain.ids.BillId
import ai.dokus.foundation.domain.ids.DocumentId
import ai.dokus.foundation.domain.ids.ExpenseId
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.model.AttachmentDto
import ai.dokus.foundation.domain.model.CashflowOverview
import ai.dokus.foundation.domain.model.CreateBillRequest
import ai.dokus.foundation.domain.model.CreateExpenseRequest
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.DocumentDto
import ai.dokus.foundation.domain.model.DocumentProcessingListResponse
import ai.dokus.foundation.domain.model.DocumentUploadResponse
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.InvoiceItemDto
import ai.dokus.foundation.domain.model.InvoiceTotals
import ai.dokus.foundation.domain.model.MarkBillPaidRequest
import ai.dokus.foundation.domain.model.PaginatedResponse
import ai.dokus.foundation.domain.model.PeppolInboxPollResponse
import ai.dokus.foundation.domain.model.PeppolSettingsDto
import ai.dokus.foundation.domain.model.PeppolTransmissionDto
import ai.dokus.foundation.domain.model.PeppolValidationResult
import ai.dokus.foundation.domain.model.PeppolVerifyResponse
import ai.dokus.foundation.domain.model.RecordPaymentRequest
import ai.dokus.foundation.domain.model.SavePeppolSettingsRequest
import ai.dokus.foundation.domain.model.SendInvoiceViaPeppolResponse
import ai.dokus.foundation.domain.routes.Attachments
import ai.dokus.foundation.domain.routes.Bills
import ai.dokus.foundation.domain.routes.Cashflow
import ai.dokus.foundation.domain.routes.Documents
import ai.dokus.foundation.domain.routes.Expenses
import ai.dokus.foundation.domain.routes.Invoices
import ai.dokus.foundation.domain.routes.Peppol
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onUpload
import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.patch
import io.ktor.client.plugins.resources.post
import io.ktor.client.plugins.resources.put
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.datetime.LocalDate

/**
 * HTTP-based implementation of CashflowRemoteDataSource.
 * Uses Ktor HttpClient with type-safe routing to communicate with the cashflow management API.
 */
internal class CashflowRemoteDataSourceImpl(
    private val httpClient: HttpClient
) : CashflowRemoteDataSource {

    // ============================================================================
    // INVOICE MANAGEMENT
    // ============================================================================

    override suspend fun createInvoice(request: CreateInvoiceRequest): Result<FinancialDocumentDto.InvoiceDto> {
        return runCatching {
            httpClient.post(Invoices()) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun getInvoice(id: InvoiceId): Result<FinancialDocumentDto.InvoiceDto> {
        return runCatching {
            httpClient.get(Invoices.Id(id = id.toString())).body()
        }
    }

    override suspend fun listInvoices(
        status: InvoiceStatus?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        limit: Int,
        offset: Int
    ): Result<PaginatedResponse<FinancialDocumentDto.InvoiceDto>> {
        return runCatching {
            httpClient.get(Invoices(
                status = status,
                fromDate = fromDate,
                toDate = toDate,
                limit = limit,
                offset = offset
            )).body()
        }
    }

    override suspend fun listOverdueInvoices(): Result<List<FinancialDocumentDto.InvoiceDto>> {
        return runCatching {
            httpClient.get(Invoices.Overdue()).body()
        }
    }

    override suspend fun updateInvoiceStatus(
        invoiceId: InvoiceId,
        status: InvoiceStatus
    ): Result<Unit> {
        return runCatching {
            val invoiceIdRoute = Invoices.Id(id = invoiceId.toString())
            httpClient.patch(Invoices.Id.Status(parent = invoiceIdRoute)) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("status" to status.name))
            }.body()
        }
    }

    override suspend fun updateInvoice(
        invoiceId: InvoiceId,
        request: CreateInvoiceRequest
    ): Result<FinancialDocumentDto.InvoiceDto> {
        return runCatching {
            httpClient.put(Invoices.Id(id = invoiceId.toString())) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun deleteInvoice(invoiceId: InvoiceId): Result<Unit> {
        return runCatching {
            httpClient.delete(Invoices.Id(id = invoiceId.toString())).body()
        }
    }

    override suspend fun recordPayment(request: RecordPaymentRequest): Result<Unit> {
        return runCatching {
            val invoiceIdRoute = Invoices.Id(id = request.invoiceId.toString())
            httpClient.post(Invoices.Id.Payments(parent = invoiceIdRoute)) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun sendInvoiceEmail(
        invoiceId: InvoiceId,
        recipientEmail: String?,
        message: String?
    ): Result<Unit> {
        return runCatching {
            val invoiceIdRoute = Invoices.Id(id = invoiceId.toString())
            httpClient.post(Invoices.Id.SendEmail(parent = invoiceIdRoute)) {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "recipientEmail" to recipientEmail,
                    "message" to message
                ))
            }.body()
        }
    }

    override suspend fun markInvoiceAsSent(invoiceId: InvoiceId): Result<Unit> {
        return runCatching {
            val invoiceIdRoute = Invoices.Id(id = invoiceId.toString())
            httpClient.post(Invoices.Id.MarkSent(parent = invoiceIdRoute)).body()
        }
    }

    override suspend fun calculateInvoiceTotals(items: List<InvoiceItemDto>): Result<InvoiceTotals> {
        return runCatching {
            httpClient.post(Invoices.CalculateTotals()) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("items" to items))
            }.body()
        }
    }

    // ============================================================================
    // EXPENSE MANAGEMENT
    // ============================================================================

    override suspend fun createExpense(request: CreateExpenseRequest): Result<FinancialDocumentDto.ExpenseDto> {
        return runCatching {
            httpClient.post(Expenses()) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun getExpense(id: ExpenseId): Result<FinancialDocumentDto.ExpenseDto> {
        return runCatching {
            httpClient.get(Expenses.Id(id = id.toString())).body()
        }
    }

    override suspend fun listExpenses(
        category: ExpenseCategory?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        limit: Int,
        offset: Int
    ): Result<PaginatedResponse<FinancialDocumentDto.ExpenseDto>> {
        return runCatching {
            httpClient.get(Expenses(
                category = category,
                fromDate = fromDate,
                toDate = toDate,
                limit = limit,
                offset = offset
            )).body()
        }
    }

    override suspend fun updateExpense(
        expenseId: ExpenseId,
        request: CreateExpenseRequest
    ): Result<FinancialDocumentDto.ExpenseDto> {
        return runCatching {
            httpClient.put(Expenses.Id(id = expenseId.toString())) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun deleteExpense(expenseId: ExpenseId): Result<Unit> {
        return runCatching {
            httpClient.delete(Expenses.Id(id = expenseId.toString())).body()
        }
    }

    override suspend fun categorizeExpense(
        merchant: String,
        description: String?
    ): Result<ExpenseCategory> {
        return runCatching {
            httpClient.post(Expenses.Categorize()) {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "merchant" to merchant,
                    "description" to description
                ))
            }.body()
        }
    }

    // ============================================================================
    // BILL MANAGEMENT (Supplier Invoices / Cash-Out)
    // ============================================================================

    override suspend fun createBill(request: CreateBillRequest): Result<FinancialDocumentDto.BillDto> {
        return runCatching {
            httpClient.post(Bills()) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun getBill(id: BillId): Result<FinancialDocumentDto.BillDto> {
        return runCatching {
            httpClient.get(Bills.Id(id = id.toString())).body()
        }
    }

    override suspend fun listBills(
        status: BillStatus?,
        category: ExpenseCategory?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        limit: Int,
        offset: Int
    ): Result<PaginatedResponse<FinancialDocumentDto.BillDto>> {
        return runCatching {
            httpClient.get(Bills(
                status = status,
                category = category,
                fromDate = fromDate,
                toDate = toDate,
                limit = limit,
                offset = offset
            )).body()
        }
    }

    override suspend fun listOverdueBills(): Result<List<FinancialDocumentDto.BillDto>> {
        return runCatching {
            httpClient.get(Bills.Overdue()).body()
        }
    }

    override suspend fun updateBill(
        billId: BillId,
        request: CreateBillRequest
    ): Result<FinancialDocumentDto.BillDto> {
        return runCatching {
            httpClient.put(Bills.Id(id = billId.toString())) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun markBillPaid(
        billId: BillId,
        request: MarkBillPaidRequest
    ): Result<FinancialDocumentDto.BillDto> {
        return runCatching {
            val billIdRoute = Bills.Id(id = billId.toString())
            httpClient.post(Bills.Id.Pay(parent = billIdRoute)) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun deleteBill(billId: BillId): Result<Unit> {
        return runCatching {
            httpClient.delete(Bills.Id(id = billId.toString())).body()
        }
    }

    // ============================================================================
    // DOCUMENT/ATTACHMENT MANAGEMENT
    // ============================================================================

    override suspend fun uploadInvoiceDocument(
        invoiceId: InvoiceId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): Result<AttachmentId> {
        return runCatching {
            val invoiceIdRoute = Invoices.Id(id = invoiceId.toString())
            httpClient.submitFormWithBinaryData(
                url = "/api/v1/invoices/$invoiceId/attachments",
                formData = formData {
                    append(
                        key = "file",
                        value = fileContent,
                        headers = Headers.build {
                            append(
                                HttpHeaders.ContentDisposition,
                                "form-data; name=\"file\"; filename=\"$filename\""
                            )
                            append(HttpHeaders.ContentType, contentType)
                        }
                    )
                }
            ).body()
        }
    }

    override suspend fun uploadExpenseReceipt(
        expenseId: ExpenseId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): Result<AttachmentId> {
        return runCatching {
            httpClient.submitFormWithBinaryData(
                url = "/api/v1/expenses/$expenseId/attachments",
                formData = formData {
                    append(
                        key = "file",
                        value = fileContent,
                        headers = Headers.build {
                            append(
                                HttpHeaders.ContentDisposition,
                                "form-data; name=\"file\"; filename=\"$filename\""
                            )
                            append(HttpHeaders.ContentType, contentType)
                        }
                    )
                }
            ).body()
        }
    }

    override suspend fun getInvoiceAttachments(invoiceId: InvoiceId): Result<List<AttachmentDto>> {
        return runCatching {
            val invoiceIdRoute = Invoices.Id(id = invoiceId.toString())
            httpClient.get(Invoices.Id.Attachments(parent = invoiceIdRoute)).body()
        }
    }

    override suspend fun getExpenseAttachments(expenseId: ExpenseId): Result<List<AttachmentDto>> {
        return runCatching {
            val expenseIdRoute = Expenses.Id(id = expenseId.toString())
            httpClient.get(Expenses.Id.Attachments(parent = expenseIdRoute)).body()
        }
    }

    override suspend fun getAttachmentDownloadUrl(attachmentId: AttachmentId): Result<String> {
        return runCatching {
            val attachmentIdRoute = Attachments.Id(id = attachmentId.toString())
            httpClient.get(Attachments.Id.DownloadUrl(parent = attachmentIdRoute)).body()
        }
    }

    override suspend fun deleteAttachment(attachmentId: AttachmentId): Result<Unit> {
        return runCatching {
            httpClient.delete(Attachments.Id(id = attachmentId.toString())).body()
        }
    }

    // ============================================================================
    // GENERIC DOCUMENT UPLOAD (MinIO Storage)
    // ============================================================================

    override suspend fun uploadDocument(
        fileContent: ByteArray,
        filename: String,
        contentType: String,
        prefix: String
    ): Result<DocumentDto> {
        return runCatching {
            val response: DocumentUploadResponse = httpClient.submitFormWithBinaryData(
                url = "/api/v1/documents/upload",
                formData = formData {
                    append(
                        key = "file",
                        value = fileContent,
                        headers = Headers.build {
                            append(
                                HttpHeaders.ContentDisposition,
                                "form-data; name=\"file\"; filename=\"$filename\""
                            )
                            append(HttpHeaders.ContentType, contentType)
                        }
                    )
                    append("prefix", prefix)
                }
            ).body()
            response.document
        }
    }

    override suspend fun uploadDocumentWithProgress(
        fileContent: ByteArray,
        filename: String,
        contentType: String,
        prefix: String,
        onProgress: (Float) -> Unit
    ): Result<DocumentDto> {
        return runCatching {
            val response: DocumentUploadResponse = httpClient.submitFormWithBinaryData(
                url = "/api/v1/documents/upload",
                formData = formData {
                    append(
                        key = "file",
                        value = fileContent,
                        headers = Headers.build {
                            append(
                                HttpHeaders.ContentDisposition,
                                "form-data; name=\"file\"; filename=\"$filename\""
                            )
                            append(HttpHeaders.ContentType, contentType)
                        }
                    )
                    append("prefix", prefix)
                }
            ) {
                onUpload { bytesSentTotal, contentLength ->
                    val progress = if (contentLength != null && contentLength > 0) {
                        bytesSentTotal.toFloat() / contentLength.toFloat()
                    } else {
                        0f
                    }
                    onProgress(progress.coerceIn(0f, 1f))
                }
            }.body()
            response.document
        }
    }

    override suspend fun getDocument(documentId: DocumentId): Result<DocumentDto> {
        return runCatching {
            httpClient.get(Documents.Id(id = documentId.toString())).body()
        }
    }

    override suspend fun deleteDocument(documentId: DocumentId): Result<Unit> {
        return runCatching {
            httpClient.delete(Documents.Id(id = documentId.toString())).body()
        }
    }

    // ============================================================================
    // STATISTICS & OVERVIEW
    // ============================================================================

    override suspend fun listCashflowDocuments(
        fromDate: LocalDate?,
        toDate: LocalDate?,
        limit: Int,
        offset: Int
    ): Result<PaginatedResponse<FinancialDocumentDto>> {
        return runCatching {
            httpClient.get(Cashflow.Documents(
                fromDate = fromDate,
                toDate = toDate,
                limit = limit,
                offset = offset
            )).body()
        }
    }

    override suspend fun getCashflowOverview(
        fromDate: LocalDate,
        toDate: LocalDate
    ): Result<CashflowOverview> {
        return runCatching {
            httpClient.get(Cashflow.Overview(
                fromDate = fromDate,
                toDate = toDate
            )).body()
        }
    }

    // ============================================================================
    // DOCUMENT PROCESSING (AI Extraction Pipeline)
    // ============================================================================

    override suspend fun listDocumentProcessing(
        statuses: List<ProcessingStatus>,
        page: Int,
        limit: Int
    ): Result<DocumentProcessingListResponse> {
        return runCatching {
            // Use comma-separated dbValues for cleaner URLs
            val statusParam = if (statuses.isNotEmpty()) {
                statuses.joinToString(",") { it.dbValue }
            } else null
            httpClient.get(Documents.Processing(
                status = statusParam,
                page = page,
                limit = limit
            )).body()
        }
    }

    // ============================================================================
    // PEPPOL E-INVOICING
    // ============================================================================

    override suspend fun getPeppolProviders(): Result<List<String>> {
        return runCatching {
            val response: ProvidersResponse = httpClient.get(Peppol.Providers()).body()
            response.providers
        }
    }

    override suspend fun getPeppolSettings(): Result<PeppolSettingsDto?> {
        return runCatching {
            val response = httpClient.get(Peppol.Settings())
            if (response.status.value == 404) {
                null
            } else {
                response.body<PeppolSettingsDto>()
            }
        }
    }

    override suspend fun savePeppolSettings(request: SavePeppolSettingsRequest): Result<PeppolSettingsDto> {
        return runCatching {
            httpClient.put(Peppol.Settings()) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun deletePeppolSettings(): Result<Unit> {
        return runCatching {
            httpClient.delete(Peppol.Settings()).body()
        }
    }

    override suspend fun testPeppolConnection(): Result<Boolean> {
        return runCatching {
            val settingsRoute = Peppol.Settings()
            val response: TestConnectionResponse = httpClient.post(Peppol.Settings.Test(parent = settingsRoute)).body()
            response.success
        }
    }

    override suspend fun verifyPeppolRecipient(peppolId: String): Result<PeppolVerifyResponse> {
        return runCatching {
            httpClient.post(Peppol.Verify()) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("peppolId" to peppolId))
            }.body()
        }
    }

    override suspend fun validateInvoiceForPeppol(invoiceId: InvoiceId): Result<PeppolValidationResult> {
        return runCatching {
            val sendRoute = Peppol.Send()
            httpClient.post(Peppol.Send.Validate(parent = sendRoute, invoiceId = invoiceId.toString())).body()
        }
    }

    override suspend fun sendInvoiceViaPeppol(invoiceId: InvoiceId): Result<SendInvoiceViaPeppolResponse> {
        return runCatching {
            val sendRoute = Peppol.Send()
            httpClient.post(Peppol.Send.Invoice(parent = sendRoute, invoiceId = invoiceId.toString())).body()
        }
    }

    override suspend fun pollPeppolInbox(): Result<PeppolInboxPollResponse> {
        return runCatching {
            val inboxRoute = Peppol.Inbox()
            httpClient.post(Peppol.Inbox.Poll(parent = inboxRoute)).body()
        }
    }

    override suspend fun listPeppolTransmissions(
        direction: PeppolTransmissionDirection?,
        status: PeppolStatus?,
        limit: Int,
        offset: Int
    ): Result<List<PeppolTransmissionDto>> {
        return runCatching {
            httpClient.get(Peppol.Transmissions(
                direction = direction,
                status = status,
                limit = limit,
                offset = offset
            )).body()
        }
    }

    override suspend fun getPeppolTransmissionForInvoice(invoiceId: InvoiceId): Result<PeppolTransmissionDto?> {
        return runCatching {
            val transmissionsRoute = Peppol.Transmissions()
            val response = httpClient.get(Peppol.Transmissions.ByInvoice(
                parent = transmissionsRoute,
                invoiceId = invoiceId.toString()
            ))
            if (response.status.value == 404) {
                null
            } else {
                response.body<PeppolTransmissionDto>()
            }
        }
    }
}

// Internal response DTOs for Peppol endpoints
@kotlinx.serialization.Serializable
private data class ProvidersResponse(val providers: List<String>)

@kotlinx.serialization.Serializable
private data class TestConnectionResponse(val success: Boolean)
