package tech.dokus.domain.ids

import tech.dokus.domain.Validatable
import tech.dokus.domain.ValueClass
import tech.dokus.domain.exceptions.DokusException
import ai.dokus.foundation.domain.usecases.validators.ValidateInvoiceNumberUseCase
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
        get() = ValidateInvoiceNumberUseCase(this)

    override val validOrThrows: InvoiceNumber
        get() = if (isValid) this else throw DokusException.Validation.InvalidInvoiceNumber
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class BillId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): BillId = BillId(Uuid.random())
        fun parse(value: String): BillId = BillId(Uuid.parse(value))
    }
}
