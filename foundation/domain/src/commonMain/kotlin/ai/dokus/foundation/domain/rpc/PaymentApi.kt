package ai.dokus.foundation.domain.rpc

import ai.dokus.foundation.domain.InvoiceId
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.PaymentId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.TransactionId
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.model.Payment
import ai.dokus.foundation.domain.model.RecordPaymentRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.rpc.annotations.Rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Rpc
interface PaymentApi {

    /**
     * Record a new payment against an invoice
     * Automatically updates invoice status (PartiallyPaid, Paid)
     */
    suspend fun recordPayment(request: RecordPaymentRequest): Result<Payment>

    /**
     * Get a payment by ID
     * Enforces tenant isolation
     */
    suspend fun getPayment(id: PaymentId, tenantId: TenantId): Result<Payment>

    /**
     * List all payments for a tenant
     * Supports date range and payment method filtering
     */
    suspend fun listPayments(
        tenantId: TenantId,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        paymentMethod: PaymentMethod? = null,
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<Payment>>

    /**
     * Get all payments for a specific invoice
     * Used to display payment history
     */
    suspend fun getPaymentsByInvoice(
        invoiceId: InvoiceId,
        tenantId: TenantId
    ): Result<List<Payment>>

    /**
     * Update an existing payment
     * All fields are optional - only provided fields will be updated
     */
    suspend fun updatePayment(
        id: PaymentId,
        tenantId: TenantId,
        amount: Money? = null,
        paymentDate: LocalDate? = null,
        paymentMethod: PaymentMethod? = null,
        transactionId: TransactionId? = null,
        notes: String? = null
    ): Result<Payment>

    /**
     * Delete a payment
     * Will recalculate invoice payment status
     */
    suspend fun deletePayment(id: PaymentId, tenantId: TenantId): Result<Unit>

    /**
     * Find payment by transaction ID
     * Used for webhook reconciliation from payment providers (Stripe, Mollie)
     */
    suspend fun findByTransactionId(
        transactionId: TransactionId,
        tenantId: TenantId
    ): Result<Payment?>

    /**
     * Get payment statistics for dashboard
     * Returns total revenue, payment counts, etc.
     */
    suspend fun getPaymentStats(
        tenantId: TenantId,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): Result<PaymentStats>

    /**
     * Watch for payment changes in real-time
     * Emits events when payments are recorded, updated, or deleted
     */
    fun watchPayments(tenantId: TenantId): Flow<PaymentEvent>
}

/**
 * Payment statistics for dashboard
 */
@Serializable
data class PaymentStats(
    val totalPayments: Long,
    val totalRevenue: Money,
    val averagePaymentAmount: Money,
    val paymentsByMethod: Map<String, Long>
)

/**
 * Real-time payment events for reactive UI updates
 */
@Serializable
sealed class PaymentEvent {
    @Serializable
    @SerialName("PaymentEvent.PaymentRecorded")
    data class PaymentRecorded(val payment: Payment) : PaymentEvent()

    @Serializable
    @SerialName("PaymentEvent.PaymentUpdated")
    data class PaymentUpdated(val payment: Payment) : PaymentEvent()

    @Serializable
    @SerialName("PaymentEvent.PaymentDeleted")
    data class PaymentDeleted(val paymentId: PaymentId) : PaymentEvent()
}
