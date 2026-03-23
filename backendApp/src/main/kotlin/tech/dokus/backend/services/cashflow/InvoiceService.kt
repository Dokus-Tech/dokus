package tech.dokus.backend.services.cashflow

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.backend.mappers.from
import tech.dokus.database.entity.InvoiceEntity
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreateInvoiceRequest
import tech.dokus.domain.model.DocDto
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

    suspend fun createInvoice(
        tenantId: TenantId,
        request: CreateInvoiceRequest
    ): Result<DocDto.Invoice.Confirmed> {
        logger.info("Creating invoice for tenant: $tenantId, contact: ${request.contactId}")
        return invoiceRepository.createInvoice(tenantId, request)
            .map { DocDto.Invoice.Confirmed.from(it) }
            .onSuccess { logger.info("Invoice created: ${it.id}") }
            .onFailure { logger.error("Failed to create invoice for tenant: $tenantId", it) }
    }

    suspend fun getInvoice(
        invoiceId: InvoiceId,
        tenantId: TenantId
    ): Result<DocDto.Invoice.Confirmed?> {
        logger.debug("Fetching invoice: {} for tenant: {}", invoiceId, tenantId)
        return invoiceRepository.getInvoice(invoiceId, tenantId)
            .map { it?.let { entity -> DocDto.Invoice.Confirmed.from(entity) } }
            .onFailure { logger.error("Failed to fetch invoice: $invoiceId", it) }
    }

    suspend fun listInvoices(
        tenantId: TenantId,
        status: InvoiceStatus? = null,
        direction: DocumentDirection? = null,
        contactId: ContactId? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<DocDto.Invoice.Confirmed>> {
        logger.debug(
            "Listing invoices for tenant: {} (status={}, limit={}, offset={})",
            tenantId,
            status,
            limit,
            offset
        )
        return invoiceRepository.listInvoices(
            tenantId = tenantId,
            status = status,
            direction = direction,
            contactId = contactId,
            fromDate = fromDate,
            toDate = toDate,
            limit = limit,
            offset = offset
        )
            .map { page ->
                PaginatedResponse(
                    items = page.items.map { DocDto.Invoice.Confirmed.from(it) },
                    total = page.total,
                    limit = page.limit,
                    offset = page.offset
                )
            }
            .onSuccess { logger.debug("Retrieved ${it.items.size} invoices (total=${it.total})") }
            .onFailure { logger.error("Failed to list invoices for tenant: $tenantId", it) }
    }

    suspend fun getLatestInvoiceForContact(
        tenantId: TenantId,
        contactId: ContactId
    ): Result<DocDto.Invoice.Confirmed?> {
        return invoiceRepository.getLatestInvoiceForContact(tenantId, contactId)
            .map { it?.let { entity -> DocDto.Invoice.Confirmed.from(entity) } }
            .onFailure { logger.error("Failed to get latest invoice for contact: $contactId", it) }
    }

    suspend fun listOverdueInvoices(
        tenantId: TenantId,
        direction: DocumentDirection = DocumentDirection.Outbound
    ): Result<List<DocDto.Invoice.Confirmed>> {
        logger.debug("Listing overdue invoices for tenant: {}", tenantId)
        return invoiceRepository.listOverdueInvoices(tenantId, direction)
            .map { list -> list.map { DocDto.Invoice.Confirmed.from(it) } }
            .onSuccess { logger.debug("Retrieved ${it.size} overdue invoices") }
            .onFailure { logger.error("Failed to list overdue invoices for tenant: $tenantId", it) }
    }

    suspend fun updateInvoice(
        invoiceId: InvoiceId,
        tenantId: TenantId,
        request: CreateInvoiceRequest
    ): Result<DocDto.Invoice.Confirmed> {
        logger.info("Updating invoice: $invoiceId for tenant: $tenantId")
        return invoiceRepository.updateInvoice(invoiceId, tenantId, request)
            .map { DocDto.Invoice.Confirmed.from(it) }
            .onSuccess { logger.info("Invoice updated: $invoiceId") }
            .onFailure { logger.error("Failed to update invoice: $invoiceId", it) }
    }

    /**
     * Get raw invoice entity — for internal use by services that need entity fields
     * (e.g., PeppolService for UBL generation).
     */
    internal suspend fun getInvoiceEntity(
        invoiceId: InvoiceId,
        tenantId: TenantId
    ): Result<InvoiceEntity?> {
        return invoiceRepository.getInvoice(invoiceId, tenantId)
    }

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

    suspend fun deleteInvoice(
        invoiceId: InvoiceId,
        tenantId: TenantId
    ): Result<Boolean> {
        logger.info("Deleting invoice: $invoiceId")
        return invoiceRepository.deleteInvoice(invoiceId, tenantId)
            .onSuccess { logger.info("Invoice deleted: $invoiceId") }
            .onFailure { logger.error("Failed to delete invoice: $invoiceId", it) }
    }

    suspend fun exists(
        invoiceId: InvoiceId,
        tenantId: TenantId
    ): Result<Boolean> {
        return invoiceRepository.exists(invoiceId, tenantId)
    }

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
        val newRemaining = if (remaining.isNegative) Money.zero(remaining.currency) else remaining
        val newStatus = if (newRemaining.minor <= 0) CashflowEntryStatus.Paid else entry.status
        val paidAt = if (newStatus == CashflowEntryStatus.Paid && entry.status != CashflowEntryStatus.Paid) {
            LocalDateTime(
                request.paymentDate.year,
                request.paymentDate.monthNumber,
                request.paymentDate.dayOfMonth,
                12,
                0,
                0
            )
        } else {
            entry.paidAt
        }

        cashflowEntriesRepository.updateRemainingAmountAndStatus(
            entryId = entry.id,
            tenantId = tenantId,
            newRemainingAmount = newRemaining,
            newStatus = newStatus,
            paidAt = paidAt
        ).getOrThrow()
    }
}
