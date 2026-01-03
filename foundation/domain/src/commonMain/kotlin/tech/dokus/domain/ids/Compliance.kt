package tech.dokus.domain.ids

import kotlinx.serialization.Serializable
import tech.dokus.domain.Validatable
import tech.dokus.domain.ValueClass
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.validators.ValidatePeppolIdUseCase
import tech.dokus.domain.validators.ValidateVatNumberUseCase
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
        get() = normalize(value)

    val countryCode: String?
        get() = normalized.takeIf { it.length >= 2 && it.substring(0, 2).all { ch -> ch.isLetter() } }
            ?.substring(0, 2)

    val companyNumber: String
        get() = normalized.let { normalizedValue ->
            val code = countryCode
            if (code == null || normalizedValue.length <= 2) normalizedValue else normalizedValue.substring(2)
        }

    val formatted: String
        get() = format(includeCountry = true)

    val formattedCompanyNumber: String
        get() = format(includeCountry = false)

    val isBelgian: Boolean
        get() = countryCode == "BE"

    override val isValid: Boolean
        get() = ValidateVatNumberUseCase(this)

    override val validOrThrows: VatNumber
        get() = if (isValid) this else throw DokusException.Validation.InvalidVatNumber

    companion object {
        val Empty = VatNumber("")

        fun normalize(raw: String): String = raw
            .trim()
            .uppercase()
            .replace(Regex("[^A-Z0-9]"), "")

        fun fromCountryAndCompanyNumber(countryCode: String, companyNumber: String): VatNumber {
            val normalizedCountry = countryCode.trim().uppercase()
            val normalizedCompany = normalize(companyNumber)
            return VatNumber("$normalizedCountry$normalizedCompany")
        }
    }

    private fun format(includeCountry: Boolean): String {
        val country = countryCode
        val formattedCompany = when (country) {
            "BE" -> formatBelgianCompanyNumber(companyNumber)
            else -> companyNumber
        }
        return if (includeCountry && country != null) {
            "$country$formattedCompany"
        } else {
            formattedCompany
        }
    }

    private fun formatBelgianCompanyNumber(number: String): String {
        val digits = number.filter { it.isDigit() }
        if (digits.length != 10) return number
        return "${digits.substring(0, 4)}.${digits.substring(4, 7)}.${digits.substring(7, 10)}"
    }
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
