package tech.dokus.backend.services.cashflow
import kotlin.uuid.Uuid

import kotlinx.datetime.LocalDate
import tech.dokus.database.repository.cashflow.CreditNoteRepository
import tech.dokus.database.repository.cashflow.RefundClaimRepository
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CreditNoteStatus
import tech.dokus.domain.enums.CreditNoteType
import tech.dokus.domain.enums.SettlementIntent
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.CreditNoteId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreateCreditNoteRequest
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.RecordRefundRequest
import tech.dokus.domain.model.RefundClaimDto
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Service for credit note business operations.
 *
 * Credit notes represent reductions in receivables (Sales) or payables (Purchase).
 * They do NOT directly create cashflow entries.
 * Cashflow entries are created only when refunds are recorded.
 *
 * Workflow:
 * 1. Create credit note (Draft status)
 * 2. Confirm credit note → NO cashflow, optionally creates RefundClaim if intent=RefundExpected
 * 3. Record refund → Creates cashflow entry + settles the claim
 */
class CreditNoteService(
    private val creditNoteRepository: CreditNoteRepository,
    private val refundClaimRepository: RefundClaimRepository,
    private val cashflowEntriesService: CashflowEntriesService
) {
    private val logger = loggerFor()

    /**
     * Create a new credit note.
     */
    suspend fun createCreditNote(
        tenantId: TenantId,
        request: CreateCreditNoteRequest
    ): Result<FinancialDocumentDto.CreditNoteDto> {
        logger.info("Creating credit note: type=${request.creditNoteType}, tenant=$tenantId")
        return creditNoteRepository.createCreditNote(tenantId, request)
            .onSuccess { logger.info("Credit note created: ${it.id}") }
            .onFailure { logger.error("Failed to create credit note", it) }
    }

    /**
     * Get a credit note by ID.
     */
    suspend fun getCreditNote(
        creditNoteId: CreditNoteId,
        tenantId: TenantId
    ): Result<FinancialDocumentDto.CreditNoteDto?> {
        return creditNoteRepository.getCreditNote(creditNoteId, tenantId)
    }

    /**
     * Update credit note details.
     *
     * Status transitions are handled separately (confirm/refund/cancel flows).
     */
    suspend fun updateCreditNote(
        creditNoteId: CreditNoteId,
        tenantId: TenantId,
        request: CreateCreditNoteRequest
    ): Result<FinancialDocumentDto.CreditNoteDto> {
        logger.info("Updating credit note: $creditNoteId, tenant=$tenantId")
        return creditNoteRepository.updateCreditNote(creditNoteId, tenantId, request)
            .onSuccess { logger.info("Credit note updated: $creditNoteId") }
            .onFailure { logger.error("Failed to update credit note: $creditNoteId", it) }
    }

    /**
     * List credit notes with optional filters.
     */
    @Suppress("LongParameterList")
    suspend fun listCreditNotes(
        tenantId: TenantId,
        status: CreditNoteStatus? = null,
        creditNoteType: CreditNoteType? = null,
        contactId: ContactId? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Int = 0,
    ): Result<PaginatedResponse<FinancialDocumentDto.CreditNoteDto>> {
        return creditNoteRepository.listCreditNotes(
            tenantId = tenantId,
            status = status,
            creditNoteType = creditNoteType,
            contactId = contactId,
            fromDate = fromDate,
            toDate = toDate,
            limit = limit,
            offset = offset,
        )
    }

    /**
     * Confirm a credit note.
     *
     * This marks the credit note as confirmed but does NOT create any cashflow entry.
     * If settlementIntent is RefundExpected, creates a RefundClaim to track the expected refund.
     */
    suspend fun confirmCreditNote(
        creditNoteId: CreditNoteId,
        tenantId: TenantId
    ): Result<FinancialDocumentDto.CreditNoteDto> = runCatching {
        logger.info("Confirming credit note: $creditNoteId, tenant=$tenantId")

        // Get the credit note
        val creditNote = creditNoteRepository.getCreditNote(creditNoteId, tenantId).getOrThrow()
            ?: error("Credit note not found: $creditNoteId")

        // Validate status
        if (creditNote.status != CreditNoteStatus.Draft) {
            error("Credit note is not in Draft status: ${creditNote.status}")
        }

        // Update status to Confirmed
        creditNoteRepository.updateStatus(creditNoteId, tenantId, CreditNoteStatus.Confirmed).getOrThrow()

        // If settlement intent is RefundExpected, create a RefundClaim
        if (creditNote.settlementIntent == SettlementIntent.RefundExpected) {
            logger.info("Creating refund claim for credit note: $creditNoteId")
            refundClaimRepository.createRefundClaim(
                tenantId = tenantId,
                creditNoteId = creditNoteId,
                counterpartyId = creditNote.contactId,
                amount = creditNote.totalAmount,
                currency = creditNote.currency,
                expectedDate = null // Can be set later if needed
            ).getOrThrow()
        }

        // Return updated credit note
        creditNoteRepository.getCreditNote(creditNoteId, tenantId).getOrThrow()
            ?: error("Credit note not found after confirmation: $creditNoteId")
    }.onFailure { logger.error("Failed to confirm credit note: $creditNoteId", it) }

    /**
     * Record a refund payment for a credit note.
     *
     * This creates a cashflow entry:
     * - Sales credit note refund → Cash-Out (paying money to customer)
     * - Purchase credit note refund → Cash-In (receiving money from supplier)
     *
     * Also settles any open refund claim.
     */
    suspend fun recordRefund(
        creditNoteId: CreditNoteId,
        tenantId: TenantId,
        request: RecordRefundRequest
    ): Result<FinancialDocumentDto.CreditNoteDto> = runCatching {
        logger.info("Recording refund for credit note: $creditNoteId, amount=${request.amount}")

        // Get the credit note
        val creditNote = creditNoteRepository.getCreditNote(creditNoteId, tenantId).getOrThrow()
            ?: error("Credit note not found: $creditNoteId")

        // Validate status (must be Confirmed)
        if (creditNote.status != CreditNoteStatus.Confirmed) {
            error("Credit note must be confirmed before recording refund: ${creditNote.status}")
        }

        // Determine cashflow direction based on credit note type
        val direction = when (creditNote.creditNoteType) {
            CreditNoteType.Sales -> CashflowDirection.Out // Paying customer
            CreditNoteType.Purchase -> CashflowDirection.In // Receiving from supplier
        }

        // Create cashflow entry
        val cashflowEntry = cashflowEntriesService.createFromRefund(
            tenantId = tenantId,
            creditNoteId = creditNoteId.value,
            documentId = creditNote.documentId,
            refundDate = request.refundDate,
            amountGross = request.amount,
            amountVat = Money.ZERO, // VAT already accounted for in credit note
            direction = direction,
            contactId = creditNote.contactId
        ).getOrThrow()

        logger.info("Created cashflow entry for refund: ${cashflowEntry.id}")

        // Settle any open refund claim
        val openClaim = refundClaimRepository.getOpenClaimForCreditNote(tenantId, creditNoteId)
        if (openClaim != null) {
            refundClaimRepository.settleRefundClaim(
                claimId = openClaim.id,
                tenantId = tenantId,
                cashflowEntryId = cashflowEntry.id
            ).getOrThrow()
            logger.info("Settled refund claim: ${openClaim.id}")
        }

        // Update credit note status to Settled
        creditNoteRepository.updateStatus(creditNoteId, tenantId, CreditNoteStatus.Settled).getOrThrow()

        // Return updated credit note
        creditNoteRepository.getCreditNote(creditNoteId, tenantId).getOrThrow()
            ?: error("Credit note not found after refund: $creditNoteId")
    }.onFailure { logger.error("Failed to record refund for credit note: $creditNoteId", it) }

    /**
     * Cancel a credit note.
     */
    suspend fun cancelCreditNote(
        creditNoteId: CreditNoteId,
        tenantId: TenantId
    ): Result<Boolean> {
        logger.info("Cancelling credit note: $creditNoteId")

        // Cancel any open refund claim
        val openClaim = refundClaimRepository.getOpenClaimForCreditNote(tenantId, creditNoteId)
        if (openClaim != null) {
            refundClaimRepository.cancelRefundClaim(openClaim.id, tenantId)
        }

        return creditNoteRepository.updateStatus(creditNoteId, tenantId, CreditNoteStatus.Cancelled)
            .onSuccess { logger.info("Credit note cancelled: $creditNoteId") }
            .onFailure { logger.error("Failed to cancel credit note: $creditNoteId", it) }
    }

    /**
     * Get open refund claims for a tenant.
     */
    suspend fun getOpenRefundClaims(tenantId: TenantId): List<RefundClaimDto> {
        return refundClaimRepository.listOpenClaims(tenantId)
    }

    /**
     * Update settlement intent for a credit note.
     */
    suspend fun updateSettlementIntent(
        creditNoteId: CreditNoteId,
        tenantId: TenantId,
        intent: SettlementIntent
    ): Result<Boolean> {
        logger.info("Updating settlement intent for credit note: $creditNoteId -> $intent")
        return creditNoteRepository.updateSettlementIntent(creditNoteId, tenantId, intent)
    }

    /**
     * Find credit note by document ID.
     */
    suspend fun findByDocumentId(
        tenantId: TenantId,
        documentId: DocumentId
    ): FinancialDocumentDto.CreditNoteDto? {
        return creditNoteRepository.findByDocumentId(tenantId, documentId)
    }
}
