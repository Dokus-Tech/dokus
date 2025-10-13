package ai.dokus.foundation.apispec

import ai.dokus.foundation.domain.InvoiceId
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.Invoice
import ai.dokus.foundation.domain.model.InvoiceItem
import ai.dokus.foundation.domain.model.InvoiceTotals
import ai.dokus.foundation.domain.model.RecordPaymentRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.rpc.annotations.Rpc

@Rpc
interface InvoiceApi {

    suspend fun createInvoice(request: CreateInvoiceRequest): Result<Invoice>

    suspend fun getInvoice(id: InvoiceId): Result<Invoice>

    suspend fun listInvoices(
        tenantId: TenantId,
        status: InvoiceStatus? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): Result<List<Invoice>>

    suspend fun listOverdueInvoices(tenantId: TenantId): Result<List<Invoice>>

    suspend fun updateInvoiceStatus(invoiceId: InvoiceId, status: InvoiceStatus): Result<Unit>

    suspend fun recordPayment(request: RecordPaymentRequest): Result<Unit>

    suspend fun sendInvoiceEmail(
        invoiceId: InvoiceId,
        recipientEmail: String? = null,
        message: String? = null
    ): Result<Unit>

    suspend fun markInvoiceAsSent(invoiceId: InvoiceId): Result<Unit>

    fun watchInvoices(tenantId: TenantId): Flow<Invoice>

    suspend fun calculateInvoiceTotals(items: List<InvoiceItem>): Result<InvoiceTotals>
}
