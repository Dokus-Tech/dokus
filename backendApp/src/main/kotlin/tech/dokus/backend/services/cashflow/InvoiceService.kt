package tech.dokus.backend.services.cashflow

import kotlinx.datetime.LocalDate
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreateInvoiceRequest
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.RecordPaymentRequest
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.foundation.backend.utils.loggerFor
import java.util.UUID

/**
 * Service for invoice business operations.
 *
 * Invoices represent outgoing invoices to clients (Cash-In).
 * This service handles all business logic related to invoices
 * and delegates data access to the repository layer.
 */
class InvoiceService(
    private val invoiceRepository: InvoiceRepository,
    private val cashflowEntriesRepository: CashflowEntriesRepository
) {
    private val logger = loggerFor()

    /**
     * Create a new invoice for a tenant.
     */
    suspend fun createInvoice(
        tenantId: TenantId,
        request: CreateInvoiceRequest
    ): Result<FinancialDocumentDto.InvoiceDto> {
        logger.info("Creating invoice for tenant: $tenantId, contact: ${request.contactId}")
        return invoiceRepository.createInvoice(tenantId, request)
            .onSuccess { logger.info("Invoice created: ${it.id}") }
            .onFailure { logger.error("Failed to create invoice for tenant: $tenantId", it) }
    }

    /**
     * Get an invoice by ID.
     */
    suspend fun getInvoice(
        invoiceId: InvoiceId,
        tenantId: TenantId
    ): Result<FinancialDocumentDto.InvoiceDto?> {
        logger.debug("Fetching invoice: {} for tenant: {}", invoiceId, tenantId)
        return invoiceRepository.getInvoice(invoiceId, tenantId)
            .onFailure { logger.error("Failed to fetch invoice: $invoiceId", it) }
    }

    /**
     * List invoices with optional filters.
     */
    suspend fun listInvoices(
        tenantId: TenantId,
        status: InvoiceStatus? = null,
        direction: DocumentDirection? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<FinancialDocumentDto.InvoiceDto>> {
        logger.debug(
            "Listing invoices for tenant: {} (status={}, limit={}, offset={})",
            tenantId,
            status,
            limit,
            offset
        )
        return invoiceRepository.listInvoices(tenantId, status, direction, fromDate, toDate, limit, offset)
            .onSuccess { logger.debug("Retrieved ${it.items.size} invoices (total=${it.total})") }
            .onFailure { logger.error("Failed to list invoices for tenant: $tenantId", it) }
    }

    /**
     * List overdue invoices for a tenant.
     */
    suspend fun listOverdueInvoices(
        tenantId: TenantId,
        direction: DocumentDirection = DocumentDirection.Outbound
    ): Result<List<FinancialDocumentDto.InvoiceDto>> {
        logger.debug("Listing overdue invoices for tenant: {}", tenantId)
        return invoiceRepository.listOverdueInvoices(tenantId, direction)
            .onSuccess { logger.debug("Retrieved ${it.size} overdue invoices") }
            .onFailure { logger.error("Failed to list overdue invoices for tenant: $tenantId", it) }
    }

    /**
     * Update invoice details.
     */
    suspend fun updateInvoice(
        invoiceId: InvoiceId,
        tenantId: TenantId,
        request: CreateInvoiceRequest
    ): Result<FinancialDocumentDto.InvoiceDto> {
        logger.info("Updating invoice: $invoiceId for tenant: $tenantId")
        return invoiceRepository.updateInvoice(invoiceId, tenantId, request)
            .onSuccess { logger.info("Invoice updated: $invoiceId") }
            .onFailure { logger.error("Failed to update invoice: $invoiceId", it) }
    }

    /**
     * Update invoice status.
     */
    suspend fun updateInvoiceStatus(
        invoiceId: InvoiceId,
        tenantId: TenantId,
        status: InvoiceStatus
    ): Result<Boolean> {
        logger.info("Updating invoice status: $invoiceId -> $status")
        return invoiceRepository.updateInvoiceStatus(invoiceId, tenantId, status)
            .onSuccess { logger.info("Invoice status updated: $invoiceId -> $status") }
            .onFailure { logger.error("Failed to update invoice status: $invoiceId", it) }
    }

    /**
     * Delete an invoice.
     */
    suspend fun deleteInvoice(
        invoiceId: InvoiceId,
        tenantId: TenantId
    ): Result<Boolean> {
        logger.info("Deleting invoice: $invoiceId")
        return invoiceRepository.deleteInvoice(invoiceId, tenantId)
            .onSuccess { logger.info("Invoice deleted: $invoiceId") }
            .onFailure { logger.error("Failed to delete invoice: $invoiceId", it) }
    }

    /**
     * Check if an invoice exists.
     */
    suspend fun exists(
        invoiceId: InvoiceId,
        tenantId: TenantId
    ): Result<Boolean> {
        return invoiceRepository.exists(invoiceId, tenantId)
    }

    /**
     * Record a payment for an invoice and synchronize its cashflow entry (if present).
     */
    suspend fun recordPayment(
        invoiceId: InvoiceId,
        tenantId: TenantId,
        request: RecordPaymentRequest
    ): Result<Unit> = runCatching {
        val update = invoiceRepository.recordPayment(
            invoiceId = invoiceId,
            tenantId = tenantId,
            amount = request.amount,
            paymentDate = request.paymentDate,
            paymentMethod = request.paymentMethod
        ).getOrThrow()

        // Best-effort: update cashflow entry projection if it exists.
        val entry = cashflowEntriesRepository.getBySource(
            tenantId = tenantId,
            sourceType = CashflowSourceType.Invoice,
            sourceId = UUID.fromString(invoiceId.toString())
        ).getOrNull() ?: return@runCatching

        val remaining = update.totalAmount - update.paidAmount
        val newRemaining = if (remaining.isNegative) tech.dokus.domain.Money.ZERO else remaining
        val newStatus = if (newRemaining.minor <= 0) CashflowEntryStatus.Paid else entry.status

        cashflowEntriesRepository.updateRemainingAmountAndStatus(
            entryId = entry.id,
            tenantId = tenantId,
            newRemainingAmount = newRemaining,
            newStatus = newStatus
        ).getOrThrow()
    }
}
