package tech.dokus.domain.ids

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Strongly typed ID for document processing records.
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class DocumentProcessingId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): DocumentProcessingId = DocumentProcessingId(Uuid.random())
        fun parse(value: String): DocumentProcessingId = DocumentProcessingId(Uuid.parse(value))
    }
}
