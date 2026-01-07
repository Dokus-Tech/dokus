package tech.dokus.backend.services.cashflow

import kotlinx.datetime.LocalDate
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.foundation.backend.utils.loggerFor
import java.util.UUID

/**
 * Service for cashflow entry business operations.
 *
 * Cashflow entries are projections of financial facts (Invoice, Bill, Expense).
 * They are created during document confirmation and updated when payments are recorded.
 *
 * This is the normalized source of truth for cashflow data.
 */
class CashflowEntriesService(
    private val cashflowEntriesRepository: CashflowEntriesRepository
) {
    private val logger = loggerFor()

    /**
     * Create a cashflow entry for an invoice (Cash-In).
     */
    suspend fun createFromInvoice(
        tenantId: TenantId,
        invoiceId: UUID,
        documentId: DocumentId?,
        dueDate: LocalDate,
        amountGross: Money,
        amountVat: Money,
        customerId: ContactId?
    ): Result<CashflowEntry> {
        logger.info("Creating cashflow entry for invoice: $invoiceId, tenant: $tenantId")
        return cashflowEntriesRepository.createEntry(
            tenantId = tenantId,
            sourceType = CashflowSourceType.Invoice,
            sourceId = invoiceId,
            documentId = documentId,
            direction = CashflowDirection.In,
            eventDate = dueDate,
            amountGross = amountGross,
            amountVat = amountVat,
            counterpartyId = customerId
        )
            .onSuccess { logger.info("Cashflow entry created: ${it.id} for invoice: $invoiceId") }
            .onFailure { logger.error("Failed to create cashflow entry for invoice: $invoiceId", it) }
    }

    /**
     * Create a cashflow entry for a bill (Cash-Out).
     */
    suspend fun createFromBill(
        tenantId: TenantId,
        billId: UUID,
        documentId: DocumentId?,
        dueDate: LocalDate,
        amountGross: Money,
        amountVat: Money,
        vendorId: ContactId?
    ): Result<CashflowEntry> {
        logger.info("Creating cashflow entry for bill: $billId, tenant: $tenantId")
        return cashflowEntriesRepository.createEntry(
            tenantId = tenantId,
            sourceType = CashflowSourceType.Bill,
            sourceId = billId,
            documentId = documentId,
            direction = CashflowDirection.Out,
            eventDate = dueDate,
            amountGross = amountGross,
            amountVat = amountVat,
            counterpartyId = vendorId
        )
            .onSuccess { logger.info("Cashflow entry created: ${it.id} for bill: $billId") }
            .onFailure { logger.error("Failed to create cashflow entry for bill: $billId", it) }
    }

    /**
     * Create a cashflow entry for an expense (Cash-Out).
     */
    suspend fun createFromExpense(
        tenantId: TenantId,
        expenseId: UUID,
        documentId: DocumentId?,
        expenseDate: LocalDate,
        amountGross: Money,
        amountVat: Money,
        vendorId: ContactId?
    ): Result<CashflowEntry> {
        logger.info("Creating cashflow entry for expense: $expenseId, tenant: $tenantId")
        return cashflowEntriesRepository.createEntry(
            tenantId = tenantId,
            sourceType = CashflowSourceType.Expense,
            sourceId = expenseId,
            documentId = documentId,
            direction = CashflowDirection.Out,
            eventDate = expenseDate,
            amountGross = amountGross,
            amountVat = amountVat,
            counterpartyId = vendorId
        )
            .onSuccess { logger.info("Cashflow entry created: ${it.id} for expense: $expenseId") }
            .onFailure { logger.error("Failed to create cashflow entry for expense: $expenseId", it) }
    }

    /**
     * Get cashflow entry by ID.
     */
    suspend fun getEntry(
        entryId: CashflowEntryId,
        tenantId: TenantId
    ): Result<CashflowEntry?> {
        logger.debug("Fetching cashflow entry: {} for tenant: {}", entryId, tenantId)
        return cashflowEntriesRepository.getEntry(entryId, tenantId)
            .onFailure { logger.error("Failed to fetch cashflow entry: $entryId", it) }
    }

    /**
     * Get cashflow entry by source (Invoice/Bill/Expense ID).
     */
    suspend fun getBySource(
        tenantId: TenantId,
        sourceType: CashflowSourceType,
        sourceId: UUID
    ): Result<CashflowEntry?> {
        logger.debug("Fetching cashflow entry for source: {} {}", sourceType, sourceId)
        return cashflowEntriesRepository.getBySource(tenantId, sourceType, sourceId)
            .onFailure { logger.error("Failed to fetch cashflow entry for source: $sourceType $sourceId", it) }
    }

    /**
     * List cashflow entries with optional filters.
     */
    suspend fun listEntries(
        tenantId: TenantId,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        direction: CashflowDirection? = null,
        status: CashflowEntryStatus? = null
    ): Result<List<CashflowEntry>> {
        logger.debug(
            "Listing cashflow entries for tenant: {} (from={}, to={}, direction={}, status={})",
            tenantId, fromDate, toDate, direction, status
        )
        return cashflowEntriesRepository.listEntries(tenantId, fromDate, toDate, direction, status)
            .onSuccess { logger.debug("Retrieved ${it.size} cashflow entries") }
            .onFailure { logger.error("Failed to list cashflow entries for tenant: $tenantId", it) }
    }

    /**
     * Record a payment against a cashflow entry.
     * Updates the remaining amount and status accordingly.
     */
    suspend fun recordPayment(
        entryId: CashflowEntryId,
        tenantId: TenantId,
        paymentAmount: Money
    ): Result<Boolean> {
        logger.info("Recording payment of $paymentAmount for cashflow entry: $entryId")

        // Get current entry to calculate new remaining amount
        val entry = cashflowEntriesRepository.getEntry(entryId, tenantId).getOrNull()
            ?: return Result.failure(IllegalArgumentException("Cashflow entry not found: $entryId"))

        val newRemaining = entry.remainingAmount - paymentAmount
        val newStatus = when {
            newRemaining.minor <= 0 -> CashflowEntryStatus.Paid
            else -> entry.status // Keep current status (Open/Overdue)
        }

        return cashflowEntriesRepository.updateRemainingAmountAndStatus(
            entryId = entryId,
            tenantId = tenantId,
            newRemainingAmount = if (newRemaining.isNegative) Money.ZERO else newRemaining,
            newStatus = newStatus
        )
            .onSuccess { logger.info("Payment recorded for cashflow entry: $entryId, new status: $newStatus") }
            .onFailure { logger.error("Failed to record payment for cashflow entry: $entryId", it) }
    }

    /**
     * Mark entry as overdue.
     */
    suspend fun markOverdue(
        entryId: CashflowEntryId,
        tenantId: TenantId
    ): Result<Boolean> {
        logger.info("Marking cashflow entry as overdue: $entryId")
        return cashflowEntriesRepository.updateStatus(entryId, tenantId, CashflowEntryStatus.Overdue)
            .onSuccess { logger.info("Cashflow entry marked as overdue: $entryId") }
            .onFailure { logger.error("Failed to mark cashflow entry as overdue: $entryId", it) }
    }

    /**
     * Mark entry as cancelled.
     */
    suspend fun cancel(
        entryId: CashflowEntryId,
        tenantId: TenantId
    ): Result<Boolean> {
        logger.info("Cancelling cashflow entry: $entryId")
        return cashflowEntriesRepository.updateStatus(entryId, tenantId, CashflowEntryStatus.Cancelled)
            .onSuccess { logger.info("Cashflow entry cancelled: $entryId") }
            .onFailure { logger.error("Failed to cancel cashflow entry: $entryId", it) }
    }
}
