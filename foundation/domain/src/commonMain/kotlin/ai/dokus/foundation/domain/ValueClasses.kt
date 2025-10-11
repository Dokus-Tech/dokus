package ai.dokus.foundation.domain

import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.usecases.validators.ValidateEmailUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidateNameUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidatePasswordUseCase
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface ValueClass<T> {
    val value: T
}

interface Validatable<ValueClassType> where ValueClassType : ValueClass<*> {
    val isValid: Boolean
    val validOrThrows: ValueClassType
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
value class Password(override val value: String) : ValueClass<String>, Validatable<Password> {
    override fun toString(): String = value
    override val isValid get() = ValidatePasswordUseCase(this)

    override val validOrThrows: Password
        get() = if (isValid) this else throw DokusException.WeakPassword
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

@Serializable
@JvmInline
value class Email(override val value: String) : ValueClass<String>, Validatable<Email> {
    override fun toString(): String = value

    override val isValid get() = ValidateEmailUseCase(this)

    override val validOrThrows: Email
        get() = if (isValid) this else throw DokusException.InvalidEmail
}

@Serializable
@JvmInline
value class Name(override val value: String) : ValueClass<String>, Validatable<Name> {
    override fun toString(): String = value

    val initialOrEmpty: String
        get() = value.firstOrNull()?.toString() ?: ""

    override val isValid get() = ValidateNameUseCase(this)

    override val validOrThrows: Name
        get() = if (isValid) this else throw DokusException.InvalidFirstName
}