package ai.dokus.app.cashflow.datasource

import ai.dokus.foundation.domain.enums.BillStatus
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.ids.AttachmentId
import ai.dokus.foundation.domain.ids.BillId
import ai.dokus.foundation.domain.ids.ExpenseId
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.model.AttachmentDto
import ai.dokus.foundation.domain.model.CashflowOverview
import ai.dokus.foundation.domain.model.CreateBillRequest
import ai.dokus.foundation.domain.model.CreateExpenseRequest
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.InvoiceItemDto
import ai.dokus.foundation.domain.model.InvoiceTotals
import ai.dokus.foundation.domain.model.MarkBillPaidRequest
import ai.dokus.foundation.domain.model.PaginatedResponse
import ai.dokus.foundation.domain.model.RecordPaymentRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.datetime.LocalDate

/**
 * HTTP-based implementation of CashflowRemoteDataSource
 * Uses Ktor HttpClient to communicate with the cashflow management API
 */
internal class CashflowRemoteDataSourceImpl(
    private val httpClient: HttpClient
) : CashflowRemoteDataSource {

    // ============================================================================
    // INVOICE MANAGEMENT
    // ============================================================================

    override suspend fun createInvoice(request: CreateInvoiceRequest): Result<FinancialDocumentDto.InvoiceDto> {
        return runCatching {
            httpClient.post("/api/v1/invoices") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun getInvoice(id: InvoiceId): Result<FinancialDocumentDto.InvoiceDto> {
        return runCatching {
            httpClient.get("/api/v1/invoices/$id").body()
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
            httpClient.get("/api/v1/invoices") {
                status?.let { parameter("status", it.name) }
                fromDate?.let { parameter("fromDate", it.toString()) }
                toDate?.let { parameter("toDate", it.toString()) }
                parameter("limit", limit)
                parameter("offset", offset)
            }.body()
        }
    }

    override suspend fun listOverdueInvoices(): Result<List<FinancialDocumentDto.InvoiceDto>> {
        return runCatching {
            httpClient.get("/api/v1/invoices/overdue").body()
        }
    }

    override suspend fun updateInvoiceStatus(
        invoiceId: InvoiceId,
        status: InvoiceStatus
    ): Result<Unit> {
        return runCatching {
            httpClient.patch("/api/v1/invoices/$invoiceId/status") {
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
            httpClient.put("/api/v1/invoices/$invoiceId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun deleteInvoice(invoiceId: InvoiceId): Result<Unit> {
        return runCatching {
            httpClient.delete("/api/v1/invoices/$invoiceId").body()
        }
    }

    override suspend fun recordPayment(request: RecordPaymentRequest): Result<Unit> {
        return runCatching {
            httpClient.post("/api/v1/invoices/${request.invoiceId}/payments") {
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
            httpClient.post("/api/v1/invoices/$invoiceId/send-email") {
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
            httpClient.post("/api/v1/invoices/$invoiceId/mark-sent").body()
        }
    }

    override suspend fun calculateInvoiceTotals(items: List<InvoiceItemDto>): Result<InvoiceTotals> {
        return runCatching {
            httpClient.post("/api/v1/invoices/calculate-totals") {
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
            httpClient.post("/api/v1/expenses") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun getExpense(id: ExpenseId): Result<FinancialDocumentDto.ExpenseDto> {
        return runCatching {
            httpClient.get("/api/v1/expenses/$id").body()
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
            httpClient.get("/api/v1/expenses") {
                category?.let { parameter("category", it.name) }
                fromDate?.let { parameter("fromDate", it.toString()) }
                toDate?.let { parameter("toDate", it.toString()) }
                parameter("limit", limit)
                parameter("offset", offset)
            }.body()
        }
    }

    override suspend fun updateExpense(
        expenseId: ExpenseId,
        request: CreateExpenseRequest
    ): Result<FinancialDocumentDto.ExpenseDto> {
        return runCatching {
            httpClient.put("/api/v1/expenses/$expenseId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun deleteExpense(expenseId: ExpenseId): Result<Unit> {
        return runCatching {
            httpClient.delete("/api/v1/expenses/$expenseId").body()
        }
    }

    override suspend fun categorizeExpense(
        merchant: String,
        description: String?
    ): Result<ExpenseCategory> {
        return runCatching {
            httpClient.post("/api/v1/expenses/categorize") {
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
            httpClient.post("/api/v1/cashflow/cash-out/bills") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun getBill(id: BillId): Result<FinancialDocumentDto.BillDto> {
        return runCatching {
            httpClient.get("/api/v1/cashflow/cash-out/bills/$id").body()
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
            httpClient.get("/api/v1/cashflow/cash-out/bills") {
                status?.let { parameter("status", it.name) }
                category?.let { parameter("category", it.name) }
                fromDate?.let { parameter("fromDate", it.toString()) }
                toDate?.let { parameter("toDate", it.toString()) }
                parameter("limit", limit)
                parameter("offset", offset)
            }.body()
        }
    }

    override suspend fun listOverdueBills(): Result<List<FinancialDocumentDto.BillDto>> {
        return runCatching {
            httpClient.get("/api/v1/cashflow/cash-out/bills/overdue").body()
        }
    }

    override suspend fun updateBill(
        billId: BillId,
        request: CreateBillRequest
    ): Result<FinancialDocumentDto.BillDto> {
        return runCatching {
            httpClient.put("/api/v1/cashflow/cash-out/bills/$billId") {
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
            httpClient.post("/api/v1/cashflow/cash-out/bills/$billId/pay") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun deleteBill(billId: BillId): Result<Unit> {
        return runCatching {
            httpClient.delete("/api/v1/cashflow/cash-out/bills/$billId").body()
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
            httpClient.get("/api/v1/invoices/$invoiceId/attachments").body()
        }
    }

    override suspend fun getExpenseAttachments(expenseId: ExpenseId): Result<List<AttachmentDto>> {
        return runCatching {
            httpClient.get("/api/v1/expenses/$expenseId/attachments").body()
        }
    }

    override suspend fun getAttachmentDownloadUrl(attachmentId: AttachmentId): Result<String> {
        return runCatching {
            httpClient.get("/api/v1/attachments/$attachmentId/download-url").body()
        }
    }

    override suspend fun deleteAttachment(attachmentId: AttachmentId): Result<Unit> {
        return runCatching {
            httpClient.delete("/api/v1/attachments/$attachmentId").body()
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
    ): Result<DocumentUploadResult> {
        return runCatching {
            httpClient.submitFormWithBinaryData(
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
            httpClient.get("/api/v1/cashflow/documents") {
                fromDate?.let { parameter("fromDate", it.toString()) }
                toDate?.let { parameter("toDate", it.toString()) }
                parameter("limit", limit)
                parameter("offset", offset)
            }.body()
        }
    }

    override suspend fun getCashflowOverview(
        fromDate: LocalDate,
        toDate: LocalDate
    ): Result<CashflowOverview> {
        return runCatching {
            httpClient.get("/api/v1/cashflow/overview") {
                parameter("fromDate", fromDate.toString())
                parameter("toDate", toDate.toString())
            }.body()
        }
    }
}
