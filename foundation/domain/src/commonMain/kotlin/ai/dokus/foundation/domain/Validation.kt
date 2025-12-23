package ai.dokus.foundation.domain

import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.usecases.validators.ValidateEmailUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidateLegalNameUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidateNameUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidatePasswordUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidatePhoneNumberUseCase
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

interface ValueClass<T> {
    val value: T
}

interface Validatable<ValueClassType> where ValueClassType : ValueClass<*> {
    val isValid: Boolean
    val validOrThrows: ValueClassType
}

@Serializable
@JvmInline
value class Password(override val value: String) : ValueClass<String>, Validatable<Password> {
    override fun toString(): String = value
    override val isValid get() = ValidatePasswordUseCase(this)

    override val validOrThrows: Password
        get() = if (isValid) this else throw DokusException.Validation.WeakPassword
}

@Serializable
@JvmInline
value class Email(override val value: String) : ValueClass<String>, Validatable<Email> {
    override fun toString(): String = value

    override val isValid get() = ValidateEmailUseCase(this)

    override val validOrThrows: Email
        get() = if (isValid) this else throw DokusException.Validation.InvalidEmail
}

@Serializable
@JvmInline
value class Name(override val value: String) : ValueClass<String>, Validatable<Name> {
    override fun toString(): String = value

    val initialOrEmpty: String
        get() = value.firstOrNull()?.toString() ?: ""

    override val isValid get() = ValidateNameUseCase(this)

    override val validOrThrows: Name
        get() = if (isValid) this else throw DokusException.Validation.InvalidFirstName
}

@Serializable
@JvmInline
value class LegalName(override val value: String) : ValueClass<String>, Validatable<LegalName> {
    override fun toString(): String = value

    val initialOrEmpty: String
        get() = value.firstOrNull()?.toString() ?: ""

    override val isValid get() = ValidateLegalNameUseCase(this)

    override val validOrThrows: LegalName
        get() = if (isValid) this else throw DokusException.Validation.InvalidLegalName
}

@Serializable
@JvmInline
value class DisplayName(override val value: String) : ValueClass<String>, Validatable<DisplayName> {
    override fun toString(): String = value

    val initialOrEmpty: String
        get() = value.firstOrNull()?.toString() ?: ""

    override val isValid get() = value.isNotBlank() && value.length <= 255

    override val validOrThrows: DisplayName
        get() = if (isValid) this else throw DokusException.Validation.InvalidDisplayName
}

@Serializable
@JvmInline
value class PhoneNumber(override val value: String) : ValueClass<String>, Validatable<PhoneNumber> {
    override fun toString(): String = value

    override val isValid get() = ValidatePhoneNumberUseCase(this)

    override val validOrThrows: PhoneNumber
        get() = if (isValid) this else throw DokusException.Validation.InvalidPhoneNumber
}
