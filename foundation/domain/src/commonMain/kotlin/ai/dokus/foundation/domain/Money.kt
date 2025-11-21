package ai.dokus.foundation.domain

import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.usecases.validators.ValidateMoneyUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidatePercentageUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidateQuantityUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidateVatRateUseCase
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class Money(override val value: String) : ValueClass<String>, Validatable<Money> {
    override fun toString(): String = value

    override val isValid: Boolean
        get() = ValidateMoneyUseCase(this)

    override val validOrThrows: Money
        get() = if (isValid) this else throw DokusException.Validation.InvalidMoney

    companion object {
        val ZERO = Money("0.00")

        fun parse(value: String): Money = Money(value)

        fun fromDouble(value: Double): Money {
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
        get() = ValidateVatRateUseCase(this)

    override val validOrThrows: VatRate
        get() = if (isValid) this else throw DokusException.Validation.InvalidVatRate

    companion object {
        val ZERO = VatRate("0.00")
        val STANDARD_BE = VatRate("21.00")  // Belgium standard rate
        val REDUCED_BE = VatRate("6.00")     // Belgium reduced rate

        fun parse(value: String): VatRate = VatRate(value)
    }
}

@Serializable
@JvmInline
value class Percentage(override val value: String) : ValueClass<String>, Validatable<Percentage> {
    override fun toString(): String = value

    override val isValid: Boolean
        get() = ValidatePercentageUseCase(this)

    override val validOrThrows: Percentage
        get() = if (isValid) this else throw DokusException.Validation.InvalidPercentage

    companion object {
        val ZERO = Percentage("0.00")
        val FULL = Percentage("100.00")

        fun parse(value: String): Percentage = Percentage(value)
    }
}

@Serializable
@JvmInline
value class Quantity(override val value: String) : ValueClass<String>, Validatable<Quantity> {
    override fun toString(): String = value

    override val isValid: Boolean
        get() = ValidateQuantityUseCase(this)

    override val validOrThrows: Quantity
        get() = if (isValid) this else throw DokusException.Validation.InvalidQuantity

    companion object {
        val ONE = Quantity("1")

        fun parse(value: String): Quantity = Quantity(value)
        fun fromDouble(value: Double): Quantity = Quantity(value.toString())
        fun fromInt(value: Int): Quantity = Quantity(value.toString())
    }
}
