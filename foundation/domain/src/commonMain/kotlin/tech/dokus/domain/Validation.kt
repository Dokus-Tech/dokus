package tech.dokus.domain

import kotlinx.serialization.Serializable
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.validators.ValidateCityUseCase
import tech.dokus.domain.validators.ValidateEmailUseCase
import tech.dokus.domain.validators.ValidateLegalNameUseCase
import tech.dokus.domain.validators.ValidateNameUseCase
import tech.dokus.domain.validators.ValidatePasswordUseCase
import tech.dokus.domain.validators.ValidatePhoneNumberUseCase
import kotlin.jvm.JvmInline

private const val DISPLAY_NAME_MAX_LENGTH = 255

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
    override val isValid
        get() = ValidatePasswordUseCase(
            this
        )

    override val validOrThrows: Password
        get() = if (isValid) this else throw DokusException.Validation.WeakPassword
}

@Serializable
@JvmInline
value class Email(override val value: String) : ValueClass<String>, Validatable<Email> {
    override fun toString(): String = value

    override val isValid
        get() = ValidateEmailUseCase(this)

    override val validOrThrows: Email
        get() = if (isValid) this else throw DokusException.Validation.InvalidEmail

    companion object {
        val Empty = Email("")

        /**
         * Normalize raw email input into canonical form.
         *
         * Rules:
         * - Trim whitespace
         * - Lowercase for consistent storage/display
         * - Keep cleaned value even if invalid
         */
        fun from(raw: String?): Email? {
            if (raw == null) return null
            val cleaned = raw.trim().lowercase()
            return Email(cleaned)
        }
    }
}

@Serializable
@JvmInline
value class Name(override val value: String) : ValueClass<String>, Validatable<Name> {
    override fun toString(): String = value

    val initialOrEmpty: String
        get() = value.firstOrNull()?.toString() ?: ""

    override val isValid
        get() = ValidateNameUseCase(this)

    override val validOrThrows: Name
        get() = if (isValid) this else throw DokusException.Validation.InvalidFirstName

    companion object {
        val Empty = Name("")
    }
}

@Serializable
@JvmInline
value class LegalName(override val value: String) : ValueClass<String>, Validatable<LegalName> {
    override fun toString(): String = value

    val initialOrEmpty: String
        get() = value.firstOrNull()?.toString() ?: ""

    override val isValid
        get() = ValidateLegalNameUseCase(this)

    override val validOrThrows: LegalName
        get() = if (isValid) this else throw DokusException.Validation.InvalidLegalName

    companion object {
        val Empty = LegalName("")
    }
}

@Serializable
@JvmInline
value class DisplayName(override val value: String) : ValueClass<String>, Validatable<DisplayName> {
    override fun toString(): String = value

    val initialOrEmpty: String
        get() = value.firstOrNull()?.toString() ?: ""

    override val isValid get() = value.isNotBlank() && value.length <= DISPLAY_NAME_MAX_LENGTH

    override val validOrThrows: DisplayName
        get() = if (isValid) this else throw DokusException.Validation.InvalidDisplayName
}

@Serializable
@JvmInline
value class PhoneNumber(override val value: String) : ValueClass<String>, Validatable<PhoneNumber> {
    override fun toString(): String = value

    override val isValid
        get() = ValidatePhoneNumberUseCase(this)

    override val validOrThrows: PhoneNumber
        get() = if (isValid) this else throw DokusException.Validation.InvalidPhoneNumber

    companion object {
        val Empty = PhoneNumber("")
    }
}

@Serializable
@JvmInline
value class City(override val value: String) : ValueClass<String>, Validatable<City> {
    override fun toString(): String = value

    override val isValid
        get() = ValidateCityUseCase(
            this
        )

    override val validOrThrows: City
        get() = if (isValid) this else throw DokusException.Validation.InvalidCity
}
