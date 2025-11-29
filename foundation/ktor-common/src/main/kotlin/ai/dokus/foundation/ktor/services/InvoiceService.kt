package ai.dokus.foundation.ktor.services

import ai.dokus.foundation.domain.ids.ClientId
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.InvoiceItemDto
import ai.dokus.foundation.domain.model.InvoiceTotals
import ai.dokus.foundation.domain.model.RecordPaymentRequest
import ai.dokus.foundation.domain.model.UpdateInvoiceStatusRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.rpc.annotations.Rpc
import kotlin.time.ExperimentalTime

@Rpc
interface InvoiceService {
    /**
     * Creates a new invoice with items
     * Automatically generates invoice number and calculates totals
     *
     * @param request The invoice creation request with all details
     * @return The created invoice
     * @throws IllegalArgumentException if validation fails
     */
    suspend fun create(request: CreateInvoiceRequest): FinancialDocumentDto.InvoiceDto

    /**
     * Updates an existing invoice
     * Can only update draft invoices
     *
     * @param invoiceId The invoice's unique identifier
     * @param issueDate The new issue date (optional)
     * @param dueDate The new due date (optional)
     * @param notes Additional notes (optional)
     * @param termsAndConditions Terms and conditions (optional)
     * @throws IllegalArgumentException if invoice not found or not in draft status
     */
    suspend fun update(
        invoiceId: InvoiceId,
        issueDate: LocalDate? = null,
        dueDate: LocalDate? = null,
        notes: String? = null,
        termsAndConditions: String? = null
    )

    /**
     * Updates invoice items
     * Can only update draft invoices
     * Automatically recalculates totals
     *
     * @param invoiceId The invoice's unique identifier
     * @param items The new list of invoice items
     * @throws IllegalArgumentException if invoice not found or not in draft status
     */
    suspend fun updateItems(invoiceId: InvoiceId, items: List<InvoiceItemDto>)

    /**
     * Soft deletes an invoice by marking it as cancelled
     * Can only delete draft invoices
     *
     * @param invoiceId The invoice's unique identifier
     * @throws IllegalArgumentException if invoice not found or not in draft status
     */
    suspend fun delete(invoiceId: InvoiceId)

    /**
     * Finds an invoice by its unique ID
     *
     * @param id The invoice's unique identifier
     * @return The invoice if found, null otherwise
     */
    suspend fun findById(id: InvoiceId): FinancialDocumentDto.InvoiceDto?

    /**
     * Lists all invoices for a tenant
     *
     * @param tenantId The tenant's unique identifier
     * @param status Filter by status (optional)
     * @param clientId Filter by client (optional)
     * @param fromDate Filter invoices issued on or after this date (optional)
     * @param toDate Filter invoices issued on or before this date (optional)
     * @param limit Maximum number of results (optional)
     * @param offset Pagination offset (optional)
     * @return List of invoices
     */
    suspend fun listByTenant(
        tenantId: TenantId,
        status: InvoiceStatus? = null,
        clientId: ClientId? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int? = null,
        offset: Int? = null
    ): List<FinancialDocumentDto.InvoiceDto>

    /**
     * Lists invoices for a specific client
     *
     * @param clientId The client's unique identifier
     * @param status Filter by status (optional)
     * @return List of invoices
     */
    suspend fun listByClient(clientId: ClientId, status: InvoiceStatus? = null): List<FinancialDocumentDto.InvoiceDto>

    /**
     * Lists overdue invoices for a tenant
     *
     * @param tenantId The tenant's unique identifier
     * @return List of overdue invoices
     */
    suspend fun listOverdue(tenantId: TenantId): List<FinancialDocumentDto.InvoiceDto>

    /**
     * Updates the status of an invoice
     *
     * @param request The status update request
     * @throws IllegalArgumentException if invoice not found or invalid status transition
     */
    suspend fun updateStatus(request: UpdateInvoiceStatusRequest)

    /**
     * Records a payment for an invoice
     * Automatically updates invoice status when fully paid
     *
     * @param request The payment recording request
     * @throws IllegalArgumentException if invoice not found or validation fails
     */
    suspend fun recordPayment(request: RecordPaymentRequest)

    /**
     * Sends an invoice via email to the client
     *
     * @param invoiceId The invoice's unique identifier
     * @param recipientEmail The recipient's email (optional, defaults to client email)
     * @param ccEmails Additional CC recipients (optional)
     * @param message Custom message to include in email (optional)
     * @throws IllegalArgumentException if invoice not found or client has no email
     */
    suspend fun sendViaEmail(
        invoiceId: InvoiceId,
        recipientEmail: String? = null,
        ccEmails: List<String>? = null,
        message: String? = null
    )

    /**
     * Sends an invoice via Peppol e-invoicing network
     * Required for Belgian B2B invoices from 2026
     *
     * @param invoiceId The invoice's unique identifier
     * @throws IllegalArgumentException if invoice not found or client not Peppol-enabled
     */
    suspend fun sendViaPeppol(invoiceId: InvoiceId)

    /**
     * Generates a PDF for an invoice
     *
     * @param invoiceId The invoice's unique identifier
     * @return The PDF content as ByteArray
     * @throws IllegalArgumentException if invoice not found
     */
    suspend fun generatePDF(invoiceId: InvoiceId): ByteArray

    /**
     * Generates a payment link for an invoice
     * Integrates with Stripe/Mollie/etc
     *
     * @param invoiceId The invoice's unique identifier
     * @param expiresAt When the payment link expires (optional)
     * @return The payment link URL
     * @throws IllegalArgumentException if invoice not found or already paid
     */
    @OptIn(ExperimentalTime::class)
    suspend fun generatePaymentLink(invoiceId: InvoiceId, expiresAt: Instant? = null): String

    /**
     * Marks an invoice as sent
     *
     * @param invoiceId The invoice's unique identifier
     * @throws IllegalArgumentException if invoice not found
     */
    suspend fun markAsSent(invoiceId: InvoiceId)

    /**
     * Watches invoice updates for a tenant in real-time
     * Returns a Flow that emits whenever invoices are created or updated
     *
     * @param tenantId The tenant's unique identifier
     * @return Flow of invoice updates
     */
    fun watchInvoices(tenantId: TenantId): Flow<FinancialDocumentDto.InvoiceDto>

    /**
     * Calculates totals for an invoice
     * Used for validation and preview
     *
     * @param items List of invoice items
     * @return Invoice totals calculation
     */
    suspend fun calculateTotals(items: List<InvoiceItemDto>): InvoiceTotals

    /**
     * Gets invoice statistics for a tenant
     *
     * @param tenantId The tenant's unique identifier
     * @param fromDate Start date for statistics (optional)
     * @param toDate End date for statistics (optional)
     * @return Map of statistics (totalInvoiced, totalPaid, totalOverdue, etc.)
     */
    suspend fun getStatistics(
        tenantId: TenantId,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): Map<String, Money>
}
