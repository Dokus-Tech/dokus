package ai.dokus.cashflow.backend.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*

/**
 * Registers all Cashflow REST API routes.
 *
 * This function consolidates all cashflow-related routes and should be called
 * from the main Application configuration.
 *
 * Routes registered:
 * - Invoice routes: /api/v1/invoices
 * - Expense routes: /api/v1/expenses
 * - Attachment routes: /api/v1/invoices/{id}/attachments, /api/v1/expenses/{id}/attachments, /api/v1/attachments
 * - Cashflow overview routes: /api/v1/cashflow/overview
 *
 * @see invoiceRoutes
 * @see expenseRoutes
 * @see attachmentRoutes
 * @see cashflowOverviewRoutes
 */
fun Application.configureCashflowRoutes() {
    routing {
        invoiceRoutes()
        expenseRoutes()
        attachmentRoutes()
        cashflowOverviewRoutes()
    }
}
