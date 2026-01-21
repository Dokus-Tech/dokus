package tech.dokus.domain.ids

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Unique identifier for a document example used in few-shot learning.
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class ExampleId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): ExampleId = ExampleId(Uuid.random())
        fun parse(value: String): ExampleId = ExampleId(Uuid.parse(value))
    }
}
