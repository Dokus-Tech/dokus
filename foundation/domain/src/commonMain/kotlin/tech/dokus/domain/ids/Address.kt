package tech.dokus.domain.ids

import kotlinx.serialization.Serializable
import tech.dokus.domain.Validatable
import tech.dokus.domain.ValueClass
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.validators.ValidatePostalCodeUseCase
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class PostalCode(override val value: String) : ValueClass<String>, Validatable<PostalCode> {
    override fun toString(): String = value

    override val isValid: Boolean
        get() = ValidatePostalCodeUseCase(this)

    override val validOrThrows: PostalCode
        get() = if (isValid) this else throw DokusException.Validation.InvalidPostalCode
}
