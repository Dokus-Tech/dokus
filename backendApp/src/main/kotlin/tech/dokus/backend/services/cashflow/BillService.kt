package tech.dokus.backend.services.cashflow

import kotlinx.datetime.LocalDate
import tech.dokus.database.repository.cashflow.BillRepository
import tech.dokus.database.repository.cashflow.BillStatistics
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.BillStatus
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.ids.BillId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreateBillRequest
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.MarkBillPaidRequest
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.peppol.model.PeppolReceivedDocument

/**
 * Service for bill business operations.
 *
 * Bills represent incoming supplier invoices (Cash-Out).
 * This service handles all business logic related to bills
 * and delegates data access to the repository layer.
 */
class BillService(
    private val billRepository: BillRepository
) {
    private val logger = loggerFor()

    /**
     * Create a new bill for a tenant.
     */
    suspend fun createBill(
        tenantId: TenantId,
        request: CreateBillRequest
    ): Result<FinancialDocumentDto.BillDto> {
        logger.info("Creating bill for tenant: $tenantId, supplier: ${request.supplierName}")
        return billRepository.createBill(tenantId, request)
            .onSuccess { logger.info("Bill created: ${it.id}") }
            .onFailure { logger.error("Failed to create bill for tenant: $tenantId", it) }
    }

    /**
     * Get a bill by ID.
     */
    suspend fun getBill(
        billId: BillId,
        tenantId: TenantId
    ): Result<FinancialDocumentDto.BillDto?> {
        logger.debug("Fetching bill: {} for tenant: {}", billId, tenantId)
        return billRepository.getBill(billId, tenantId)
            .onFailure { logger.error("Failed to fetch bill: $billId", it) }
    }

    /**
     * List bills with optional filters.
     */
    suspend fun listBills(
        tenantId: TenantId,
        status: BillStatus? = null,
        category: ExpenseCategory? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<FinancialDocumentDto.BillDto>> {
        logger.debug(
            "Listing bills for tenant: {} (status={}, category={}, limit={}, offset={})",
            tenantId,
            status,
            category,
            limit,
            offset
        )
        return billRepository.listBills(tenantId, status, category, fromDate, toDate, limit, offset)
            .onSuccess { logger.debug("Retrieved ${it.items.size} bills (total=${it.total})") }
            .onFailure { logger.error("Failed to list bills for tenant: $tenantId", it) }
    }

    /**
     * List overdue bills for a tenant.
     */
    suspend fun listOverdueBills(tenantId: TenantId): Result<List<FinancialDocumentDto.BillDto>> {
        logger.debug("Listing overdue bills for tenant: {}", tenantId)
        return billRepository.listOverdueBills(tenantId)
            .onSuccess { logger.debug("Retrieved ${it.size} overdue bills") }
            .onFailure { logger.error("Failed to list overdue bills for tenant: $tenantId", it) }
    }

    /**
     * Update bill details.
     */
    suspend fun updateBill(
        billId: BillId,
        tenantId: TenantId,
        request: CreateBillRequest
    ): Result<FinancialDocumentDto.BillDto> {
        logger.info("Updating bill: $billId for tenant: $tenantId")
        return billRepository.updateBill(billId, tenantId, request)
            .onSuccess { logger.info("Bill updated: $billId") }
            .onFailure { logger.error("Failed to update bill: $billId", it) }
    }

    /**
     * Update bill status.
     */
    suspend fun updateBillStatus(
        billId: BillId,
        tenantId: TenantId,
        status: BillStatus
    ): Result<Boolean> {
        logger.info("Updating bill status: $billId -> $status")
        return billRepository.updateBillStatus(billId, tenantId, status)
            .onSuccess { logger.info("Bill status updated: $billId -> $status") }
            .onFailure { logger.error("Failed to update bill status: $billId", it) }
    }

    /**
     * Mark a bill as paid.
     */
    suspend fun markBillPaid(
        billId: BillId,
        tenantId: TenantId,
        request: MarkBillPaidRequest
    ): Result<FinancialDocumentDto.BillDto> {
        logger.info("Marking bill as paid: $billId")
        return billRepository.markBillPaid(billId, tenantId, request)
            .onSuccess { logger.info("Bill marked as paid: $billId") }
            .onFailure { logger.error("Failed to mark bill as paid: $billId", it) }
    }

    /**
     * Delete a bill.
     */
    suspend fun deleteBill(
        billId: BillId,
        tenantId: TenantId
    ): Result<Boolean> {
        logger.info("Deleting bill: $billId")
        return billRepository.deleteBill(billId, tenantId)
            .onSuccess { logger.info("Bill deleted: $billId") }
            .onFailure { logger.error("Failed to delete bill: $billId", it) }
    }

    /**
     * Check if a bill exists.
     */
    suspend fun exists(
        billId: BillId,
        tenantId: TenantId
    ): Result<Boolean> {
        return billRepository.exists(billId, tenantId)
    }

    /**
     * Get bill statistics for cashflow calculations.
     */
    suspend fun getBillStatistics(
        tenantId: TenantId,
        fromDate: LocalDate,
        toDate: LocalDate
    ): Result<BillStatistics> {
        logger.debug(
            "Getting bill statistics for tenant: {} (from={}, to={})",
            tenantId,
            fromDate,
            toDate
        )
        return billRepository.getBillStatistics(tenantId, fromDate, toDate)
            .onFailure { logger.error("Failed to get bill statistics for tenant: $tenantId", it) }
    }

    /**
     * Create a bill from a received Peppol document.
     *
     * This method converts a Peppol document received via inbox polling
     * into a Bill record in the system.
     */
    suspend fun createBillFromPeppol(
        tenantId: TenantId,
        peppolDocument: PeppolReceivedDocument
    ): Result<FinancialDocumentDto.BillDto> {
        logger.info("Creating bill from Peppol document: ${peppolDocument.id} for tenant: $tenantId")

        // Parse dates from string format
        val issueDate = peppolDocument.issueDate?.let { parseDate(it) }
            ?: LocalDate.fromEpochDays(0) // Fallback if no issue date
        val dueDate = peppolDocument.dueDate?.let { parseDate(it) }
            ?: issueDate // Default due date to issue date if not specified

        // Extract amounts
        val totalAmount = peppolDocument.totals?.payableAmount
            ?: peppolDocument.totals?.taxInclusiveAmount
            ?: peppolDocument.lineItems?.sumOf { it.lineTotal ?: 0.0 }
            ?: 0.0

        val taxAmount = peppolDocument.taxTotal?.taxAmount

        // Determine VAT rate from tax breakdown
        val vatRate = peppolDocument.taxTotal?.taxSubtotals?.firstOrNull()?.taxPercent

        // Build create request
        val request = CreateBillRequest(
            supplierName = peppolDocument.seller?.name ?: "Unknown Supplier",
            supplierVatNumber = peppolDocument.seller?.vatNumber,
            invoiceNumber = peppolDocument.invoiceNumber ?: peppolDocument.id,
            issueDate = issueDate,
            dueDate = dueDate,
            amount = Money.fromDouble(totalAmount),
            vatAmount = taxAmount?.let { Money.fromDouble(it) },
            vatRate = vatRate?.let { VatRate.fromMultiplier(it.toDouble() / 100.0) },
            category = categorizeSupplier(peppolDocument.seller?.name),
            description = buildDescription(peppolDocument),
            notes = buildNotes(peppolDocument)
        )

        return billRepository.createBill(tenantId, request)
            .onSuccess { logger.info("Bill created from Peppol document: ${it.id}") }
            .onFailure {
                logger.error(
                    "Failed to create bill from Peppol document: ${peppolDocument.id}",
                    it
                )
            }
    }

    /**
     * Parse date string to LocalDate.
     * Supports ISO format (YYYY-MM-DD) and common variants.
     */
    private fun parseDate(dateString: String): LocalDate? {
        return try {
            // Try ISO format first
            LocalDate.parse(dateString.take(10))
        } catch (e: Exception) {
            logger.warn("Failed to parse date: $dateString", e)
            null
        }
    }

    /**
     * Categorize a bill based on supplier name.
     * Simple heuristic - can be improved with ML or user preferences.
     */
    private fun categorizeSupplier(supplierName: String?): ExpenseCategory {
        if (supplierName == null) return ExpenseCategory.Other

        val lowerName = supplierName.lowercase()
        return when {
            lowerName.contains("telecom") || lowerName.contains("mobile") ||
                lowerName.contains("proximus") || lowerName.contains("orange") ||
                lowerName.contains("telenet") -> ExpenseCategory.Telecommunications

            lowerName.contains("electric") || lowerName.contains("gas") ||
                lowerName.contains("water") || lowerName.contains("engie") ||
                lowerName.contains("luminus") -> ExpenseCategory.Utilities

            lowerName.contains("software") || lowerName.contains("cloud") ||
                lowerName.contains("microsoft") || lowerName.contains("google") ||
                lowerName.contains("aws") || lowerName.contains("adobe") -> ExpenseCategory.Software

            lowerName.contains("insurance") || lowerName.contains("axa") ||
                lowerName.contains("ethias") -> ExpenseCategory.Insurance

            lowerName.contains("office") || lowerName.contains("staples") ||
                lowerName.contains("bol.com") -> ExpenseCategory.OfficeSupplies

            lowerName.contains("accountant") || lowerName.contains("lawyer") ||
                lowerName.contains("consultant") || lowerName.contains("fiduciary") -> ExpenseCategory.ProfessionalServices

            lowerName.contains("rent") || lowerName.contains("lease") ||
                lowerName.contains("immobili") -> ExpenseCategory.Rent

            lowerName.contains("fuel") || lowerName.contains("garage") ||
                lowerName.contains("toyota") || lowerName.contains("volkswagen") -> ExpenseCategory.Vehicle

            lowerName.contains("hotel") || lowerName.contains("airline") ||
                lowerName.contains("train") || lowerName.contains("sncb") ||
                lowerName.contains("nmbs") -> ExpenseCategory.Travel

            else -> ExpenseCategory.Other
        }
    }

    /**
     * Build description from Peppol document.
     */
    private fun buildDescription(doc: PeppolReceivedDocument): String {
        val parts = mutableListOf<String>()

        // Add document type
        parts.add("Peppol ${doc.documentType}")

        // Add line item summary if available
        doc.lineItems?.let { items ->
            if (items.isNotEmpty()) {
                val itemCount = items.size
                parts.add("($itemCount line items)")
            }
        }

        return parts.joinToString(" ")
    }

    /**
     * Build notes from Peppol document metadata.
     */
    private fun buildNotes(doc: PeppolReceivedDocument): String {
        val parts = mutableListOf<String>()

        // Add Peppol document ID
        parts.add("Peppol Document ID: ${doc.id}")

        // Add sender Peppol ID
        parts.add("Sender Peppol ID: ${doc.senderPeppolId}")

        // Add note if present
        doc.note?.let { parts.add("Note: $it") }

        // Add seller details
        doc.seller?.let { seller ->
            seller.companyNumber?.let { parts.add("Company Number: $it") }
            seller.contactEmail?.let { parts.add("Contact Email: $it") }
        }

        return parts.joinToString("\n")
    }
}
