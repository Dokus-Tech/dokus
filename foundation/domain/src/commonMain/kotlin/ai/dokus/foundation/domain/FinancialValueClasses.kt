package ai.dokus.foundation.domain

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// ============================================================================
// TENANT & USER IDS
// ============================================================================

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class TenantId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): TenantId = TenantId(Uuid.random())
        fun parse(value: String): TenantId = TenantId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class BusinessUserId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): BusinessUserId = BusinessUserId(Uuid.random())
        fun parse(value: String): BusinessUserId = BusinessUserId(Uuid.parse(value))
    }
}

// ============================================================================
// INVOICE & CLIENT IDS
// ============================================================================

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class ClientId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): ClientId = ClientId(Uuid.random())
        fun parse(value: String): ClientId = ClientId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class InvoiceId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): InvoiceId = InvoiceId(Uuid.random())
        fun parse(value: String): InvoiceId = InvoiceId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class InvoiceItemId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): InvoiceItemId = InvoiceItemId(Uuid.random())
        fun parse(value: String): InvoiceItemId = InvoiceItemId(Uuid.parse(value))
    }
}

@Serializable
@JvmInline
value class InvoiceNumber(val value: String) {
    override fun toString(): String = value

    init {
        require(value.isNotBlank()) { "Invoice number cannot be blank" }
    }
}

// ============================================================================
// EXPENSE & PAYMENT IDS
// ============================================================================

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class ExpenseId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): ExpenseId = ExpenseId(Uuid.random())
        fun parse(value: String): ExpenseId = ExpenseId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class PaymentId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): PaymentId = PaymentId(Uuid.random())
        fun parse(value: String): PaymentId = PaymentId(Uuid.parse(value))
    }
}

// ============================================================================
// BANKING IDS
// ============================================================================

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class BankConnectionId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): BankConnectionId = BankConnectionId(Uuid.random())
        fun parse(value: String): BankConnectionId = BankConnectionId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class BankTransactionId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): BankTransactionId = BankTransactionId(Uuid.random())
        fun parse(value: String): BankTransactionId = BankTransactionId(Uuid.parse(value))
    }
}

// ============================================================================
// VAT & AUDIT IDS
// ============================================================================

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class VatReturnId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): VatReturnId = VatReturnId(Uuid.random())
        fun parse(value: String): VatReturnId = VatReturnId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class AuditLogId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): AuditLogId = AuditLogId(Uuid.random())
        fun parse(value: String): AuditLogId = AuditLogId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class AttachmentId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): AttachmentId = AttachmentId(Uuid.random())
        fun parse(value: String): AttachmentId = AttachmentId(Uuid.parse(value))
    }
}

// ============================================================================
// MONETARY VALUES
// ============================================================================

@Serializable
@JvmInline
value class Money(val value: String) {
    override fun toString(): String = value

    init {
        require(value.matches(Regex("^-?\\d+(\\.\\d{1,2})?$"))) {
            "Invalid money format: $value. Expected format: 123.45"
        }
    }

    companion object {
        val ZERO = Money("0.00")

        fun parse(value: String): Money = Money(value)
        fun fromDouble(value: Double): Money {
            // Format to 2 decimal places
            val formatted = ((value * 100).toLong() / 100.0).toString()
            return if (formatted.contains('.')) {
                val parts = formatted.split('.')
                val decimals = if (parts[1].length == 1) "${parts[1]}0" else parts[1].take(2)
                Money("${parts[0]}.$decimals")
            } else {
                Money("$formatted.00")
            }
        }
        fun fromInt(value: Int): Money = Money("$value.00")
    }
}

@Serializable
@JvmInline
value class VatRate(val value: String) {
    override fun toString(): String = value

    init {
        require(value.matches(Regex("^\\d+(\\.\\d{1,2})?$"))) {
            "Invalid VAT rate format: $value. Expected format: 21.00"
        }
        val rate = value.toDouble()
        require(rate in 0.0..100.0) {
            "VAT rate must be between 0 and 100: $value"
        }
    }

    companion object {
        val ZERO = VatRate("0.00")
        val STANDARD_BE = VatRate("21.00")
        val REDUCED_BE = VatRate("6.00")

        fun parse(value: String): VatRate = VatRate(value)
    }
}

@Serializable
@JvmInline
value class Percentage(val value: String) {
    override fun toString(): String = value

    init {
        require(value.matches(Regex("^\\d+(\\.\\d{1,2})?$"))) {
            "Invalid percentage format: $value. Expected format: 100.00"
        }
        val percent = value.toDouble()
        require(percent in 0.0..100.0) {
            "Percentage must be between 0 and 100: $value"
        }
    }

    companion object {
        val ZERO = Percentage("0.00")
        val FULL = Percentage("100.00")

        fun parse(value: String): Percentage = Percentage(value)
    }
}

// ============================================================================
// OTHER VALUE CLASSES
// ============================================================================

@Serializable
@JvmInline
value class VatNumber(val value: String) {
    override fun toString(): String = value

    init {
        require(value.isNotBlank()) { "VAT number cannot be blank" }
        // Basic VAT number validation - can be enhanced per country
        require(value.matches(Regex("^[A-Z]{2}[A-Z0-9]+$"))) {
            "Invalid VAT number format: $value"
        }
    }
}

@Serializable
@JvmInline
value class Iban(val value: String) {
    override fun toString(): String = value

    init {
        require(value.isNotBlank()) { "IBAN cannot be blank" }
        // Basic IBAN validation
        val cleanIban = value.replace(" ", "").uppercase()
        require(cleanIban.matches(Regex("^[A-Z]{2}\\d{2}[A-Z0-9]+$"))) {
            "Invalid IBAN format: $value"
        }
        require(cleanIban.length in 15..34) {
            "IBAN length must be between 15 and 34 characters: $value"
        }
    }
}

@Serializable
@JvmInline
value class Bic(val value: String) {
    override fun toString(): String = value

    init {
        require(value.isNotBlank()) { "BIC cannot be blank" }
        // BIC/SWIFT code validation
        require(value.matches(Regex("^[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?$"))) {
            "Invalid BIC/SWIFT code format: $value"
        }
    }
}

@Serializable
@JvmInline
value class TransactionId(val value: String) {
    override fun toString(): String = value

    init {
        require(value.isNotBlank()) { "Transaction ID cannot be blank" }
    }
}

@Serializable
@JvmInline
value class PeppolId(val value: String) {
    override fun toString(): String = value

    init {
        require(value.isNotBlank()) { "PEPPOL ID cannot be blank" }
    }
}

@Serializable
@JvmInline
value class Quantity(val value: String) {
    override fun toString(): String = value

    init {
        require(value.matches(Regex("^\\d+(\\.\\d+)?$"))) {
            "Invalid quantity format: $value"
        }
        require(value.toDouble() > 0) {
            "Quantity must be positive: $value"
        }
    }

    companion object {
        val ONE = Quantity("1")

        fun parse(value: String): Quantity = Quantity(value)
        fun fromDouble(value: Double): Quantity = Quantity(value.toString())
        fun fromInt(value: Int): Quantity = Quantity(value.toString())
    }
}