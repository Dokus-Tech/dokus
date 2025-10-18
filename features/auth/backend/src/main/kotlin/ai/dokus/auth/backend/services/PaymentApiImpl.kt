package ai.dokus.auth.backend.services

import ai.dokus.foundation.apispec.PaymentApi
import ai.dokus.foundation.apispec.PaymentEvent
import ai.dokus.foundation.apispec.PaymentStats
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.model.Payment
import ai.dokus.foundation.domain.model.RecordPaymentRequest
import ai.dokus.foundation.ktor.services.InvoiceService
import ai.dokus.foundation.ktor.services.PaymentService
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class PaymentApiImpl(
    private val paymentService: PaymentService,
    private val invoiceService: InvoiceService
) : PaymentApi {

    override suspend fun recordPayment(request: RecordPaymentRequest): Result<Payment> = runCatching {
        // Get invoice to extract tenantId for proper tenant isolation
        val invoice = invoiceService.findById(request.invoiceId)
            ?: throw IllegalArgumentException("Invoice not found: ${request.invoiceId}")

        // Record payment with all details from the request
        paymentService.recordPayment(
            tenantId = invoice.tenantId,
            invoiceId = request.invoiceId,
            amount = request.amount,
            paymentDate = request.paymentDate,
            paymentMethod = request.paymentMethod,
            transactionId = request.transactionId?.value,
            notes = request.notes
        )
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
        // PaymentService doesn't provide findByTransactionId directly
        // Search through payments by listing all tenant payments and filtering
        val allPayments = paymentService.listByTenant(
            tenantId = tenantId,
            fromDate = null,
            toDate = null,
            paymentMethod = null,
            limit = 10000,
            offset = null
        )

        allPayments.firstOrNull { it.transactionId?.value == transactionId.value }
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
        // Map payment updates to payment events
        // Currently only supports PaymentRecorded events
        return kotlinx.coroutines.flow.flow {
            // Implementation would require a proper event stream from PaymentService
            // For now, this is a placeholder that emits nothing
            // In production, this would connect to a message queue or database change stream
        }
    }
}
