package ai.dokus.foundation.domain.ids

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
