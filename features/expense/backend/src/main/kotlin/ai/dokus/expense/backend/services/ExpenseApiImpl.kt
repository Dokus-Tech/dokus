package ai.dokus.expense.backend.services

import ai.dokus.foundation.apispec.ExpenseApi
import ai.dokus.foundation.domain.ExpenseId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.model.CreateExpenseRequest
import ai.dokus.foundation.domain.model.Expense
import ai.dokus.foundation.ktor.services.ExpenseService
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class ExpenseApiImpl(
    private val expenseService: ExpenseService
) : ExpenseApi {

    override suspend fun createExpense(request: CreateExpenseRequest): Result<Expense> = runCatching {
        expenseService.create(request)
    }

    override suspend fun getExpense(id: ExpenseId): Result<Expense> = runCatching {
        expenseService.findById(id) ?: throw IllegalArgumentException("Expense not found: $id")
    }

    override suspend fun listExpenses(
        tenantId: TenantId,
        category: ExpenseCategory?,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): Result<List<Expense>> = runCatching {
        expenseService.listByTenant(tenantId, 1000, 0)
            .filter { expense ->
                (category == null || expense.category == category) &&
                (fromDate == null || expense.date >= fromDate) &&
                (toDate == null || expense.date <= toDate)
            }
    }

    override suspend fun categorizeExpense(merchant: String, description: String?): Result<ExpenseCategory> = runCatching {
        // Simple categorization logic - can be enhanced with ML later
        when {
            merchant.contains("software", ignoreCase = true) -> ExpenseCategory.SOFTWARE
            merchant.contains("travel", ignoreCase = true) -> ExpenseCategory.TRAVEL
            merchant.contains("office", ignoreCase = true) -> ExpenseCategory.OFFICE_SUPPLIES
            else -> ExpenseCategory.OTHER
        }
    }

    override suspend fun uploadReceipt(
        expenseId: ExpenseId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): Result<String> = runCatching {
        // TODO: Implement S3 upload
        "receipt-url-placeholder"
    }

    override suspend fun deleteExpense(expenseId: ExpenseId): Result<Unit> = runCatching {
        expenseService.delete(expenseId)
    }

    override fun watchExpenses(tenantId: TenantId): Flow<Expense> {
        return expenseService.watchExpenses(tenantId)
    }
}
