package tech.dokus.domain.ids

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class DocumentBlobId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): DocumentBlobId = DocumentBlobId(Uuid.random())
        fun parse(value: String): DocumentBlobId = DocumentBlobId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class DocumentSourceId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): DocumentSourceId = DocumentSourceId(Uuid.random())
        fun parse(value: String): DocumentSourceId = DocumentSourceId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class DocumentMatchReviewId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): DocumentMatchReviewId = DocumentMatchReviewId(Uuid.random())
        fun parse(value: String): DocumentMatchReviewId = DocumentMatchReviewId(Uuid.parse(value))
    }
}
