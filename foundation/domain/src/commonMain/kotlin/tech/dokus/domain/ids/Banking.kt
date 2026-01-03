package tech.dokus.domain.ids

import kotlinx.serialization.Serializable
import tech.dokus.domain.Validatable
import tech.dokus.domain.ValueClass
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.validators.ValidateBicUseCase
import tech.dokus.domain.validators.ValidateIbanUseCase
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
value class Iban(override val value: String) : ValueClass<String>, Validatable<Iban> {
    override fun toString(): String = value

    override val isValid: Boolean
        get() = ValidateIbanUseCase(this)

    override val validOrThrows: Iban
        get() = if (isValid) this else throw DokusException.Validation.InvalidIban
}

@Serializable
@JvmInline
value class Bic(override val value: String) : ValueClass<String>, Validatable<Bic> {
    override fun toString(): String = value

    override val isValid: Boolean
        get() = ValidateBicUseCase(this)

    override val validOrThrows: Bic
        get() = if (isValid) this else throw DokusException.Validation.InvalidBic
}
