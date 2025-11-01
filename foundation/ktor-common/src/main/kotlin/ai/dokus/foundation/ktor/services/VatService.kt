package ai.dokus.foundation.ktor.services

import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.VatReturnId
import ai.dokus.foundation.domain.enums.VatReturnStatus
import ai.dokus.foundation.domain.model.QuarterInfo
import ai.dokus.foundation.domain.model.VatCalculation
import ai.dokus.foundation.domain.model.VatReturn
import kotlinx.datetime.Instant
import kotlinx.rpc.annotations.Rpc
import kotlin.time.ExperimentalTime

@Rpc
interface VatService {
    /**
     * Calculates VAT for a quarter
     * Computes sales VAT (from invoices) and purchase VAT (from expenses)
     *
     * @param tenantId The tenant's unique identifier
     * @param year The year
     * @param quarter The quarter (1, 2, 3, 4)
     * @return VAT calculation with sales VAT, purchase VAT, and net VAT
     */
    suspend fun calculateVat(tenantId: TenantId, year: Int, quarter: Int): VatCalculation

    /**
     * Creates a VAT return for a quarter
     * Automatically calculates VAT from invoices and expenses
     *
     * @param tenantId The tenant's unique identifier
     * @param year The year
     * @param quarter The quarter (1, 2, 3, 4)
     * @return The created VAT return
     * @throws IllegalArgumentException if VAT return already exists for this period
     */
    suspend fun createReturn(tenantId: TenantId, year: Int, quarter: Int): VatReturn

    /**
     * Updates a VAT return
     * Can only update draft returns
     *
     * @param returnId The VAT return's unique identifier
     * @param salesVat The sales VAT amount (optional)
     * @param purchaseVat The purchase VAT amount (optional)
     * @throws IllegalArgumentException if return not found or not in draft status
     */
    suspend fun updateReturn(
        returnId: VatReturnId,
        salesVat: Money? = null,
        purchaseVat: Money? = null
    )

    /**
     * Submits a VAT return to tax authorities
     * Marks the return as submitted and records the filing timestamp
     *
     * @param returnId The VAT return's unique identifier
     * @throws IllegalArgumentException if return not found or already submitted
     */
    suspend fun submitReturn(returnId: VatReturnId)

    /**
     * Marks a VAT return as paid
     * Records when the VAT payment was made to tax authorities
     *
     * @param returnId The VAT return's unique identifier
     * @param paidAt The payment timestamp
     * @throws IllegalArgumentException if return not found or not submitted
     */
    @OptIn(ExperimentalTime::class)
    suspend fun markAsPaid(returnId: VatReturnId, paidAt: Instant)

    /**
     * Finds a VAT return by year and quarter
     *
     * @param tenantId The tenant's unique identifier
     * @param year The year
     * @param quarter The quarter (1, 2, 3, 4)
     * @return The VAT return if found, null otherwise
     */
    suspend fun findByQuarter(tenantId: TenantId, year: Int, quarter: Int): VatReturn?

    /**
     * Finds a VAT return by its unique ID
     *
     * @param id The VAT return's unique identifier
     * @return The VAT return if found, null otherwise
     */
    suspend fun findById(id: VatReturnId): VatReturn?

    /**
     * Lists all VAT returns for a tenant
     *
     * @param tenantId The tenant's unique identifier
     * @param year Filter by year (optional)
     * @param status Filter by status (optional)
     * @return List of VAT returns
     */
    suspend fun listReturns(
        tenantId: TenantId,
        year: Int? = null,
        status: VatReturnStatus? = null
    ): List<VatReturn>

    /**
     * Lists unpaid VAT returns for a tenant
     *
     * @param tenantId The tenant's unique identifier
     * @return List of submitted but unpaid VAT returns
     */
    suspend fun listUnpaid(tenantId: TenantId): List<VatReturn>

    /**
     * Deletes a VAT return
     * Can only delete draft returns
     *
     * @param returnId The VAT return's unique identifier
     * @throws IllegalArgumentException if return not found or not in draft status
     */
    suspend fun delete(returnId: VatReturnId)

    /**
     * Gets the current quarter based on current date
     *
     * @return Current quarter information with year and quarter
     */
    suspend fun getCurrentQuarter(): QuarterInfo

    /**
     * Gets the previous quarter
     *
     * @param year The current year
     * @param quarter The current quarter
     * @return Previous quarter information with year and quarter
     */
    suspend fun getPreviousQuarter(year: Int, quarter: Int): QuarterInfo

    /**
     * Gets the next quarter
     *
     * @param year The current year
     * @param quarter The current quarter
     * @return Next quarter information with year and quarter
     */
    suspend fun getNextQuarter(year: Int, quarter: Int): QuarterInfo

    /**
     * Validates a quarter number
     *
     * @param quarter The quarter to validate
     * @return True if valid (1-4), false otherwise
     */
    suspend fun isValidQuarter(quarter: Int): Boolean

    /**
     * Gets detailed VAT breakdown for a quarter
     * Includes itemized invoice and expense VAT
     *
     * @param tenantId The tenant's unique identifier
     * @param year The year
     * @param quarter The quarter (1, 2, 3, 4)
     * @return Map with detailed breakdown (invoices, expenses, totals)
     */
    suspend fun getVatBreakdown(
        tenantId: TenantId,
        year: Int,
        quarter: Int
    ): Map<String, Any>
}
