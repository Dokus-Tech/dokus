package ai.dokus.foundation.database.services

import ai.dokus.foundation.domain.InvoiceId
import ai.dokus.foundation.domain.Money
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

/**
 * Payment gateway interface for processing online payments
 */
interface PaymentGateway {
    /**
     * Create a payment intent for a client to pay
     * @param invoiceId The invoice being paid
     * @param amount The amount to charge
     * @param currency The currency code (EUR, USD, etc.)
     * @param customerEmail The customer's email
     * @param metadata Additional metadata to attach to the payment
     * @return Payment intent details including client secret for frontend
     */
    suspend fun createPaymentIntent(
        invoiceId: InvoiceId,
        amount: Money,
        currency: String,
        customerEmail: String,
        metadata: Map<String, String> = emptyMap()
    ): PaymentIntentResult

    /**
     * Confirm a payment was successful
     * @param paymentIntentId The payment intent ID from the provider
     * @return Payment confirmation details
     */
    suspend fun confirmPayment(paymentIntentId: String): PaymentConfirmation

    /**
     * Refund a payment
     * @param paymentId The original payment ID
     * @param amount The amount to refund (null for full refund)
     * @param reason The reason for the refund
     * @return Refund details
     */
    suspend fun refundPayment(
        paymentId: String,
        amount: Money? = null,
        reason: String? = null
    ): RefundResult

    /**
     * Retrieve payment details
     * @param paymentIntentId The payment intent ID
     * @return Payment details
     */
    suspend fun getPayment(paymentIntentId: String): PaymentDetails?

    /**
     * Create a customer in the payment gateway
     * @param email Customer email
     * @param name Customer name
     * @param metadata Additional customer metadata
     * @return Customer ID in the payment gateway
     */
    suspend fun createCustomer(
        email: String,
        name: String,
        metadata: Map<String, String> = emptyMap()
    ): String

    /**
     * Verify webhook signature
     * @param payload The webhook payload
     * @param signature The webhook signature header
     * @return True if signature is valid
     */
    suspend fun verifyWebhookSignature(payload: String, signature: String): Boolean

    /**
     * Parse webhook event
     * @param payload The webhook payload
     * @return Parsed webhook event
     */
    suspend fun parseWebhookEvent(payload: String): WebhookEvent
}

/**
 * Payment intent creation result
 */
@Serializable
data class PaymentIntentResult(
    val paymentIntentId: String,
    val clientSecret: String,
    val amount: Money,
    val currency: String,
    val status: String
)

/**
 * Payment confirmation details
 */
@Serializable
data class PaymentConfirmation(
    val paymentIntentId: String,
    val transactionId: String,
    val amount: Money,
    val currency: String,
    val status: String,
    val paymentMethod: String,
    val receiptUrl: String?
)

/**
 * Refund result
 */
@Serializable
data class RefundResult(
    val refundId: String,
    val paymentIntentId: String,
    val amount: Money,
    val status: String,
    val reason: String?
)

/**
 * Payment details from gateway
 */
@Serializable
data class PaymentDetails(
    val paymentIntentId: String,
    val amount: Money,
    val currency: String,
    val status: String,
    val customerId: String?,
    val customerEmail: String?,
    val paymentMethod: String?,
    val receiptUrl: String?,
    val metadata: Map<String, String>
)

/**
 * Webhook event types
 */
sealed class WebhookEvent {
    data class PaymentSucceeded(
        val paymentIntentId: String,
        val amount: Money,
        val currency: String,
        val customerEmail: String?,
        val metadata: Map<String, String>
    ) : WebhookEvent()

    data class PaymentFailed(
        val paymentIntentId: String,
        val errorMessage: String
    ) : WebhookEvent()

    data class RefundCreated(
        val refundId: String,
        val paymentIntentId: String,
        val amount: Money
    ) : WebhookEvent()

    data class Unknown(val eventType: String) : WebhookEvent()
}

/**
 * Stripe payment gateway implementation
 */
class StripePaymentGateway(
    private val apiKey: String,
    private val webhookSecret: String,
    private val publishableKey: String
) : PaymentGateway {

    private val logger = LoggerFactory.getLogger(StripePaymentGateway::class.java)

    override suspend fun createPaymentIntent(
        invoiceId: InvoiceId,
        amount: Money,
        currency: String,
        customerEmail: String,
        metadata: Map<String, String>
    ): PaymentIntentResult {
        logger.info("Creating Stripe payment intent: invoice=$invoiceId, amount=$amount, currency=$currency")

        // TODO: Implement Stripe API integration
        // val stripe = Stripe(apiKey)
        // val amountInCents = (BigDecimal(amount.value) * BigDecimal("100")).toLong()
        //
        // val params = PaymentIntentCreateParams.builder()
        //     .setAmount(amountInCents)
        //     .setCurrency(currency.lowercase())
        //     .setReceiptEmail(customerEmail)
        //     .putAllMetadata(
        //         metadata + mapOf(
        //             "invoice_id" to invoiceId.value.toString(),
        //             "customer_email" to customerEmail
        //         )
        //     )
        //     .setAutomaticPaymentMethods(
        //         PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
        //             .setEnabled(true)
        //             .build()
        //     )
        //     .build()
        //
        // val paymentIntent = PaymentIntent.create(params)
        //
        // return PaymentIntentResult(
        //     paymentIntentId = paymentIntent.id,
        //     clientSecret = paymentIntent.clientSecret,
        //     amount = amount,
        //     currency = currency,
        //     status = paymentIntent.status
        // )

        throw NotImplementedError("Stripe integration not yet configured - add Stripe SDK dependency to build.gradle.kts")
    }

    override suspend fun confirmPayment(paymentIntentId: String): PaymentConfirmation {
        logger.info("Confirming Stripe payment: paymentIntentId=$paymentIntentId")

        // TODO: Implement Stripe payment confirmation
        // val paymentIntent = PaymentIntent.retrieve(paymentIntentId)
        //
        // return PaymentConfirmation(
        //     paymentIntentId = paymentIntent.id,
        //     transactionId = paymentIntent.charges.data.firstOrNull()?.id ?: paymentIntent.id,
        //     amount = Money((paymentIntent.amount / 100.0).toString()),
        //     currency = paymentIntent.currency.uppercase(),
        //     status = paymentIntent.status,
        //     paymentMethod = paymentIntent.paymentMethod?.toString() ?: "card",
        //     receiptUrl = paymentIntent.charges.data.firstOrNull()?.receiptUrl
        // )

        throw NotImplementedError("Stripe integration not yet configured - add Stripe SDK dependency to build.gradle.kts")
    }

    override suspend fun refundPayment(
        paymentId: String,
        amount: Money?,
        reason: String?
    ): RefundResult {
        logger.info("Refunding Stripe payment: paymentId=$paymentId, amount=$amount, reason=$reason")

        // TODO: Implement Stripe refund
        // val params = RefundCreateParams.builder()
        //     .setPaymentIntent(paymentId)
        //     .apply {
        //         amount?.let { setAmount((BigDecimal(it.value) * BigDecimal("100")).toLong()) }
        //         reason?.let { setReason(RefundCreateParams.Reason.valueOf(it.uppercase())) }
        //     }
        //     .build()
        //
        // val refund = Refund.create(params)
        //
        // return RefundResult(
        //     refundId = refund.id,
        //     paymentIntentId = refund.paymentIntent,
        //     amount = Money((refund.amount / 100.0).toString()),
        //     status = refund.status,
        //     reason = refund.reason?.value
        // )

        throw NotImplementedError("Stripe integration not yet configured - add Stripe SDK dependency to build.gradle.kts")
    }

    override suspend fun getPayment(paymentIntentId: String): PaymentDetails? {
        logger.info("Retrieving Stripe payment: paymentIntentId=$paymentIntentId")

        // TODO: Implement Stripe payment retrieval
        // val paymentIntent = PaymentIntent.retrieve(paymentIntentId)
        //
        // return PaymentDetails(
        //     paymentIntentId = paymentIntent.id,
        //     amount = Money((paymentIntent.amount / 100.0).toString()),
        //     currency = paymentIntent.currency.uppercase(),
        //     status = paymentIntent.status,
        //     customerId = paymentIntent.customer,
        //     customerEmail = paymentIntent.receiptEmail,
        //     paymentMethod = paymentIntent.paymentMethod?.toString(),
        //     receiptUrl = paymentIntent.charges.data.firstOrNull()?.receiptUrl,
        //     metadata = paymentIntent.metadata
        // )

        throw NotImplementedError("Stripe integration not yet configured - add Stripe SDK dependency to build.gradle.kts")
    }

    override suspend fun createCustomer(
        email: String,
        name: String,
        metadata: Map<String, String>
    ): String {
        logger.info("Creating Stripe customer: email=$email, name=$name")

        // TODO: Implement Stripe customer creation
        // val params = CustomerCreateParams.builder()
        //     .setEmail(email)
        //     .setName(name)
        //     .putAllMetadata(metadata)
        //     .build()
        //
        // val customer = Customer.create(params)
        // return customer.id

        throw NotImplementedError("Stripe integration not yet configured - add Stripe SDK dependency to build.gradle.kts")
    }

    override suspend fun verifyWebhookSignature(payload: String, signature: String): Boolean {
        logger.debug("Verifying Stripe webhook signature")

        // TODO: Implement Stripe webhook signature verification
        // return try {
        //     Webhook.constructEvent(payload, signature, webhookSecret)
        //     true
        // } catch (e: SignatureVerificationException) {
        //     logger.warn("Invalid webhook signature", e)
        //     false
        // }

        throw NotImplementedError("Stripe integration not yet configured - add Stripe SDK dependency to build.gradle.kts")
    }

    override suspend fun parseWebhookEvent(payload: String): WebhookEvent {
        logger.info("Parsing Stripe webhook event")

        // TODO: Implement Stripe webhook event parsing
        // val event = Event.GSON.fromJson(payload, Event::class.java)
        //
        // return when (event.type) {
        //     "payment_intent.succeeded" -> {
        //         val paymentIntent = event.dataObjectDeserializer.`object`.get() as PaymentIntent
        //         WebhookEvent.PaymentSucceeded(
        //             paymentIntentId = paymentIntent.id,
        //             amount = Money((paymentIntent.amount / 100.0).toString()),
        //             currency = paymentIntent.currency.uppercase(),
        //             customerEmail = paymentIntent.receiptEmail,
        //             metadata = paymentIntent.metadata
        //         )
        //     }
        //     "payment_intent.payment_failed" -> {
        //         val paymentIntent = event.dataObjectDeserializer.`object`.get() as PaymentIntent
        //         WebhookEvent.PaymentFailed(
        //             paymentIntentId = paymentIntent.id,
        //             errorMessage = paymentIntent.lastPaymentError?.message ?: "Payment failed"
        //         )
        //     }
        //     "charge.refunded" -> {
        //         val charge = event.dataObjectDeserializer.`object`.get() as Charge
        //         WebhookEvent.RefundCreated(
        //             refundId = charge.refunds.data.firstOrNull()?.id ?: "unknown",
        //             paymentIntentId = charge.paymentIntent,
        //             amount = Money((charge.amountRefunded / 100.0).toString())
        //         )
        //     }
        //     else -> WebhookEvent.Unknown(event.type)
        // }

        throw NotImplementedError("Stripe integration not yet configured - add Stripe SDK dependency to build.gradle.kts")
    }
}

/**
 * Mock payment gateway for testing
 */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class MockPaymentGateway : PaymentGateway {
    private val logger = LoggerFactory.getLogger(MockPaymentGateway::class.java)
    private val payments = mutableMapOf<String, PaymentDetails>()
    private val refunds = mutableListOf<RefundResult>()
    private var paymentIntentCounter = 0

    override suspend fun createPaymentIntent(
        invoiceId: InvoiceId,
        amount: Money,
        currency: String,
        customerEmail: String,
        metadata: Map<String, String>
    ): PaymentIntentResult {
        logger.info("MOCK: Creating payment intent for invoice $invoiceId, amount $amount")

        val paymentIntentId = "pi_mock_${++paymentIntentCounter}"
        val clientSecret = "${paymentIntentId}_secret_mock"

        payments[paymentIntentId] = PaymentDetails(
            paymentIntentId = paymentIntentId,
            amount = amount,
            currency = currency,
            status = "requires_payment_method",
            customerId = null,
            customerEmail = customerEmail,
            paymentMethod = null,
            receiptUrl = null,
            metadata = metadata + mapOf("invoice_id" to invoiceId.value.toString())
        )

        return PaymentIntentResult(
            paymentIntentId = paymentIntentId,
            clientSecret = clientSecret,
            amount = amount,
            currency = currency,
            status = "requires_payment_method"
        )
    }

    override suspend fun confirmPayment(paymentIntentId: String): PaymentConfirmation {
        logger.info("MOCK: Confirming payment $paymentIntentId")

        val payment = payments[paymentIntentId]
            ?: throw IllegalArgumentException("Payment intent not found: $paymentIntentId")

        payments[paymentIntentId] = payment.copy(
            status = "succeeded",
            paymentMethod = "card",
            receiptUrl = "https://mock.stripe.com/receipts/$paymentIntentId"
        )

        return PaymentConfirmation(
            paymentIntentId = paymentIntentId,
            transactionId = "ch_mock_${paymentIntentId}",
            amount = payment.amount,
            currency = payment.currency,
            status = "succeeded",
            paymentMethod = "card",
            receiptUrl = "https://mock.stripe.com/receipts/$paymentIntentId"
        )
    }

    override suspend fun refundPayment(
        paymentId: String,
        amount: Money?,
        reason: String?
    ): RefundResult {
        logger.info("MOCK: Refunding payment $paymentId")

        val payment = payments[paymentId]
            ?: throw IllegalArgumentException("Payment not found: $paymentId")

        val refundAmount = amount ?: payment.amount
        val refundId = "re_mock_${refunds.size + 1}"

        val result = RefundResult(
            refundId = refundId,
            paymentIntentId = paymentId,
            amount = refundAmount,
            status = "succeeded",
            reason = reason
        )

        refunds.add(result)
        return result
    }

    override suspend fun getPayment(paymentIntentId: String): PaymentDetails? {
        return payments[paymentIntentId]
    }

    override suspend fun createCustomer(
        email: String,
        name: String,
        metadata: Map<String, String>
    ): String {
        logger.info("MOCK: Creating customer $email")
        return "cus_mock_${email.hashCode()}"
    }

    override suspend fun verifyWebhookSignature(payload: String, signature: String): Boolean {
        logger.debug("MOCK: Verifying webhook signature")
        return true // Always valid in mock
    }

    override suspend fun parseWebhookEvent(payload: String): WebhookEvent {
        logger.info("MOCK: Parsing webhook event")
        // Simple mock - always return payment succeeded
        return WebhookEvent.PaymentSucceeded(
            paymentIntentId = "pi_mock_webhook",
            amount = Money("100.00"),
            currency = "EUR",
            customerEmail = "test@example.com",
            metadata = emptyMap()
        )
    }

    /**
     * Get all payments (for testing)
     */
    fun getAllPayments(): Map<String, PaymentDetails> = payments.toMap()

    /**
     * Get all refunds (for testing)
     */
    fun getAllRefunds(): List<RefundResult> = refunds.toList()

    /**
     * Clear all data (for testing)
     */
    fun clear() {
        payments.clear()
        refunds.clear()
        paymentIntentCounter = 0
    }
}
