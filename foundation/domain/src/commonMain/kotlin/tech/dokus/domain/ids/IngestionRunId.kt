package tech.dokus.domain.ids

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Strongly typed ID for document ingestion run records.
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class IngestionRunId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): IngestionRunId = IngestionRunId(Uuid.random())
        fun parse(value: String): IngestionRunId = IngestionRunId(Uuid.parse(value))
    }
}
