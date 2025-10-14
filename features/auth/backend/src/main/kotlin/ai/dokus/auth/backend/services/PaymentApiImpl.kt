package ai.dokus.auth.backend.services

import ai.dokus.foundation.apispec.PaymentApi
import ai.dokus.foundation.apispec.PaymentEvent
import ai.dokus.foundation.apispec.PaymentStats
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.model.Payment
import ai.dokus.foundation.domain.model.RecordPaymentRequest
import ai.dokus.foundation.ktor.services.PaymentService
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class PaymentApiImpl(
    private val paymentService: PaymentService
) : PaymentApi {

    override suspend fun recordPayment(request: RecordPaymentRequest): Result<Payment> = runCatching {
        // Note: RecordPaymentRequest doesn't include tenantId
        // We need to get the invoice first to extract its tenantId
        // For now, we'll need to add tenantId to RecordPaymentRequest or fetch invoice first
        // TODO: Update RecordPaymentRequest to include tenantId or fetch invoice to get tenantId
        throw NotImplementedError("recordPayment requires tenantId - RecordPaymentRequest needs to be updated")
    }

    override suspend fun getPayment(id: PaymentId, tenantId: TenantId): Result<Payment> = runCatching {
        val payment = paymentService.findById(id)
            ?: throw IllegalArgumentException("Payment not found: $id")

        // Verify tenant isolation
        if (payment.tenantId != tenantId) {
            throw IllegalArgumentException("Payment does not belong to tenant: $tenantId")
        }

        payment
    }

    override suspend fun listPayments(
        tenantId: TenantId,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        paymentMethod: PaymentMethod?,
        limit: Int,
        offset: Int
    ): Result<List<Payment>> = runCatching {
        paymentService.listByTenant(
            tenantId = tenantId,
            fromDate = fromDate,
            toDate = toDate,
            paymentMethod = paymentMethod,
            limit = limit,
            offset = offset
        )
    }

    override suspend fun getPaymentsByInvoice(
        invoiceId: InvoiceId,
        tenantId: TenantId
    ): Result<List<Payment>> = runCatching {
        val payments = paymentService.listByInvoice(invoiceId)

        // Verify tenant isolation
        payments.forEach { payment ->
            if (payment.tenantId != tenantId) {
                throw IllegalArgumentException("Invoice does not belong to tenant: $tenantId")
            }
        }

        payments
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
        // Verify payment exists and belongs to tenant
        val existingPayment = paymentService.findById(id)
            ?: throw IllegalArgumentException("Payment not found: $id")

        if (existingPayment.tenantId != tenantId) {
            throw IllegalArgumentException("Payment does not belong to tenant: $tenantId")
        }

        // Update using reconcile for transaction ID
        if (transactionId != null) {
            paymentService.reconcile(id, transactionId.value)
        }

        // Note: PaymentService doesn't have a general update method yet
        // For now, we can only update transactionId via reconcile
        // TODO: Add update method to PaymentService for other fields

        // Return updated payment
        paymentService.findById(id)
            ?: throw IllegalStateException("Payment disappeared after update: $id")
    }

    override suspend fun deletePayment(id: PaymentId, tenantId: TenantId): Result<Unit> = runCatching {
        // Verify payment exists and belongs to tenant
        val payment = paymentService.findById(id)
            ?: throw IllegalArgumentException("Payment not found: $id")

        if (payment.tenantId != tenantId) {
            throw IllegalArgumentException("Payment does not belong to tenant: $tenantId")
        }

        paymentService.delete(id)
    }

    override suspend fun findByTransactionId(
        transactionId: TransactionId,
        tenantId: TenantId
    ): Result<Payment?> = runCatching {
        // Note: PaymentService doesn't have findByTransactionId method
        // We'll need to add this to the service layer or use repository directly
        // For now, return null to indicate not implemented
        // TODO: Add findByTransactionId to PaymentService
        null
    }

    override suspend fun getPaymentStats(
        tenantId: TenantId,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ): Result<PaymentStats> = runCatching {
        val payments = paymentService.listByTenant(
            tenantId = tenantId,
            fromDate = fromDate,
            toDate = toDate,
            paymentMethod = null,
            limit = null,
            offset = null
        )

        val totalPayments = payments.size.toLong()
        val totalRevenue = Money(
            payments.sumOf { java.math.BigDecimal(it.amount.value) }.toString()
        )
        val averagePaymentAmount = if (totalPayments > 0) {
            Money(
                (java.math.BigDecimal(totalRevenue.value) / java.math.BigDecimal(totalPayments.toString())).toString()
            )
        } else {
            Money("0")
        }

        val paymentsByMethod = payments
            .groupBy { it.paymentMethod.name }
            .mapValues { it.value.size.toLong() }

        PaymentStats(
            totalPayments = totalPayments,
            totalRevenue = totalRevenue,
            averagePaymentAmount = averagePaymentAmount,
            paymentsByMethod = paymentsByMethod
        )
    }

    override fun watchPayments(tenantId: TenantId): Flow<PaymentEvent> {
        // Note: PaymentService doesn't have a watch method yet
        // This would require implementing a Flow-based real-time update mechanism
        // TODO: Add watchPayments to PaymentService
        throw NotImplementedError("Real-time payment watching not yet implemented")
    }
}
