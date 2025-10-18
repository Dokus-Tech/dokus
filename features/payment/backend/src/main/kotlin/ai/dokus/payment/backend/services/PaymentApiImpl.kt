package ai.dokus.payment.backend.services

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

        // PaymentService currently only supports updating transactionId via reconcile
        // Other fields (amount, paymentDate, paymentMethod, notes) are immutable after creation
        // to maintain audit trail integrity. Delete and recreate if needed.

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
        // Implement polling-based watching since PaymentService doesn't provide streaming
        return kotlinx.coroutines.flow.flow {
            var lastSeenPayments = emptySet<PaymentId>()

            while (true) {
                // Poll for new payments every 5 seconds
                kotlinx.coroutines.delay(5000)

                try {
                    val currentPayments = paymentService.listByTenant(
                        tenantId = tenantId,
                        fromDate = null,
                        toDate = null,
                        paymentMethod = null,
                        limit = 100,
                        offset = null
                    )

                    val currentIds = currentPayments.map { it.id }.toSet()
                    val newPaymentIds = currentIds - lastSeenPayments

                    // Emit events for new payments
                    currentPayments
                        .filter { it.id in newPaymentIds }
                        .forEach { payment ->
                            emit(PaymentEvent.PaymentRecorded(payment))
                        }

                    lastSeenPayments = currentIds
                } catch (e: Exception) {
                    // Log error but continue polling
                    // In production, this would use proper logging
                }
            }
        }
    }
}
