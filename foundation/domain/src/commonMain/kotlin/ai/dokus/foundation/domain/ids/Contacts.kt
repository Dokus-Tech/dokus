package ai.dokus.foundation.domain.ids

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class ContactId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): ContactId = ContactId(Uuid.random())
        fun parse(value: String): ContactId = ContactId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class ContactNoteId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): ContactNoteId = ContactNoteId(Uuid.random())
        fun parse(value: String): ContactNoteId = ContactNoteId(Uuid.parse(value))
    }
}
