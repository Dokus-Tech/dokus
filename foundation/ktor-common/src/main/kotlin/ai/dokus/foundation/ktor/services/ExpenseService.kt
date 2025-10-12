package ai.dokus.foundation.ktor.services

import ai.dokus.foundation.domain.ExpenseId
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.model.CreateExpenseRequest
import ai.dokus.foundation.domain.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.rpc.RPC

@RPC
interface ExpenseService {
    /**
     * Creates a new expense
     *
     * @param request The expense creation request with all details
     * @return The created expense
     * @throws IllegalArgumentException if validation fails
     */
    suspend fun create(request: CreateExpenseRequest): Expense

    /**
     * Updates an existing expense
     *
     * @param expenseId The expense's unique identifier
     * @param date The expense date (optional)
     * @param merchant The merchant name (optional)
     * @param amount The expense amount (optional)
     * @param vatAmount The VAT amount (optional)
     * @param vatRate The VAT rate (optional)
     * @param category The expense category (optional)
     * @param description The expense description (optional)
     * @param paymentMethod The payment method (optional)
     * @param isDeductible Whether the expense is tax deductible (optional)
     * @param deductiblePercentage The percentage that is deductible (optional)
     * @param isRecurring Whether this is a recurring expense (optional)
     * @param notes Additional notes (optional)
     * @throws IllegalArgumentException if expense not found
     */
    suspend fun update(
        expenseId: ExpenseId,
        date: LocalDate? = null,
        merchant: String? = null,
        amount: Money? = null,
        vatAmount: Money? = null,
        vatRate: VatRate? = null,
        category: ExpenseCategory? = null,
        description: String? = null,
        paymentMethod: PaymentMethod? = null,
        isDeductible: Boolean? = null,
        deductiblePercentage: Double? = null,
        isRecurring: Boolean? = null,
        notes: String? = null
    )

    /**
     * Deletes an expense
     *
     * @param expenseId The expense's unique identifier
     * @throws IllegalArgumentException if expense not found
     */
    suspend fun delete(expenseId: ExpenseId)

    /**
     * Finds an expense by its unique ID
     *
     * @param id The expense's unique identifier
     * @return The expense if found, null otherwise
     */
    suspend fun findById(id: ExpenseId): Expense?

    /**
     * Lists all expenses for a tenant
     *
     * @param tenantId The tenant's unique identifier
     * @param category Filter by category (optional)
     * @param fromDate Filter expenses on or after this date (optional)
     * @param toDate Filter expenses on or before this date (optional)
     * @param merchant Filter by merchant name (optional)
     * @param limit Maximum number of results (optional)
     * @param offset Pagination offset (optional)
     * @return List of expenses
     */
    suspend fun listByTenant(
        tenantId: TenantId,
        category: ExpenseCategory? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        merchant: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): List<Expense>

    /**
     * Uploads a receipt for an expense
     * Stores the file in S3/MinIO and associates it with the expense
     *
     * @param expenseId The expense's unique identifier
     * @param fileContent The receipt file content
     * @param filename The original filename
     * @param contentType The file content type (e.g., "image/jpeg", "application/pdf")
     * @return The URL where the receipt was stored
     * @throws IllegalArgumentException if expense not found or file invalid
     */
    suspend fun uploadReceipt(
        expenseId: ExpenseId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): String

    /**
     * Downloads a receipt for an expense
     *
     * @param expenseId The expense's unique identifier
     * @return The receipt file content, or null if no receipt exists
     * @throws IllegalArgumentException if expense not found
     */
    suspend fun downloadReceipt(expenseId: ExpenseId): ByteArray?

    /**
     * Deletes the receipt for an expense
     *
     * @param expenseId The expense's unique identifier
     * @throws IllegalArgumentException if expense not found
     */
    suspend fun deleteReceipt(expenseId: ExpenseId)

    /**
     * Auto-categorizes an expense using AI/ML
     * Uses merchant name and description to suggest a category
     *
     * @param merchant The merchant name
     * @param description The expense description (optional)
     * @return The suggested expense category
     */
    suspend fun categorize(merchant: String, description: String? = null): ExpenseCategory

    /**
     * Lists recurring expenses for a tenant
     *
     * @param tenantId The tenant's unique identifier
     * @return List of recurring expenses
     */
    suspend fun listRecurring(tenantId: TenantId): List<Expense>

    /**
     * Watches expense updates for a tenant in real-time
     * Returns a Flow that emits whenever expenses are created or updated
     *
     * @param tenantId The tenant's unique identifier
     * @return Flow of expense updates
     */
    suspend fun watchExpenses(tenantId: TenantId): Flow<Expense>

    /**
     * Gets expense statistics for a tenant
     *
     * @param tenantId The tenant's unique identifier
     * @param fromDate Start date for statistics (optional)
     * @param toDate End date for statistics (optional)
     * @return Map of statistics (totalExpenses, totalDeductible, byCategory, etc.)
     */
    suspend fun getStatistics(
        tenantId: TenantId,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): Map<String, Any>

    /**
     * Calculates total deductible amount for an expense
     *
     * @param amount The expense amount
     * @param deductiblePercentage The percentage that is deductible
     * @return The deductible amount
     */
    suspend fun calculateDeductible(amount: Money, deductiblePercentage: Double): Money
}
