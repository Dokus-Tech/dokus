package tech.dokus.domain.ids

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class DocumentId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): DocumentId = DocumentId(Uuid.random())
        fun parse(value: String): DocumentId = DocumentId(Uuid.parse(value))
    }
}
