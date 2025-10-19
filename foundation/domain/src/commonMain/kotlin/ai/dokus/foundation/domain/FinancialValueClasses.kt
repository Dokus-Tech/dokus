package ai.dokus.foundation.domain

import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.usecases.validators.ValidateBicUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidateIbanUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidateInvoiceNumberUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidateMoneyUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidatePeppolIdUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidatePercentageUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidateQuantityUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidateVatNumberUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidateVatRateUseCase
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
value class InvoiceNumber(override val value: String) : ValueClass<String>, Validatable<InvoiceNumber> {
    override fun toString(): String = value

    override val isValid: Boolean
        get() = ai.dokus.foundation.domain.usecases.validators.ValidateInvoiceNumberUseCase(this)

    override val validOrThrows: InvoiceNumber
        get() = if (isValid) this else throw DokusException.InvalidInvoiceNumber
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
value class Money(override val value: String) : ValueClass<String>, Validatable<Money> {
    override fun toString(): String = value

    override val isValid: Boolean
        get() = ai.dokus.foundation.domain.usecases.validators.ValidateMoneyUseCase(this)

    override val validOrThrows: Money
        get() = if (isValid) this else throw DokusException.InvalidMoney

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
value class VatRate(override val value: String) : ValueClass<String>, Validatable<VatRate> {
    override fun toString(): String = value

    override val isValid: Boolean
        get() = ai.dokus.foundation.domain.usecases.validators.ValidateVatRateUseCase(this)

    override val validOrThrows: VatRate
        get() = if (isValid) this else throw DokusException.InvalidVatRate

    companion object {
        val ZERO = VatRate("0.00")
        val STANDARD_BE = VatRate("21.00")
        val REDUCED_BE = VatRate("6.00")

        fun parse(value: String): VatRate = VatRate(value)
    }
}

@Serializable
@JvmInline
value class Percentage(override val value: String) : ValueClass<String>, Validatable<Percentage> {
    override fun toString(): String = value

    override val isValid: Boolean
        get() = ai.dokus.foundation.domain.usecases.validators.ValidatePercentageUseCase(this)

    override val validOrThrows: Percentage
        get() = if (isValid) this else throw DokusException.InvalidPercentage

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
value class VatNumber(override val value: String) : ValueClass<String>, Validatable<VatNumber> {
    override fun toString(): String = value

    override val isValid: Boolean
        get() = ValidateVatNumberUseCase(this)

    override val validOrThrows: VatNumber
        get() = if (isValid) this else throw DokusException.InvalidVatNumber
}

@Serializable
@JvmInline
value class Iban(override val value: String) : ValueClass<String>, Validatable<Iban> {
    override fun toString(): String = value

    override val isValid: Boolean
        get() = ValidateIbanUseCase(this)

    override val validOrThrows: Iban
        get() = if (isValid) this else throw DokusException.InvalidIban
}

@Serializable
@JvmInline
value class Bic(override val value: String) : ValueClass<String>, Validatable<Bic> {
    override fun toString(): String = value

    override val isValid: Boolean
        get() = ValidateBicUseCase(this)

    override val validOrThrows: Bic
        get() = if (isValid) this else throw DokusException.InvalidBic
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
value class PeppolId(override val value: String) : ValueClass<String>, Validatable<PeppolId> {
    override fun toString(): String = value

    override val isValid: Boolean
        get() = ValidatePeppolIdUseCase(this)

    override val validOrThrows: PeppolId
        get() = if (isValid) this else throw DokusException.InvalidPeppolId
}

@Serializable
@JvmInline
value class Quantity(override val value: String) : ValueClass<String>, Validatable<Quantity> {
    override fun toString(): String = value

    override val isValid: Boolean
        get() = ai.dokus.foundation.domain.usecases.validators.ValidateQuantityUseCase(this)

    override val validOrThrows: Quantity
        get() = if (isValid) this else throw DokusException.InvalidQuantity

    companion object {
        val ONE = Quantity("1")

        fun parse(value: String): Quantity = Quantity(value)
        fun fromDouble(value: Double): Quantity = Quantity(value.toString())
        fun fromInt(value: Int): Quantity = Quantity(value.toString())
    }
}