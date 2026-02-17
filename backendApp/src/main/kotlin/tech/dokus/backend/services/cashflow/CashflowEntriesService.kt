package tech.dokus.backend.services.cashflow

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tech.dokus.backend.util.isUniqueViolation
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.CashflowViewMode
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.foundation.backend.utils.loggerFor
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Service for cashflow entry business operations.
 *
 * Cashflow entries are projections of financial facts (Invoice, Expense).
 * They are created during document confirmation and updated when payments are recorded.
 *
 * This is the normalized source of truth for cashflow data.
 */
@Suppress("LongParameterList")
class CashflowEntriesService(
    private val cashflowEntriesRepository: CashflowEntriesRepository
) {
    private val logger = loggerFor()

    private suspend fun getBySourceOrNull(
        tenantId: TenantId,
        sourceType: CashflowSourceType,
        sourceId: UUID
    ): CashflowEntry? = cashflowEntriesRepository.getBySource(tenantId, sourceType, sourceId).getOrThrow()

    /**
     * Create a cashflow entry for an invoice.
     */
    suspend fun createFromInvoice(
        tenantId: TenantId,
        invoiceId: UUID,
        documentId: DocumentId?,
        dueDate: LocalDate,
        amountGross: Money,
        amountVat: Money,
        direction: DocumentDirection,
        contactId: ContactId?
    ): Result<CashflowEntry> {
        logger.info("Creating cashflow entry for invoice: $invoiceId, tenant: $tenantId")
        return runCatching {
            val existing = getBySourceOrNull(tenantId, CashflowSourceType.Invoice, invoiceId)
            if (existing != null) return@runCatching existing

            try {
                cashflowEntriesRepository.createEntry(
                    tenantId = tenantId,
                    sourceType = CashflowSourceType.Invoice,
                    sourceId = invoiceId,
                    documentId = documentId,
                    direction = if (direction == DocumentDirection.Inbound) CashflowDirection.Out else CashflowDirection.In,
                    eventDate = dueDate,
                    amountGross = amountGross,
                    amountVat = amountVat,
                    contactId = contactId
                ).getOrThrow()
            } catch (t: Throwable) {
                if (t.isUniqueViolation()) {
                    getBySourceOrNull(tenantId, CashflowSourceType.Invoice, invoiceId) ?: throw t
                } else {
                    throw t
                }
            }
        }
            .onSuccess { logger.info("Cashflow entry ready: ${it.id} for invoice: $invoiceId") }
            .onFailure { logger.error("Failed to ensure cashflow entry for invoice: $invoiceId", it) }
    }

    /**
     * Update an invoice cashflow projection from the latest draft.
     *
     * Safety rules (MVP):
     * - Only allowed when entry is OPEN and unpaid (remaining == gross, paidAt == null).
     * - Creates the entry if missing (invariant repair).
     */
    suspend fun updateFromInvoice(
        tenantId: TenantId,
        invoiceId: UUID,
        documentId: DocumentId?,
        dueDate: LocalDate,
        amountGross: Money,
        amountVat: Money,
        direction: DocumentDirection,
        contactId: ContactId?
    ): Result<CashflowEntry> = runCatching {
        val existing = getBySourceOrNull(tenantId, CashflowSourceType.Invoice, invoiceId)
            ?: return@runCatching createFromInvoice(
                tenantId = tenantId,
                invoiceId = invoiceId,
                documentId = documentId,
                dueDate = dueDate,
                amountGross = amountGross,
                amountVat = amountVat,
                direction = direction,
                contactId = contactId
            ).getOrThrow()

        val updated = cashflowEntriesRepository.updateProjectionBySource(
            tenantId = tenantId,
            sourceType = CashflowSourceType.Invoice,
            sourceId = invoiceId,
            documentId = documentId,
            direction = if (direction == DocumentDirection.Inbound) CashflowDirection.Out else CashflowDirection.In,
            eventDate = dueDate,
            amountGross = amountGross,
            amountVat = amountVat,
            contactId = contactId
        ).getOrThrow()

        if (!updated) {
            throw DokusException.BadRequest("Cashflow entry cannot be updated: it has payments or is no longer Open")
        }

        getBySourceOrNull(tenantId, CashflowSourceType.Invoice, invoiceId)
            ?: error("Cashflow entry not found after update: invoiceId=$invoiceId")
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
        contactId: ContactId?
    ): Result<CashflowEntry> {
        logger.info("Creating cashflow entry for expense: $expenseId, tenant: $tenantId")
        return runCatching {
            val existing = getBySourceOrNull(tenantId, CashflowSourceType.Expense, expenseId)
            if (existing != null) return@runCatching existing

            try {
                cashflowEntriesRepository.createEntry(
                    tenantId = tenantId,
                    sourceType = CashflowSourceType.Expense,
                    sourceId = expenseId,
                    documentId = documentId,
                    direction = CashflowDirection.Out,
                    eventDate = expenseDate,
                    amountGross = amountGross,
                    amountVat = amountVat,
                    contactId = contactId
                ).getOrThrow()
            } catch (t: Throwable) {
                if (t.isUniqueViolation()) {
                    getBySourceOrNull(tenantId, CashflowSourceType.Expense, expenseId) ?: throw t
                } else {
                    throw t
                }
            }
        }
            .onSuccess { logger.info("Cashflow entry ready: ${it.id} for expense: $expenseId") }
            .onFailure { logger.error("Failed to ensure cashflow entry for expense: $expenseId", it) }
    }

    /**
     * Update an expense cashflow projection from the latest draft.
     *
     * Safety rules (MVP):
     * - Only allowed when entry is OPEN and unpaid (remaining == gross, paidAt == null).
     * - Creates the entry if missing (invariant repair).
     */
    suspend fun updateFromExpense(
        tenantId: TenantId,
        expenseId: UUID,
        documentId: DocumentId?,
        expenseDate: LocalDate,
        amountGross: Money,
        amountVat: Money,
        contactId: ContactId?
    ): Result<CashflowEntry> = runCatching {
        val existing = getBySourceOrNull(tenantId, CashflowSourceType.Expense, expenseId)
            ?: return@runCatching createFromExpense(
                tenantId = tenantId,
                expenseId = expenseId,
                documentId = documentId,
                expenseDate = expenseDate,
                amountGross = amountGross,
                amountVat = amountVat,
                contactId = contactId
            ).getOrThrow()

        val updated = cashflowEntriesRepository.updateProjectionBySource(
            tenantId = tenantId,
            sourceType = CashflowSourceType.Expense,
            sourceId = expenseId,
            documentId = documentId,
            direction = CashflowDirection.Out,
            eventDate = expenseDate,
            amountGross = amountGross,
            amountVat = amountVat,
            contactId = contactId
        ).getOrThrow()

        if (!updated) {
            throw DokusException.BadRequest("Cashflow entry cannot be updated: it has payments or is no longer Open")
        }

        getBySourceOrNull(tenantId, CashflowSourceType.Expense, expenseId)
            ?: error("Cashflow entry not found after update: expenseId=$expenseId")
    }

    /**
     * Create a cashflow entry for a refund payment (from credit note).
     *
     * Direction depends on credit note type:
     * - Sales credit note refund → Cash-Out (paying customer)
     * - Purchase credit note refund → Cash-In (receiving from supplier)
     */
    suspend fun createFromRefund(
        tenantId: TenantId,
        creditNoteId: UUID,
        documentId: DocumentId?,
        refundDate: LocalDate,
        amountGross: Money,
        amountVat: Money,
        direction: CashflowDirection,
        contactId: ContactId?
    ): Result<CashflowEntry> {
        logger.info("Creating cashflow entry for refund: creditNote=$creditNoteId, direction=$direction")
        return runCatching {
            val existing = getBySourceOrNull(tenantId, CashflowSourceType.Refund, creditNoteId)
            if (existing != null) return@runCatching existing

            try {
                cashflowEntriesRepository.createEntry(
                    tenantId = tenantId,
                    sourceType = CashflowSourceType.Refund,
                    sourceId = creditNoteId,
                    documentId = documentId,
                    direction = direction,
                    eventDate = refundDate,
                    amountGross = amountGross,
                    amountVat = amountVat,
                    contactId = contactId
                ).getOrThrow()
            } catch (t: Throwable) {
                if (t.isUniqueViolation()) {
                    getBySourceOrNull(tenantId, CashflowSourceType.Refund, creditNoteId) ?: throw t
                } else {
                    throw t
                }
            }
        }
            .onSuccess { logger.info("Cashflow entry ready: ${it.id} for refund: $creditNoteId") }
            .onFailure { logger.error("Failed to ensure cashflow entry for refund: $creditNoteId", it) }
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
     * Get cashflow entry by source (Invoice/Expense ID).
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
     *
     * @param viewMode Determines date field filtering:
     *                 - Upcoming: filter by eventDate, sort ASC
     *                 - History: filter by paidAt, sort DESC
     * @param statuses Multi-status filter (e.g., [Open, Overdue])
     */
    suspend fun listEntries(
        tenantId: TenantId,
        viewMode: CashflowViewMode? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        direction: CashflowDirection? = null,
        statuses: List<CashflowEntryStatus>? = null
    ): Result<List<CashflowEntry>> {
        logger.debug(
            "Listing cashflow entries for tenant: {} (viewMode={}, from={}, to={}, direction={}, statuses={})",
            tenantId,
            viewMode,
            fromDate,
            toDate,
            direction,
            statuses
        )
        return cashflowEntriesRepository.listEntries(tenantId, viewMode, fromDate, toDate, direction, statuses)
            .onSuccess { logger.debug("Retrieved ${it.size} cashflow entries") }
            .onFailure { logger.error("Failed to list cashflow entries for tenant: $tenantId", it) }
    }

    /**
     * Record a payment against a cashflow entry.
     * Updates the remaining amount and status accordingly.
     *
     * INVARIANT: When transitioning to PAID, paidAt is set to current UTC time.
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
        val isNowFullyPaid = newRemaining.minor <= 0
        val newStatus = when {
            isNowFullyPaid -> CashflowEntryStatus.Paid
            else -> entry.status // Keep current status (Open/Overdue)
        }

        // Set paidAt ONLY when transitioning to fully paid
        val paidAt = if (isNowFullyPaid && entry.status != CashflowEntryStatus.Paid) {
            Clock.System.now().toLocalDateTime(TimeZone.UTC)
        } else {
            entry.paidAt
        }

        val normalizedRemaining = if (newRemaining.isNegative) Money.ZERO else newRemaining
        return cashflowEntriesRepository.updateRemainingAmountAndStatus(
            entryId = entryId,
            tenantId = tenantId,
            newRemainingAmount = normalizedRemaining,
            newStatus = newStatus,
            paidAt = paidAt
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
