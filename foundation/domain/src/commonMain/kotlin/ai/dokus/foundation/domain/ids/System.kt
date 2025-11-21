package ai.dokus.foundation.domain.ids

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class AuditLogId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): AuditLogId = AuditLogId(Uuid.random())
        fun parse(value: String): AuditLogId = AuditLogId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class AttachmentId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): AttachmentId = AttachmentId(Uuid.random())
        fun parse(value: String): AttachmentId = AttachmentId(Uuid.parse(value))
    }
}
