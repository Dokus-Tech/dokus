package ai.dokus.peppol.backend.service

import ai.dokus.foundation.domain.ids.ClientId
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.ClientDto
import ai.dokus.foundation.domain.model.CreateBillRequest
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.TenantSettings

/**
 * Interface for cashflow service operations.
 * This interface abstracts the communication with the Cashflow microservice.
 *
 * Implementations can use:
 * - HTTP client for inter-service communication
 * - Direct database access if deployed together
 */
interface ICashflowService {
    /**
     * Get an invoice by ID.
     */
    suspend fun getInvoice(invoiceId: InvoiceId, tenantId: TenantId): Result<FinancialDocumentDto.InvoiceDto?>

    /**
     * Get a client by ID.
     */
    suspend fun getClient(clientId: ClientId, tenantId: TenantId): Result<ClientDto?>

    /**
     * Get tenant settings.
     */
    suspend fun getTenantSettings(tenantId: TenantId): Result<TenantSettings?>

    /**
     * Create a bill from a Peppol document.
     */
    suspend fun createBill(request: CreateBillRequest, tenantId: TenantId): Result<FinancialDocumentDto.BillDto>
}
