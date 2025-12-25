package ai.dokus.foundation.domain.ids

import ai.dokus.foundation.domain.Validatable
import ai.dokus.foundation.domain.ValueClass
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.usecases.validators.ValidatePostalCodeUseCase
import kotlinx.serialization.Serializable
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
