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
        expenseService.listByTenant(
            tenantId = tenantId,
            category = category,
            fromDate = fromDate,
            toDate = toDate,
            merchant = null,
            limit = null,
            offset = null
        )
    }

    override suspend fun categorizeExpense(merchant: String, description: String?): Result<ExpenseCategory> = runCatching {
        expenseService.categorize(merchant, description)
    }

    override suspend fun uploadReceipt(
        expenseId: ExpenseId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): Result<String> = runCatching {
        expenseService.uploadReceipt(expenseId, fileContent, filename, contentType)
    }

    override suspend fun deleteExpense(expenseId: ExpenseId): Result<Unit> = runCatching {
        expenseService.delete(expenseId)
    }

    override fun watchExpenses(tenantId: TenantId): Flow<Expense> {
        return expenseService.watchExpenses(tenantId)
    }
}
