package ai.dokus.foundation.domain.ids

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class ClientId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): ClientId = ClientId(Uuid.random())
        fun parse(value: String): ClientId = ClientId(Uuid.parse(value))
    }
}
