package ai.dokus.cashflow.backend.routes

import io.ktor.server.application.Application
import io.ktor.server.routing.routing

/**
 * Registers all Cashflow REST API routes.
 *
 * This function consolidates all cashflow-related routes and should be called
 * from the main Application configuration.
 *
 * Routes registered:
 *
 * Cash-In (Outgoing Invoices):
 * - /api/v1/invoices - Invoice CRUD operations
 * - /api/v1/cashflow/cash-in/invoices/from-media/{mediaId} - Create invoice from media
 *
 * Cash-Out (Expenses & Bills):
 * - /api/v1/expenses - Expense CRUD operations
 * - /api/v1/cashflow/cash-out/bills - Bill CRUD operations (supplier invoices)
 * - /api/v1/cashflow/cash-out/expenses/from-media/{mediaId} - Create expense from media
 * - /api/v1/cashflow/cash-out/bills/from-media/{mediaId} - Create bill from media
 *
 * Overview:
 * - /api/v1/cashflow/overview - Cashflow overview dashboard data
 *
 * Attachments:
 * - /api/v1/attachments - Attachment management
 *
 * @see invoiceRoutes
 * @see expenseRoutes
 * @see billRoutes
 * @see attachmentRoutes
 * @see cashflowOverviewRoutes
 * @see fromMediaRoutes
 * @see cashflowDocumentRoutes
 */
fun Application.configureCashflowRoutes() {
    routing {
        // Invoice routes (Cash-In)
        invoiceRoutes()

        // Expense routes (Cash-Out)
        expenseRoutes()

        // Bill routes (Cash-Out - supplier invoices)
        billRoutes()

        // Attachment routes
        attachmentRoutes()

        // Cashflow overview
        cashflowOverviewRoutes()

        // From-media creation routes
        fromMediaRoutes()

        // Document routes
        cashflowDocumentRoutes()
    }
}
