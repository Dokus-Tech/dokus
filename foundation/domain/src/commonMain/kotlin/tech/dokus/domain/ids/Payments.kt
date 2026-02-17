package tech.dokus.domain.ids

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@Serializable
@JvmInline
value class ExpenseId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): ExpenseId = ExpenseId(Uuid.random())
        fun parse(value: String): ExpenseId = ExpenseId(Uuid.parse(value))
    }
}

@Serializable
@JvmInline
value class PaymentId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): PaymentId = PaymentId(Uuid.random())
        fun parse(value: String): PaymentId = PaymentId(Uuid.parse(value))
    }
}

@Serializable
@JvmInline
value class CashflowEntryId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): CashflowEntryId = CashflowEntryId(Uuid.random())
        fun parse(value: String): CashflowEntryId = CashflowEntryId(Uuid.parse(value))
    }
}
