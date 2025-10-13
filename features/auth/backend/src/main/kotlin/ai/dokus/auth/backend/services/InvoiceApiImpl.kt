package ai.dokus.auth.backend.services

import ai.dokus.foundation.apispec.InvoiceApi
import ai.dokus.foundation.domain.InvoiceId
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.Invoice
import ai.dokus.foundation.domain.model.RecordPaymentRequest
import ai.dokus.foundation.domain.model.UpdateInvoiceStatusRequest
import ai.dokus.foundation.ktor.services.InvoiceService
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class InvoiceApiImpl(
    private val invoiceService: InvoiceService
) : InvoiceApi {

    override suspend fun createInvoice(request: CreateInvoiceRequest): Result<Invoice> = runCatching {
        invoiceService.create(request)
    }

    override suspend fun getInvoice(id: InvoiceId): Result<Invoice> = runCatching {
        invoiceService.findById(id) ?: throw IllegalArgumentException("Invoice not found: $id")
    }

    override suspend fun listInvoices(
        tenantId: TenantId,
        status: InvoiceStatus?,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): Result<List<Invoice>> = runCatching {
        invoiceService.listByTenant(tenantId, status, null, fromDate, toDate)
    }

    override suspend fun listOverdueInvoices(tenantId: TenantId): Result<List<Invoice>> = runCatching {
        invoiceService.listOverdue(tenantId)
    }

    override suspend fun updateInvoiceStatus(invoiceId: InvoiceId, status: InvoiceStatus): Result<Unit> = runCatching {
        invoiceService.updateStatus(UpdateInvoiceStatusRequest(invoiceId, status))
    }

    override suspend fun recordPayment(request: RecordPaymentRequest): Result<Unit> = runCatching {
        invoiceService.recordPayment(request)
    }

    override suspend fun sendInvoiceEmail(
        invoiceId: InvoiceId,
        recipientEmail: String?,
        message: String?
    ): Result<Unit> = runCatching {
        invoiceService.sendViaEmail(invoiceId, recipientEmail, null, message)
    }

    override suspend fun markInvoiceAsSent(invoiceId: InvoiceId): Result<Unit> = runCatching {
        invoiceService.markAsSent(invoiceId)
    }

    override fun watchInvoices(tenantId: TenantId): Flow<Invoice> {
        return invoiceService.watchInvoices(tenantId)
    }

    override suspend fun calculateInvoiceTotals(
        items: List<InvoiceItemRequest>
    ): Result<Triple<Money, Money, Money>> = runCatching {
        invoiceService.calculateTotals(items)
    }
}
