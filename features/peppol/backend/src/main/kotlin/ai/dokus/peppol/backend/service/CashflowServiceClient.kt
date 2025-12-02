package ai.dokus.peppol.backend.service

import ai.dokus.foundation.domain.ids.ClientId
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.ClientDto
import ai.dokus.foundation.domain.model.CreateBillRequest
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.TenantSettings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.slf4j.LoggerFactory

/**
 * HTTP client implementation for communicating with the Cashflow microservice.
 * Uses inter-service communication for fetching invoice, client, and tenant data.
 */
class CashflowServiceClient(
    private val httpClient: HttpClient,
    private val cashflowServiceBaseUrl: String
) : ICashflowService {
    private val logger = LoggerFactory.getLogger(CashflowServiceClient::class.java)

    override suspend fun getInvoice(
        invoiceId: InvoiceId,
        tenantId: TenantId
    ): Result<FinancialDocumentDto.InvoiceDto?> = runCatching {
        logger.debug("Fetching invoice $invoiceId from Cashflow service")

        val response = httpClient.get("$cashflowServiceBaseUrl/api/v1/internal/invoices/$invoiceId") {
            header("X-Tenant-Id", tenantId.toString())
        }

        response.body<FinancialDocumentDto.InvoiceDto?>()
    }.onFailure {
        logger.error("Failed to fetch invoice $invoiceId from Cashflow service", it)
    }

    override suspend fun getClient(
        clientId: ClientId,
        tenantId: TenantId
    ): Result<ClientDto?> = runCatching {
        logger.debug("Fetching client $clientId from Cashflow service")

        val response = httpClient.get("$cashflowServiceBaseUrl/api/v1/internal/clients/$clientId") {
            header("X-Tenant-Id", tenantId.toString())
        }

        response.body<ClientDto?>()
    }.onFailure {
        logger.error("Failed to fetch client $clientId from Cashflow service", it)
    }

    override suspend fun getTenantSettings(tenantId: TenantId): Result<TenantSettings?> = runCatching {
        logger.debug("Fetching tenant settings for $tenantId from Auth service")

        // Tenant settings are typically stored in the Auth service
        val authServiceBaseUrl = System.getenv("AUTH_SERVICE_URL") ?: "http://localhost:8001"

        val response = httpClient.get("$authServiceBaseUrl/api/v1/internal/tenants/$tenantId/settings") {
            header("X-Tenant-Id", tenantId.toString())
        }

        response.body<TenantSettings?>()
    }.onFailure {
        logger.error("Failed to fetch tenant settings for $tenantId", it)
    }

    override suspend fun createBill(
        request: CreateBillRequest,
        tenantId: TenantId
    ): Result<FinancialDocumentDto.BillDto> = runCatching {
        logger.info("Creating bill in Cashflow service for tenant $tenantId")

        val response = httpClient.post("$cashflowServiceBaseUrl/api/v1/internal/bills") {
            header("X-Tenant-Id", tenantId.toString())
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        response.body<FinancialDocumentDto.BillDto>()
    }.onFailure {
        logger.error("Failed to create bill in Cashflow service", it)
    }
}
