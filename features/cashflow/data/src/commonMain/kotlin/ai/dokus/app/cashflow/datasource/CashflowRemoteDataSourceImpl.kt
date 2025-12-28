package ai.dokus.app.cashflow.datasource

import tech.dokus.domain.enums.BillStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PeppolTransmissionDirection
import tech.dokus.domain.ids.AttachmentId
import tech.dokus.domain.ids.BillId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.ExpenseId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.model.AttachmentDto
import tech.dokus.domain.model.CashflowOverview
import tech.dokus.domain.model.CreateBillRequest
import tech.dokus.domain.model.CreateExpenseRequest
import tech.dokus.domain.model.CreateInvoiceRequest
import tech.dokus.domain.model.ConfirmDocumentRequest
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentIngestionDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.DocumentUploadResponse
import tech.dokus.domain.model.ReprocessRequest
import tech.dokus.domain.model.ReprocessResponse
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.domain.model.UpdateDraftResponse
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.MarkBillPaidRequest
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.model.PeppolConnectRequest
import tech.dokus.domain.model.PeppolConnectResponse
import tech.dokus.domain.model.PeppolInboxPollResponse
import tech.dokus.domain.model.PeppolSettingsDto
import tech.dokus.domain.model.PeppolTransmissionDto
import tech.dokus.domain.model.PeppolValidationResult
import tech.dokus.domain.model.PeppolVerifyResponse
import tech.dokus.domain.model.RecordPaymentRequest
import tech.dokus.domain.model.SavePeppolSettingsRequest
import tech.dokus.domain.model.SendInvoiceViaPeppolResponse
import tech.dokus.domain.routes.Attachments
import tech.dokus.domain.routes.Bills
import tech.dokus.domain.routes.Cashflow
import tech.dokus.domain.routes.Documents
import tech.dokus.domain.routes.Expenses
import tech.dokus.domain.routes.Invoices
import tech.dokus.domain.routes.Peppol
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
            httpClient.post(Invoices.Id.Emails(parent = invoiceIdRoute)) {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "recipientEmail" to recipientEmail,
                    "message" to message
                ))
            }.body()
        }
    }

    // Note: markInvoiceAsSent was removed - use updateInvoiceStatus with SENT status instead.
    // Note: calculateInvoiceTotals was removed - compute client-side or include in invoice response.

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

    // Note: categorizeExpense was removed - compute client-side or use AI service directly.

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
            httpClient.post(Bills.Id.Payments(parent = billIdRoute)) {
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
            httpClient.get(Attachments.Id.Url(parent = attachmentIdRoute)).body()
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
            httpClient.get(Cashflow.CashflowDocuments(
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
    // DOCUMENT MANAGEMENT (AI Extraction Pipeline)
    // ============================================================================

    override suspend fun listDocuments(
        draftStatus: DraftStatus?,
        documentType: DocumentType?,
        ingestionStatus: IngestionStatus?,
        search: String?,
        page: Int,
        limit: Int
    ): Result<PaginatedResponse<DocumentRecordDto>> {
        return runCatching {
            httpClient.get(Documents(
                draftStatus = draftStatus?.name,
                documentType = documentType?.name,
                ingestionStatus = ingestionStatus?.name,
                search = search,
                page = page,
                limit = limit
            )).body()
        }
    }

    override suspend fun getDocumentRecord(documentId: DocumentId): Result<DocumentRecordDto> {
        return runCatching {
            httpClient.get(Documents.Id(id = documentId.toString())).body()
        }
    }

    override suspend fun getDocumentDraft(documentId: DocumentId): Result<DocumentDraftDto> {
        return runCatching {
            val docIdRoute = Documents.Id(id = documentId.toString())
            httpClient.get(Documents.Id.Draft(parent = docIdRoute)).body()
        }
    }

    override suspend fun updateDocumentDraft(
        documentId: DocumentId,
        request: UpdateDraftRequest
    ): Result<UpdateDraftResponse> {
        return runCatching {
            val docIdRoute = Documents.Id(id = documentId.toString())
            httpClient.patch(Documents.Id.Draft(parent = docIdRoute)) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun reprocessDocument(
        documentId: DocumentId,
        request: ReprocessRequest
    ): Result<ReprocessResponse> {
        return runCatching {
            val docIdRoute = Documents.Id(id = documentId.toString())
            httpClient.post(Documents.Id.Reprocess(parent = docIdRoute)) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun confirmDocument(
        documentId: DocumentId,
        request: ConfirmDocumentRequest
    ): Result<DocumentRecordDto> {
        return runCatching {
            val docIdRoute = Documents.Id(id = documentId.toString())
            httpClient.post(Documents.Id.Confirm(parent = docIdRoute)) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun getDocumentIngestions(documentId: DocumentId): Result<List<DocumentIngestionDto>> {
        return runCatching {
            val docIdRoute = Documents.Id(id = documentId.toString())
            httpClient.get(Documents.Id.Ingestions(parent = docIdRoute)).body()
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
            val response: TestConnectionResponse = httpClient.post(Peppol.Settings.ConnectionTests(parent = settingsRoute)).body()
            response.success
        }
    }

    override suspend fun connectPeppol(request: PeppolConnectRequest): Result<PeppolConnectResponse> {
        return runCatching {
            val settingsRoute = Peppol.Settings()
            httpClient.post(Peppol.Settings.Connect(parent = settingsRoute)) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun verifyPeppolRecipient(peppolId: String): Result<PeppolVerifyResponse> {
        return runCatching {
            httpClient.post(Peppol.RecipientValidations()) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("peppolId" to peppolId))
            }.body()
        }
    }

    override suspend fun validateInvoiceForPeppol(invoiceId: InvoiceId): Result<PeppolValidationResult> {
        return runCatching {
            httpClient.post(Peppol.InvoiceValidations(invoiceId = invoiceId.toString())).body()
        }
    }

    override suspend fun sendInvoiceViaPeppol(invoiceId: InvoiceId): Result<SendInvoiceViaPeppolResponse> {
        return runCatching {
            httpClient.post(Peppol.Transmissions(invoiceId = invoiceId.toString())).body()
        }
    }

    override suspend fun pollPeppolInbox(): Result<PeppolInboxPollResponse> {
        return runCatching {
            val inboxRoute = Peppol.Inbox()
            httpClient.post(Peppol.Inbox.Syncs(parent = inboxRoute)).body()
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
            // Use invoiceId filter on transmissions endpoint
            val transmissions = httpClient.get(Peppol.Transmissions(
                invoiceId = invoiceId.toString(),
                limit = 1
            )).body<List<PeppolTransmissionDto>>()
            transmissions.firstOrNull()
        }
    }
}

// Internal response DTOs for Peppol endpoints
@kotlinx.serialization.Serializable
private data class ProvidersResponse(val providers: List<String>)

@kotlinx.serialization.Serializable
private data class TestConnectionResponse(val success: Boolean)
