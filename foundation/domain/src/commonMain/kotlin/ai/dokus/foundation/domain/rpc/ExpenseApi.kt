package ai.dokus.foundation.domain.rpc

import ai.dokus.foundation.domain.ExpenseId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.model.CreateExpenseRequest
import ai.dokus.foundation.domain.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.rpc.annotations.Rpc

@Rpc
interface ExpenseApi {

    suspend fun createExpense(request: CreateExpenseRequest): Result<Expense>

    suspend fun getExpense(id: ExpenseId): Result<Expense>

    suspend fun listExpenses(
        tenantId: TenantId,
        category: ExpenseCategory? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): Result<List<Expense>>

    suspend fun categorizeExpense(merchant: String, description: String? = null): Result<ExpenseCategory>

    suspend fun uploadReceipt(
        expenseId: ExpenseId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): Result<String>

    suspend fun deleteExpense(expenseId: ExpenseId): Result<Unit>

    fun watchExpenses(tenantId: TenantId): Flow<Expense>
}
