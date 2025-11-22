package ai.dokus.foundation.domain.ids

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class TenantId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): TenantId = TenantId(Uuid.random())
        fun parse(value: String): TenantId = TenantId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class OrganizationId(val value: Uuid) {
    constructor(value: String) : this(Uuid.parse(value))

    override fun toString(): String = value.toString()

    companion object {
        fun generate(): OrganizationId = OrganizationId(Uuid.random())
        fun parse(value: String): OrganizationId = OrganizationId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class BusinessUserId(val value: Uuid) {
    constructor(value: String) : this(Uuid.parse(value))

    override fun toString(): String = value.toString()

    companion object {
        fun generate(): BusinessUserId = BusinessUserId(Uuid.random())
        fun parse(value: String): BusinessUserId = BusinessUserId(Uuid.parse(value))
    }
}

@Serializable
@JvmInline
value class UserId(val value: String) {
    override fun toString(): String = value

    @OptIn(ExperimentalUuidApi::class)
    val uuid: Uuid get() = Uuid.parse(value)
}

@Serializable
@JvmInline
value class SessionId(val value: String) {
    override fun toString(): String = value

    @OptIn(ExperimentalUuidApi::class)
    val uuid: Uuid get() = Uuid.parse(value)

    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun generate(): SessionId = SessionId(Uuid.random().toString())
    }
}
