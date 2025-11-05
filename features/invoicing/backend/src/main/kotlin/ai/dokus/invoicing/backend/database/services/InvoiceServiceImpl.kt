@file:OptIn(kotlin.time.ExperimentalTime::class)

package ai.dokus.invoicing.backend.database.services

import ai.dokus.foundation.domain.ClientId
import ai.dokus.foundation.domain.InvoiceId
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.Invoice
import ai.dokus.foundation.domain.model.InvoiceItem
import ai.dokus.foundation.domain.model.InvoiceTotals
import ai.dokus.foundation.domain.model.RecordPaymentRequest
import ai.dokus.foundation.domain.model.UpdateInvoiceStatusRequest
import ai.dokus.foundation.ktor.services.InvoiceService
import ai.dokus.foundation.ktor.services.TenantService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class InvoiceServiceImpl(
    private val tenantService: TenantService,
    private val auditService: ai.dokus.foundation.ktor.services.AuditService
) : InvoiceService {

    override suspend fun create(request: CreateInvoiceRequest): Invoice {
        TODO("Not yet implemented")
    }

    override suspend fun update(
        invoiceId: InvoiceId,
        issueDate: LocalDate?,
        dueDate: LocalDate?,
        notes: String?,
        termsAndConditions: String?
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun updateItems(invoiceId: InvoiceId, items: List<InvoiceItem>) {
        TODO("Not yet implemented")
    }

    override suspend fun delete(invoiceId: InvoiceId) {
        TODO("Not yet implemented")
    }

    override suspend fun findById(id: InvoiceId): Invoice? {
        TODO("Not yet implemented")
    }

    override suspend fun listByTenant(
        tenantId: TenantId,
        status: InvoiceStatus?,
        clientId: ClientId?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        limit: Int?,
        offset: Int?
    ): List<Invoice> {
        TODO("Not yet implemented")
    }

    override suspend fun listByClient(clientId: ClientId, status: InvoiceStatus?): List<Invoice> {
        TODO("Not yet implemented")
    }

    override suspend fun listOverdue(tenantId: TenantId): List<Invoice> {
        TODO("Not yet implemented")
    }

    override suspend fun updateStatus(request: UpdateInvoiceStatusRequest) {
        TODO("Not yet implemented")
    }

    override suspend fun recordPayment(request: RecordPaymentRequest) {
        TODO("Not yet implemented")
    }

    override suspend fun sendViaEmail(
        invoiceId: InvoiceId,
        recipientEmail: String?,
        ccEmails: List<String>?,
        message: String?
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun sendViaPeppol(invoiceId: InvoiceId) {
        TODO("Not yet implemented")
    }

    override suspend fun generatePDF(invoiceId: InvoiceId): ByteArray {
        TODO("Not yet implemented")
    }

    override suspend fun generatePaymentLink(invoiceId: InvoiceId, expiresAt: Instant?): String {
        TODO("Not yet implemented")
    }

    override suspend fun markAsSent(invoiceId: InvoiceId) {
        TODO("Not yet implemented")
    }

    override fun watchInvoices(tenantId: TenantId): Flow<Invoice> {
        return emptyFlow()
    }

    override suspend fun calculateTotals(items: List<InvoiceItem>): InvoiceTotals {
        TODO("Not yet implemented")
    }

    override suspend fun getStatistics(
        tenantId: TenantId,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): Map<String, Money> {
        TODO("Not yet implemented")
    }
}
