package ai.dokus.foundation.domain.ids

import ai.dokus.foundation.domain.Validatable
import ai.dokus.foundation.domain.ValueClass
import ai.dokus.foundation.domain.exceptions.DokusException
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
        get() = if (isValid) this else throw DokusException.Validation.InvalidInvoiceNumber()
}
