package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.ids.PaymentId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


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