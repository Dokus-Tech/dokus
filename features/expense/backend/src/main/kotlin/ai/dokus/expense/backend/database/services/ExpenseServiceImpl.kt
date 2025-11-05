@file:OptIn(kotlin.time.ExperimentalTime::class)

package ai.dokus.expense.backend.database.services

import ai.dokus.foundation.domain.ExpenseId
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.model.CreateExpenseRequest
import ai.dokus.foundation.domain.model.Expense
import ai.dokus.foundation.ktor.services.ExpenseService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class ExpenseServiceImpl(
    private val auditService: ai.dokus.foundation.ktor.services.AuditService
) : ExpenseService {

    override suspend fun create(request: CreateExpenseRequest): Expense {
        TODO("Not yet implemented")
    }

    override suspend fun update(
        expenseId: ExpenseId,
        date: LocalDate?,
        merchant: String?,
        amount: Money?,
        vatAmount: Money?,
        vatRate: VatRate?,
        category: ExpenseCategory?,
        description: String?,
        paymentMethod: PaymentMethod?,
        isDeductible: Boolean?,
        deductiblePercentage: Double?,
        isRecurring: Boolean?,
        notes: String?
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun delete(expenseId: ExpenseId) {
        TODO("Not yet implemented")
    }

    override suspend fun findById(id: ExpenseId): Expense? {
        TODO("Not yet implemented")
    }

    override suspend fun listByTenant(
        tenantId: TenantId,
        category: ExpenseCategory?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        merchant: String?,
        limit: Int?,
        offset: Int?
    ): List<Expense> {
        TODO("Not yet implemented")
    }

    override suspend fun uploadReceipt(
        expenseId: ExpenseId,
        fileContent: ByteArray,
        filename: String,
        contentType: String
    ): String {
        TODO("Not yet implemented")
    }

    override suspend fun downloadReceipt(expenseId: ExpenseId): ByteArray? {
        TODO("Not yet implemented")
    }

    override suspend fun deleteReceipt(expenseId: ExpenseId) {
        TODO("Not yet implemented")
    }

    override suspend fun categorize(merchant: String, description: String?): ExpenseCategory {
        TODO("Not yet implemented")
    }

    override suspend fun listRecurring(tenantId: TenantId): List<Expense> {
        TODO("Not yet implemented")
    }

    override fun watchExpenses(tenantId: TenantId): Flow<Expense> {
        return emptyFlow()
    }

    override suspend fun getStatistics(
        tenantId: TenantId,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): Map<String, Any> {
        TODO("Not yet implemented")
    }

    override suspend fun calculateDeductible(amount: Money, deductiblePercentage: Double): Money {
        TODO("Not yet implemented")
    }
}
