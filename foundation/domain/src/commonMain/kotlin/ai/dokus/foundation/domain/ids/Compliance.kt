package ai.dokus.foundation.domain.ids

import ai.dokus.foundation.domain.Validatable
import ai.dokus.foundation.domain.ValueClass
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.usecases.validators.ValidatePeppolIdUseCase
import ai.dokus.foundation.domain.usecases.validators.ValidateVatNumberUseCase
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class VatReturnId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): VatReturnId = VatReturnId(Uuid.random())
        fun parse(value: String): VatReturnId = VatReturnId(Uuid.parse(value))
    }
}

@Serializable
@JvmInline
value class PeppolId(override val value: String) : ValueClass<String>, Validatable<PeppolId> {
    override fun toString(): String = value

    override val isValid: Boolean
        get() = ValidatePeppolIdUseCase(this)

    override val validOrThrows: PeppolId
        get() = if (isValid) this else throw DokusException.Validation.InvalidPeppolId
}

@Serializable
@JvmInline
value class VatNumber(override val value: String) : ValueClass<String>, Validatable<VatNumber> {
    override fun toString(): String = value

    val normalized: String
        get() = value
            .replace(".", "")
            .replace(" ", "")
            .uppercase()

    override val isValid: Boolean
        get() = ValidateVatNumberUseCase(this)

    override val validOrThrows: VatNumber
        get() = if (isValid) this else throw DokusException.Validation.InvalidVatNumber
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class PeppolTransmissionId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): PeppolTransmissionId = PeppolTransmissionId(Uuid.random())
        fun parse(value: String): PeppolTransmissionId = PeppolTransmissionId(Uuid.parse(value))
    }
}

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class PeppolSettingsId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): PeppolSettingsId = PeppolSettingsId(Uuid.random())
        fun parse(value: String): PeppolSettingsId = PeppolSettingsId(Uuid.parse(value))
    }
}
