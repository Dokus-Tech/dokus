package tech.dokus.domain.ids

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.Uuid

@Serializable
@JvmInline
value class TenantId(val value: Uuid) {
    constructor(value: String) : this(Uuid.parse(value))

    override fun toString(): String = value.toString()

    companion object {
        fun generate(): TenantId = TenantId(Uuid.random())
        fun parse(value: String): TenantId = TenantId(Uuid.parse(value))
    }
}

@Serializable
@JvmInline
value class AddressId(val value: Uuid) {
    constructor(value: String) : this(Uuid.parse(value))

    override fun toString(): String = value.toString()

    companion object {
        fun generate(): AddressId = AddressId(Uuid.random())
        fun parse(value: String): AddressId = AddressId(Uuid.parse(value))
    }
}

@Serializable
@JvmInline
value class UserId(val value: Uuid) {
    constructor(value: String) : this(Uuid.parse(value))

    override fun toString(): String = value.toString()

    val uuid: Uuid get() = value

    companion object {
        fun generate(): UserId = UserId(Uuid.random())
        fun parse(value: String): UserId = UserId(Uuid.parse(value))
    }
}

@Serializable
@JvmInline
value class SessionId(val value: Uuid) {
    constructor(value: String) : this(Uuid.parse(value))

    override fun toString(): String = value.toString()

    val uuid: Uuid get() = value

    companion object {
        fun generate(): SessionId = SessionId(Uuid.random())
    }
}

@Serializable
@JvmInline
value class InvitationId(val value: Uuid) {
    constructor(value: String) : this(Uuid.parse(value))

    override fun toString(): String = value.toString()

    companion object {
        fun generate(): InvitationId = InvitationId(Uuid.random())
        fun parse(value: String): InvitationId = InvitationId(Uuid.parse(value))
    }
}
