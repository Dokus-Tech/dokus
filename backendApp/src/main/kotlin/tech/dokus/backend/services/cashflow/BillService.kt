package tech.dokus.backend.services.cashflow

import kotlinx.datetime.LocalDate
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.cashflow.BillRepository
import tech.dokus.database.repository.cashflow.BillStatistics
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BillStatus
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.ids.BillId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreateBillRequest
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.MarkBillPaidRequest
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.foundation.backend.utils.loggerFor
import java.util.UUID

/**
 * Service for bill business operations.
 *
 * Bills represent incoming supplier invoices (Cash-Out).
 * This service handles all business logic related to bills
 * and delegates data access to the repository layer.
 */
class BillService(
    private val billRepository: BillRepository,
    private val cashflowEntriesRepository: CashflowEntriesRepository
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
        return runCatching {
            val bill = billRepository.markBillPaid(billId, tenantId, request).getOrThrow()

            val entry = cashflowEntriesRepository.getBySource(
                tenantId = tenantId,
                sourceType = CashflowSourceType.Bill,
                sourceId = UUID.fromString(billId.toString())
            ).getOrNull()

            if (entry != null) {
                cashflowEntriesRepository.updateRemainingAmountAndStatus(
                    entryId = entry.id,
                    tenantId = tenantId,
                    newRemainingAmount = Money.ZERO,
                    newStatus = CashflowEntryStatus.Paid
                ).getOrThrow()
            }

            bill
        }
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
}
