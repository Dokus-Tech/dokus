package tech.dokus.backend.routes.cashflow

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
 *
 * Cash-Out (Expenses & Bills):
 * - /api/v1/expenses - Expense CRUD operations
 * - /api/v1/cashflow/cash-out/bills - Bill CRUD operations (supplier invoices)
 *
 * Overview:
 * - /api/v1/cashflow/overview - Cashflow overview dashboard data
 *
 * Attachments:
 * - /api/v1/attachments - Attachment management
 *
 * Documents:
 * - /api/v1/documents/upload - Upload documents to MinIO storage
 * - /api/v1/documents/{id} - Get document by ID with download URL
 * - /api/v1/documents/processing - List documents by processing status
 * - /api/v1/documents/{id}/processing - Get processing details
 * - /api/v1/documents/{id}/confirm - Confirm extraction and create entity
 * - /api/v1/documents/{id}/reject - Reject extraction
 * - /api/v1/documents/{id}/reprocess - Trigger re-extraction
 *
 * Peppol:
 * - /api/v1/peppol - Peppol e-invoicing operations
 *
 * Note: Contact management is handled by the separate contacts microservice.
 *
 * @see invoiceRoutes
 * @see expenseRoutes
 * @see billRoutes
 * @see attachmentRoutes
 * @see cashflowOverviewRoutes
 * @see cashflowDocumentRoutes
 * @see documentUploadRoutes
 * @see documentProcessingRoutes
 * @see peppolRoutes
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

        // Document routes
        cashflowDocumentRoutes()

        // Document upload routes (MinIO)
        documentUploadRoutes()

        // Document record routes (new canonical document API)
        documentRecordRoutes()

        // Peppol e-invoicing routes
        peppolRoutes()

        // Chat routes (RAG-powered document Q&A)
        chatRoutes()
    }
}
