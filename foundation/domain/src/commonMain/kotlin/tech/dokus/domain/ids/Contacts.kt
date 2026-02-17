package tech.dokus.domain.ids

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@Serializable
@JvmInline
value class ContactId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): ContactId = ContactId(Uuid.random())
        fun parse(value: String): ContactId = ContactId(Uuid.parse(value))
    }
}

@Serializable
@JvmInline
value class ContactNoteId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): ContactNoteId = ContactNoteId(Uuid.random())
        fun parse(value: String): ContactNoteId = ContactNoteId(Uuid.parse(value))
    }
}

@Serializable
@JvmInline
value class ContactAddressId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): ContactAddressId = ContactAddressId(Uuid.random())
        fun parse(value: String): ContactAddressId = ContactAddressId(Uuid.parse(value))
    }
}
