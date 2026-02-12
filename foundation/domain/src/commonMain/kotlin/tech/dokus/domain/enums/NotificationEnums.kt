package tech.dokus.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.database.DbEnum

@Serializable
enum class NotificationCategory {
    @SerialName("PEPPOL")
    Peppol,

    @SerialName("COMPLIANCE")
    Compliance,

    @SerialName("BILLING")
    Billing
}

@Serializable
enum class NotificationType(override val dbValue: String) : DbEnum {
    @SerialName("PEPPOL_RECEIVED")
    PeppolReceived("PEPPOL_RECEIVED"),

    @SerialName("PEPPOL_SEND_CONFIRMED")
    PeppolSendConfirmed("PEPPOL_SEND_CONFIRMED"),

    @SerialName("PEPPOL_SEND_FAILED")
    PeppolSendFailed("PEPPOL_SEND_FAILED"),

    @SerialName("COMPLIANCE_BLOCKER")
    ComplianceBlocker("COMPLIANCE_BLOCKER"),

    @SerialName("VAT_WARNING")
    VatWarning("VAT_WARNING"),

    @SerialName("PAYMENT_CONFIRMED")
    PaymentConfirmed("PAYMENT_CONFIRMED"),

    @SerialName("PAYMENT_FAILED")
    PaymentFailed("PAYMENT_FAILED"),

    @SerialName("SUBSCRIPTION_CHANGED")
    SubscriptionChanged("SUBSCRIPTION_CHANGED");

    val category: NotificationCategory
        get() = when (this) {
            PeppolReceived,
            PeppolSendConfirmed,
            PeppolSendFailed -> NotificationCategory.Peppol

            ComplianceBlocker,
            VatWarning -> NotificationCategory.Compliance

            PaymentConfirmed,
            PaymentFailed,
            SubscriptionChanged -> NotificationCategory.Billing
        }

    val defaultEmailEnabled: Boolean
        get() = when (this) {
            PeppolSendFailed,
            ComplianceBlocker,
            PaymentConfirmed,
            PaymentFailed,
            SubscriptionChanged -> true

            PeppolReceived,
            PeppolSendConfirmed,
            VatWarning -> false
        }

    val emailLocked: Boolean
        get() = when (this) {
            PeppolSendFailed,
            ComplianceBlocker,
            PaymentFailed -> true

            PeppolReceived,
            PeppolSendConfirmed,
            VatWarning,
            PaymentConfirmed,
            SubscriptionChanged -> false
        }
}

@Serializable
enum class NotificationReferenceType(override val dbValue: String) : DbEnum {
    @SerialName("DOCUMENT")
    Document("DOCUMENT"),

    @SerialName("INVOICE")
    Invoice("INVOICE"),

    @SerialName("TRANSMISSION")
    Transmission("TRANSMISSION"),

    @SerialName("COMPLIANCE_ITEM")
    ComplianceItem("COMPLIANCE_ITEM"),

    @SerialName("BILLING_ITEM")
    BillingItem("BILLING_ITEM")
}
