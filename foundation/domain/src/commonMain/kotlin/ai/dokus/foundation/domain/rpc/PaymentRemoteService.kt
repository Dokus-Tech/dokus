package ai.dokus.foundation.domain.rpc

import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.ids.PaymentId
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.TransactionId
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.model.PaymentDto
import ai.dokus.foundation.domain.model.PaymentEvent
import ai.dokus.foundation.domain.model.PaymentStats
import ai.dokus.foundation.domain.model.RecordPaymentRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.rpc.annotations.Rpc

@Rpc
interface PaymentRemoteService {

    /**
     * Record a new payment against an invoice
     * Automatically updates invoice status (PartiallyPaid, Paid)
     */
    suspend fun recordPayment(request: RecordPaymentRequest): PaymentDto

    /**
     * Get a payment by ID
     * Enforces tenant isolation
     */
    suspend fun getPayment(id: PaymentId): PaymentDto

    /**
     * List all payments for a tenant
     * Supports date range and payment method filtering
     */
    suspend fun listPayments(
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        paymentMethod: PaymentMethod? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<PaymentDto>

    /**
     * Get all payments for a specific invoice
     * Used to display payment history
     */
    suspend fun getPaymentsByInvoice(
        invoiceId: InvoiceId
    ): List<PaymentDto>

    /**
     * Update an existing payment
     * All fields are optional - only provided fields will be updated
     */
    suspend fun updatePayment(
        id: PaymentId,
        amount: Money? = null,
        paymentDate: LocalDate? = null,
        paymentMethod: PaymentMethod? = null,
        transactionId: TransactionId? = null,
        notes: String? = null
    ): PaymentDto

    /**
     * Delete a payment
     * Will recalculate invoice payment status
     */
    suspend fun deletePayment(id: PaymentId)

    /**
     * Find payment by transaction ID
     * Used for webhook reconciliation from payment providers (Stripe, Mollie)
     */
    suspend fun findByTransactionId(
        transactionId: TransactionId
    ): PaymentDto?

    /**
     * Get payment statistics for dashboard
     * Returns total revenue, payment counts, etc.
     */
    suspend fun getPaymentStats(
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): PaymentStats

    /**
     * Watch for payment changes in real-time
     * Emits events when payments are recorded, updated, or deleted
     */
    fun watchPayments(organizationId: OrganizationId): Flow<PaymentEvent>
}
