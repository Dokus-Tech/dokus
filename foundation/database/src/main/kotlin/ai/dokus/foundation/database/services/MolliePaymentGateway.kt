package ai.dokus.foundation.database.services

import ai.dokus.foundation.domain.*
import org.slf4j.LoggerFactory

/**
 * Mollie payment gateway implementation
 * Popular in Europe, especially Netherlands and Belgium
 * Supports iDEAL, Bancontact, and many other EU payment methods
 */
class MolliePaymentGateway(
    private val apiKey: String,
    private val webhookUrl: String
) : PaymentGateway {

    private val logger = LoggerFactory.getLogger(MolliePaymentGateway::class.java)

    override suspend fun createPaymentIntent(
        invoiceId: InvoiceId,
        amount: Money,
        currency: String,
        customerEmail: String,
        metadata: Map<String, String>
    ): PaymentIntentResult {
        logger.info("Creating Mollie payment: invoice=$invoiceId, amount=$amount, currency=$currency")

        // TODO: Implement Mollie API integration
        // val mollie = MollieClient(apiKey)
        //
        // val paymentRequest = PaymentRequest()
        //     .withAmount(Amount(currency, amount.value))
        //     .withDescription("Invoice ${invoiceId.value}")
        //     .withRedirectUrl("https://app.dokus.ai/payments/success")
        //     .withWebhookUrl(webhookUrl)
        //     .withMetadata(
        //         metadata + mapOf(
        //             "invoice_id" to invoiceId.value.toString(),
        //             "customer_email" to customerEmail
        //         )
        //     )
        //
        // val payment = mollie.payments().create(paymentRequest)
        //
        // return PaymentIntentResult(
        //     paymentIntentId = payment.id,
        //     clientSecret = payment.checkoutUrl, // Mollie uses checkout URL instead of client secret
        //     amount = amount,
        //     currency = currency,
        //     status = payment.status.toString()
        // )

        throw NotImplementedError("Mollie integration not yet configured - add Mollie SDK dependency to build.gradle.kts")
    }

    override suspend fun confirmPayment(paymentIntentId: String): PaymentConfirmation {
        logger.info("Confirming Mollie payment: paymentId=$paymentIntentId")

        // TODO: Implement Mollie payment confirmation
        // val mollie = MollieClient(apiKey)
        // val payment = mollie.payments().get(paymentIntentId)
        //
        // return PaymentConfirmation(
        //     paymentIntentId = payment.id,
        //     transactionId = payment.id, // Mollie uses same ID
        //     amount = Money(payment.amount.value),
        //     currency = payment.amount.currency,
        //     status = payment.status.toString(),
        //     paymentMethod = payment.method?.toString() ?: "unknown",
        //     receiptUrl = null // Mollie doesn't provide receipt URLs
        // )

        throw NotImplementedError("Mollie integration not yet configured - add Mollie SDK dependency to build.gradle.kts")
    }

    override suspend fun refundPayment(
        paymentId: String,
        amount: Money?,
        reason: String?
    ): RefundResult {
        logger.info("Refunding Mollie payment: paymentId=$paymentId, amount=$amount")

        // TODO: Implement Mollie refund
        // val mollie = MollieClient(apiKey)
        //
        // val refundRequest = RefundRequest()
        //     .apply {
        //         amount?.let { withAmount(Amount("EUR", it.value)) }
        //         reason?.let { withDescription(it) }
        //     }
        //
        // val refund = mollie.payments().refund(paymentId, refundRequest)
        //
        // return RefundResult(
        //     refundId = refund.id,
        //     paymentIntentId = paymentId,
        //     amount = Money(refund.amount.value),
        //     status = refund.status.toString(),
        //     reason = refund.description
        // )

        throw NotImplementedError("Mollie integration not yet configured - add Mollie SDK dependency to build.gradle.kts")
    }

    override suspend fun getPayment(paymentIntentId: String): PaymentDetails? {
        logger.info("Retrieving Mollie payment: paymentId=$paymentIntentId")

        // TODO: Implement Mollie payment retrieval
        // val mollie = MollieClient(apiKey)
        // val payment = mollie.payments().get(paymentIntentId)
        //
        // return PaymentDetails(
        //     paymentIntentId = payment.id,
        //     amount = Money(payment.amount.value),
        //     currency = payment.amount.currency,
        //     status = payment.status.toString(),
        //     customerId = payment.customerId,
        //     customerEmail = payment.metadata?.get("customer_email"),
        //     paymentMethod = payment.method?.toString(),
        //     receiptUrl = null,
        //     metadata = payment.metadata ?: emptyMap()
        // )

        throw NotImplementedError("Mollie integration not yet configured - add Mollie SDK dependency to build.gradle.kts")
    }

    override suspend fun createCustomer(
        email: String,
        name: String,
        metadata: Map<String, String>
    ): String {
        logger.info("Creating Mollie customer: email=$email, name=$name")

        // TODO: Implement Mollie customer creation
        // val mollie = MollieClient(apiKey)
        //
        // val customerRequest = CustomerRequest()
        //     .withEmail(email)
        //     .withName(name)
        //     .withMetadata(metadata)
        //
        // val customer = mollie.customers().create(customerRequest)
        // return customer.id

        throw NotImplementedError("Mollie integration not yet configured - add Mollie SDK dependency to build.gradle.kts")
    }

    override suspend fun verifyWebhookSignature(payload: String, signature: String): Boolean {
        logger.debug("Verifying Mollie webhook")

        // Mollie webhooks don't use signature verification
        // Instead, you should fetch the payment/order by ID to verify
        return true
    }

    override suspend fun parseWebhookEvent(payload: String): WebhookEvent {
        logger.info("Parsing Mollie webhook event")

        // TODO: Implement Mollie webhook parsing
        // Mollie webhooks only contain the payment ID
        // You need to fetch the full payment details
        //
        // val webhookData = JSON.parse<Map<String, String>>(payload)
        // val paymentId = webhookData["id"] ?: throw IllegalArgumentException("Missing payment ID")
        //
        // val mollie = MollieClient(apiKey)
        // val payment = mollie.payments().get(paymentId)
        //
        // return when (payment.status) {
        //     PaymentStatus.PAID -> WebhookEvent.PaymentSucceeded(
        //         paymentIntentId = payment.id,
        //         amount = Money(payment.amount.value),
        //         currency = payment.amount.currency,
        //         customerEmail = payment.metadata?.get("customer_email"),
        //         metadata = payment.metadata ?: emptyMap()
        //     )
        //     PaymentStatus.FAILED, PaymentStatus.CANCELED, PaymentStatus.EXPIRED -> WebhookEvent.PaymentFailed(
        //         paymentIntentId = payment.id,
        //         errorMessage = "Payment ${payment.status}"
        //     )
        //     else -> WebhookEvent.Unknown(payment.status.toString())
        // }

        throw NotImplementedError("Mollie integration not yet configured - add Mollie SDK dependency to build.gradle.kts")
    }
}

/**
 * Mollie-specific payment methods
 */
object MolliePaymentMethods {
    const val IDEAL = "ideal"               // iDEAL (Netherlands)
    const val BANCONTACT = "bancontact"     // Bancontact (Belgium)
    const val CREDITCARD = "creditcard"     // Credit card
    const val PAYPAL = "paypal"             // PayPal
    const val SOFORT = "sofort"             // SOFORT Banking (Germany)
    const val EPS = "eps"                   // EPS (Austria)
    const val GIROPAY = "giropay"           // Giropay (Germany)
    const val KBC = "kbc"                   // KBC/CBC Payment Button (Belgium)
    const val BELFIUS = "belfius"           // Belfius Pay Button (Belgium)
    const val PRZELEWY24 = "przelewy24"     // Przelewy24 (Poland)
    const val APPLEPAY = "applepay"         // Apple Pay
    const val SEPA_DIRECT_DEBIT = "directdebit" // SEPA Direct Debit
}

/**
 * Payment gateway factory
 * Creates the appropriate payment gateway based on configuration
 */
object PaymentGatewayFactory {
    private val logger = LoggerFactory.getLogger(PaymentGatewayFactory::class.java)

    fun createGateway(
        provider: String,
        apiKey: String,
        webhookSecret: String = "",
        additionalConfig: Map<String, String> = emptyMap()
    ): PaymentGateway {
        logger.info("Creating payment gateway: provider=$provider")

        return when (provider.lowercase()) {
            "stripe" -> StripePaymentGateway(
                apiKey = apiKey,
                webhookSecret = webhookSecret,
                publishableKey = additionalConfig["publishable_key"] ?: ""
            )

            "mollie" -> MolliePaymentGateway(
                apiKey = apiKey,
                webhookUrl = additionalConfig["webhook_url"] ?: ""
            )

            "mock" -> MockPaymentGateway()

            else -> throw IllegalArgumentException("Unsupported payment provider: $provider")
        }
    }

    /**
     * Create multiple gateways for failover
     * Useful for high-availability payment processing
     */
    fun createMultiGateway(
        primaryProvider: String,
        primaryApiKey: String,
        fallbackProvider: String? = null,
        fallbackApiKey: String? = null
    ): MultiPaymentGateway {
        val primary = createGateway(primaryProvider, primaryApiKey)
        val fallback = if (fallbackProvider != null && fallbackApiKey != null) {
            createGateway(fallbackProvider, fallbackApiKey)
        } else {
            null
        }

        return MultiPaymentGateway(primary, fallback)
    }
}

/**
 * Multi-gateway payment processor with automatic failover
 * If primary gateway fails, automatically tries fallback gateway
 */
class MultiPaymentGateway(
    private val primaryGateway: PaymentGateway,
    private val fallbackGateway: PaymentGateway?
) : PaymentGateway {

    private val logger = LoggerFactory.getLogger(MultiPaymentGateway::class.java)

    override suspend fun createPaymentIntent(
        invoiceId: InvoiceId,
        amount: Money,
        currency: String,
        customerEmail: String,
        metadata: Map<String, String>
    ): PaymentIntentResult {
        return try {
            primaryGateway.createPaymentIntent(invoiceId, amount, currency, customerEmail, metadata)
        } catch (e: Exception) {
            logger.warn("Primary gateway failed, trying fallback", e)
            fallbackGateway?.createPaymentIntent(invoiceId, amount, currency, customerEmail, metadata)
                ?: throw e
        }
    }

    override suspend fun confirmPayment(paymentIntentId: String): PaymentConfirmation {
        return try {
            primaryGateway.confirmPayment(paymentIntentId)
        } catch (e: Exception) {
            logger.warn("Primary gateway failed, trying fallback", e)
            fallbackGateway?.confirmPayment(paymentIntentId) ?: throw e
        }
    }

    override suspend fun refundPayment(
        paymentId: String,
        amount: Money?,
        reason: String?
    ): RefundResult {
        return try {
            primaryGateway.refundPayment(paymentId, amount, reason)
        } catch (e: Exception) {
            logger.warn("Primary gateway failed, trying fallback", e)
            fallbackGateway?.refundPayment(paymentId, amount, reason) ?: throw e
        }
    }

    override suspend fun getPayment(paymentIntentId: String): PaymentDetails? {
        return try {
            primaryGateway.getPayment(paymentIntentId)
        } catch (e: Exception) {
            logger.warn("Primary gateway failed, trying fallback", e)
            fallbackGateway?.getPayment(paymentIntentId)
        }
    }

    override suspend fun createCustomer(
        email: String,
        name: String,
        metadata: Map<String, String>
    ): String {
        return try {
            primaryGateway.createCustomer(email, name, metadata)
        } catch (e: Exception) {
            logger.warn("Primary gateway failed, trying fallback", e)
            fallbackGateway?.createCustomer(email, name, metadata) ?: throw e
        }
    }

    override suspend fun verifyWebhookSignature(payload: String, signature: String): Boolean {
        // Try both gateways
        return try {
            primaryGateway.verifyWebhookSignature(payload, signature)
        } catch (e: Exception) {
            fallbackGateway?.verifyWebhookSignature(payload, signature) ?: false
        }
    }

    override suspend fun parseWebhookEvent(payload: String): WebhookEvent {
        return try {
            primaryGateway.parseWebhookEvent(payload)
        } catch (e: Exception) {
            logger.warn("Primary gateway failed, trying fallback", e)
            fallbackGateway?.parseWebhookEvent(payload) ?: throw e
        }
    }
}
