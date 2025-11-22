@file:OptIn(kotlin.time.ExperimentalTime::class)

package ai.dokus.payment.backend.database.services

import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.ids.PaymentId
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.model.Payment
import ai.dokus.foundation.ktor.services.PaymentService
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class PaymentServiceImpl(
    private val auditService: ai.dokus.foundation.ktor.services.AuditService
) : PaymentService {

    override suspend fun recordPayment(
        organizationId: OrganizationId,
        invoiceId: InvoiceId,
        amount: Money,
        paymentDate: LocalDate,
        paymentMethod: PaymentMethod,
        transactionId: String?,
        notes: String?
    ): Payment {
        TODO("Not yet implemented")
    }

    override suspend fun findById(id: PaymentId): Payment? {
        TODO("Not yet implemented")
    }

    override suspend fun listByInvoice(invoiceId: InvoiceId): List<Payment> {
        TODO("Not yet implemented")
    }

    override suspend fun listByTenant(
        organizationId: OrganizationId,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        paymentMethod: PaymentMethod?,
        limit: Int?,
        offset: Int?
    ): List<Payment> {
        TODO("Not yet implemented")
    }

    override suspend fun delete(paymentId: PaymentId) {
        TODO("Not yet implemented")
    }

    override suspend fun reconcile(paymentId: PaymentId, transactionId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun getStatistics(
        organizationId: OrganizationId,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): Map<String, Any> {
        TODO("Not yet implemented")
    }

    override suspend fun getTotalPaid(invoiceId: InvoiceId): Money {
        TODO("Not yet implemented")
    }

    override suspend fun isFullyPaid(invoiceId: InvoiceId): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getRemainingBalance(invoiceId: InvoiceId): Money {
        TODO("Not yet implemented")
    }
}
