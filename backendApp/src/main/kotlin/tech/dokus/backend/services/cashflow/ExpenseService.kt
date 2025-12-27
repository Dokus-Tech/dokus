package tech.dokus.backend.services.cashflow

import ai.dokus.foundation.database.repository.cashflow.ExpenseRepository
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.ids.ExpenseId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.CreateExpenseRequest
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.common.PaginatedResponse
import tech.dokus.foundation.ktor.utils.loggerFor
import kotlinx.datetime.LocalDate

/**
 * Service for expense business operations.
 *
 * Expenses represent direct expenses/receipts (Cash-Out).
 * This service handles all business logic related to expenses
 * and delegates data access to the repository layer.
 */
class ExpenseService(
    private val expenseRepository: ExpenseRepository
) {
    private val logger = loggerFor()

    /**
     * Create a new expense for a tenant.
     */
    suspend fun createExpense(
        tenantId: TenantId,
        request: CreateExpenseRequest
    ): Result<FinancialDocumentDto.ExpenseDto> {
        logger.info("Creating expense for tenant: $tenantId, merchant: ${request.merchant}")
        return expenseRepository.createExpense(tenantId, request)
            .onSuccess { logger.info("Expense created: ${it.id}") }
            .onFailure { logger.error("Failed to create expense for tenant: $tenantId", it) }
    }

    /**
     * Get an expense by ID.
     */
    suspend fun getExpense(
        expenseId: ExpenseId,
        tenantId: TenantId
    ): Result<FinancialDocumentDto.ExpenseDto?> {
        logger.debug("Fetching expense: {} for tenant: {}", expenseId, tenantId)
        return expenseRepository.getExpense(expenseId, tenantId)
            .onFailure { logger.error("Failed to fetch expense: $expenseId", it) }
    }

    /**
     * List expenses with optional filters.
     */
    suspend fun listExpenses(
        tenantId: TenantId,
        category: ExpenseCategory? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<FinancialDocumentDto.ExpenseDto>> {
        logger.debug(
            "Listing expenses for tenant: {} (category={}, limit={}, offset={})",
            tenantId,
            category,
            limit,
            offset
        )
        return expenseRepository.listExpenses(tenantId, category, fromDate, toDate, limit, offset)
            .onSuccess { logger.debug("Retrieved ${it.items.size} expenses (total=${it.total})") }
            .onFailure { logger.error("Failed to list expenses for tenant: $tenantId", it) }
    }

    /**
     * Update expense details.
     */
    suspend fun updateExpense(
        expenseId: ExpenseId,
        tenantId: TenantId,
        request: CreateExpenseRequest
    ): Result<FinancialDocumentDto.ExpenseDto> {
        logger.info("Updating expense: $expenseId for tenant: $tenantId")
        return expenseRepository.updateExpense(expenseId, tenantId, request)
            .onSuccess { logger.info("Expense updated: $expenseId") }
            .onFailure { logger.error("Failed to update expense: $expenseId", it) }
    }

    /**
     * Delete an expense.
     */
    suspend fun deleteExpense(
        expenseId: ExpenseId,
        tenantId: TenantId
    ): Result<Boolean> {
        logger.info("Deleting expense: $expenseId")
        return expenseRepository.deleteExpense(expenseId, tenantId)
            .onSuccess { logger.info("Expense deleted: $expenseId") }
            .onFailure { logger.error("Failed to delete expense: $expenseId", it) }
    }

    /**
     * Check if an expense exists.
     */
    suspend fun exists(
        expenseId: ExpenseId,
        tenantId: TenantId
    ): Result<Boolean> {
        return expenseRepository.exists(expenseId, tenantId)
    }

    /**
     * Auto-categorize an expense based on merchant name.
     * This can be enhanced with ML-based categorization in the future.
     */
    fun categorizeExpense(merchant: String, description: String? = null): ExpenseCategory {
        val merchantLower = merchant.lowercase()
        val descLower = description?.lowercase() ?: ""

        return when {
            // Office supplies
            merchantLower.contains("office") ||
                    merchantLower.contains("staples") ||
                    merchantLower.contains("amazon") && descLower.contains("office") -> ExpenseCategory.OfficeSupplies

            // Software
            merchantLower.contains("github") ||
                    merchantLower.contains("jetbrains") ||
                    merchantLower.contains("aws") ||
                    merchantLower.contains("google cloud") ||
                    merchantLower.contains("azure") ||
                    merchantLower.contains("digitalocean") ||
                    merchantLower.contains("heroku") -> ExpenseCategory.Software

            // Travel
            merchantLower.contains("airline") ||
                    merchantLower.contains("sncb") ||
                    merchantLower.contains("hotel") ||
                    merchantLower.contains("uber") ||
                    merchantLower.contains("taxi") ||
                    merchantLower.contains("airbnb") -> ExpenseCategory.Travel

            // Meals
            merchantLower.contains("restaurant") ||
                    merchantLower.contains("cafe") ||
                    merchantLower.contains("deliveroo") ||
                    merchantLower.contains("uber eats") -> ExpenseCategory.Meals

            // Hardware/Equipment
            merchantLower.contains("apple") ||
                    merchantLower.contains("dell") ||
                    merchantLower.contains("lenovo") -> ExpenseCategory.Hardware

            // Utilities
            merchantLower.contains("telenet") ||
                    merchantLower.contains("proximus") ||
                    merchantLower.contains("engie") ||
                    merchantLower.contains("luminus") -> ExpenseCategory.Utilities

            // Professional services
            merchantLower.contains("accountant") ||
                    merchantLower.contains("lawyer") ||
                    merchantLower.contains("consultant") -> ExpenseCategory.ProfessionalServices

            // Marketing
            merchantLower.contains("google ads") ||
                    merchantLower.contains("facebook ads") ||
                    merchantLower.contains("linkedin") -> ExpenseCategory.Marketing

            else -> ExpenseCategory.Other
        }
    }
}
