package ai.dokus.foundation.ktor.services

import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.ids.PaymentId
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.model.PaymentDto
import kotlinx.datetime.LocalDate
import kotlinx.rpc.annotations.Rpc

@Rpc
interface PaymentService {
    /**
     * Records a payment against an invoice
     * Automatically updates invoice status when fully paid
     *
     * @param organizationId The tenant's unique identifier
     * @param invoiceId The invoice's unique identifier
     * @param amount The payment amount
     * @param paymentDate The date the payment was received
     * @param paymentMethod The payment method used
     * @param transactionId External transaction ID (optional, from payment provider)
     * @param notes Additional notes about the payment (optional)
     * @return The created payment record
     * @throws IllegalArgumentException if invoice not found or validation fails
     */
    suspend fun recordPayment(
        organizationId: OrganizationId,
        invoiceId: InvoiceId,
        amount: Money,
        paymentDate: LocalDate,
        paymentMethod: PaymentMethod,
        transactionId: String? = null,
        notes: String? = null
    ): PaymentDto

    /**
     * Finds a payment by its unique ID
     *
     * @param id The payment's unique identifier
     * @return The payment if found, null otherwise
     */
    suspend fun findById(id: PaymentId): PaymentDto?

    /**
     * Lists all payments for a specific invoice
     *
     * @param invoiceId The invoice's unique identifier
     * @return List of payments for the invoice
     */
    suspend fun listByInvoice(invoiceId: InvoiceId): List<PaymentDto>

    /**
     * Lists all payments for a tenant
     *
     * @param organizationId The tenant's unique identifier
     * @param fromDate Filter payments on or after this date (optional)
     * @param toDate Filter payments on or before this date (optional)
     * @param paymentMethod Filter by payment method (optional)
     * @param limit Maximum number of results (optional)
     * @param offset Pagination offset (optional)
     * @return List of payments
     */
    suspend fun listByTenant(
        organizationId: OrganizationId,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        paymentMethod: PaymentMethod? = null,
        limit: Int? = null,
        offset: Int? = null
    ): List<PaymentDto>

    /**
     * Deletes a payment record
     * Automatically updates the associated invoice's paid amount and status
     *
     * @param paymentId The payment's unique identifier
     * @throws IllegalArgumentException if payment not found
     */
    suspend fun delete(paymentId: PaymentId)

    /**
     * Reconciles a payment with a bank transaction
     * Links the payment to a specific bank transaction for accounting
     *
     * @param paymentId The payment's unique identifier
     * @param transactionId The bank transaction ID to reconcile with
     * @throws IllegalArgumentException if payment or transaction not found
     */
    suspend fun reconcile(paymentId: PaymentId, transactionId: String)

    /**
     * Gets payment statistics for a tenant
     *
     * @param organizationId The tenant's unique identifier
     * @param fromDate Start date for statistics (optional)
     * @param toDate End date for statistics (optional)
     * @return Map of statistics (totalReceived, byPaymentMethod, averagePaymentTime, etc.)
     */
    suspend fun getStatistics(
        organizationId: OrganizationId,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): Map<String, Any>

    /**
     * Calculates the total amount paid for an invoice
     *
     * @param invoiceId The invoice's unique identifier
     * @return The total amount paid
     */
    suspend fun getTotalPaid(invoiceId: InvoiceId): Money

    /**
     * Checks if an invoice is fully paid
     *
     * @param invoiceId The invoice's unique identifier
     * @return True if the invoice is fully paid, false otherwise
     */
    suspend fun isFullyPaid(invoiceId: InvoiceId): Boolean

    /**
     * Calculates the remaining balance for an invoice
     *
     * @param invoiceId The invoice's unique identifier
     * @return The remaining unpaid amount
     */
    suspend fun getRemainingBalance(invoiceId: InvoiceId): Money
}
