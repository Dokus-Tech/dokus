package tech.dokus.domain.ids

import kotlinx.serialization.Serializable
import tech.dokus.domain.Validatable
import tech.dokus.domain.ValueClass
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.validators.ValidateInvoiceNumberUseCase
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
value class CreditNoteId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): CreditNoteId = CreditNoteId(Uuid.random())
        fun parse(value: String): CreditNoteId = CreditNoteId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class RefundClaimId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): RefundClaimId = RefundClaimId(Uuid.random())
        fun parse(value: String): RefundClaimId = RefundClaimId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class DocumentLineItemId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): DocumentLineItemId = DocumentLineItemId(Uuid.random())
        fun parse(value: String): DocumentLineItemId = DocumentLineItemId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class DocumentLinkId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): DocumentLinkId = DocumentLinkId(Uuid.random())
        fun parse(value: String): DocumentLinkId = DocumentLinkId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class QuoteId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): QuoteId = QuoteId(Uuid.random())
        fun parse(value: String): QuoteId = QuoteId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class ProFormaId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): ProFormaId = ProFormaId(Uuid.random())
        fun parse(value: String): ProFormaId = ProFormaId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class PurchaseOrderId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): PurchaseOrderId = PurchaseOrderId(Uuid.random())
        fun parse(value: String): PurchaseOrderId = PurchaseOrderId(Uuid.parse(value))
    }
}
