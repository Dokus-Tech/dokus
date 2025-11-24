package ai.dokus.foundation.domain.ids

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class MediaId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): MediaId = MediaId(Uuid.random())
        fun parse(value: String): MediaId = MediaId(Uuid.parse(value))
    }
}
