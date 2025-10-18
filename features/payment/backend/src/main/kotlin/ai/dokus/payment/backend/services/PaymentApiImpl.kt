package ai.dokus.payment.backend.services

import ai.dokus.foundation.apispec.PaymentApi
import ai.dokus.foundation.apispec.PaymentEvent
import ai.dokus.foundation.apispec.PaymentStats
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.model.Payment
import ai.dokus.foundation.domain.model.RecordPaymentRequest
import ai.dokus.foundation.ktor.services.PaymentService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class PaymentApiImpl(
    private val paymentService: PaymentService
) : PaymentApi {

    override suspend fun recordPayment(request: RecordPaymentRequest): Result<Payment> = runCatching {
        paymentService.recordPayment(request)
    }

    override suspend fun getPayment(id: PaymentId, tenantId: TenantId): Result<Payment> = runCatching {
        paymentService.findById(id) ?: throw IllegalArgumentException("Payment not found: $id")
    }

    override suspend fun listPayments(
        tenantId: TenantId,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        paymentMethod: PaymentMethod?,
        limit: Int,
        offset: Int
    ): Result<List<Payment>> = runCatching {
        paymentService.listByTenant(tenantId, limit, offset)
            .filter { payment ->
                (fromDate == null || payment.paymentDate >= fromDate) &&
                (toDate == null || payment.paymentDate <= toDate) &&
                (paymentMethod == null || payment.method == paymentMethod)
            }
    }

    override suspend fun getPaymentsByInvoice(
        invoiceId: InvoiceId,
        tenantId: TenantId
    ): Result<List<Payment>> = runCatching {
        paymentService.listByInvoice(invoiceId)
    }

    override suspend fun updatePayment(
        id: PaymentId,
        tenantId: TenantId,
        amount: Money?,
        paymentDate: LocalDate?,
        paymentMethod: PaymentMethod?,
        transactionId: TransactionId?,
        notes: String?
    ): Result<Payment> = runCatching {
        // TODO: Implement update method in PaymentService
        throw NotImplementedError("Payment update not yet implemented")
    }

    override suspend fun deletePayment(id: PaymentId, tenantId: TenantId): Result<Unit> = runCatching {
        paymentService.deletePayment(id)
    }

    override suspend fun findByTransactionId(
        transactionId: TransactionId,
        tenantId: TenantId
    ): Result<Payment?> = runCatching {
        paymentService.findByTransactionId(transactionId)
    }

    override suspend fun getPaymentStats(
        tenantId: TenantId,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): Result<PaymentStats> = runCatching {
        val payments = paymentService.listByTenant(tenantId, 10000, 0)
            .filter { payment ->
                (fromDate == null || payment.paymentDate >= fromDate) &&
                (toDate == null || payment.paymentDate <= toDate)
            }

        val totalRevenue = payments.fold(Money.ZERO) { acc, payment -> acc + payment.amount }
        val averagePaymentAmount = if (payments.isNotEmpty()) {
            totalRevenue / payments.size
        } else {
            Money.ZERO
        }

        val paymentsByMethod = payments.groupBy { it.method.name }
            .mapValues { it.value.size.toLong() }

        PaymentStats(
            totalPayments = payments.size.toLong(),
            totalRevenue = totalRevenue,
            averagePaymentAmount = averagePaymentAmount,
            paymentsByMethod = paymentsByMethod
        )
    }

    override fun watchPayments(tenantId: TenantId): Flow<PaymentEvent> {
        return paymentService.watchPayments(tenantId).map { payment ->
            PaymentEvent.PaymentRecorded(payment)
        }
    }
}
