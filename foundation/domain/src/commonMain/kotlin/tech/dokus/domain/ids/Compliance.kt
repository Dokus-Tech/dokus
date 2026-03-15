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

private const val VAT_COUNTRY_CODE_LENGTH = 2
private const val BELGIAN_COMPANY_NUMBER_LENGTH = 10
private const val BELGIAN_COMPANY_FIRST_GROUP_END = 4
private const val BELGIAN_COMPANY_SECOND_GROUP_END = 7

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
        get() = normalized.takeIf {
            it.length >= VAT_COUNTRY_CODE_LENGTH &&
                it.substring(0, VAT_COUNTRY_CODE_LENGTH).all { ch -> ch.isLetter() }
        }?.substring(0, VAT_COUNTRY_CODE_LENGTH)

    val companyNumber: String
        get() = normalized.let { normalizedValue ->
            val code = countryCode
            if (code == null || normalizedValue.length <= VAT_COUNTRY_CODE_LENGTH) {
                normalizedValue
            } else {
                normalizedValue.substring(VAT_COUNTRY_CODE_LENGTH)
            }
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

        /**
         * Normalize raw VAT input into canonical form.
         *
         * Rules:
         * - Strip spaces/dots/dashes and uppercase.
         * - If digits-only and length is 9 or 10, prepend BE (9-digit gets leading zero).
         * - Keep cleaned value even if invalid.
         */
        fun from(raw: String?): VatNumber? {
            if (raw == null) return null
            val cleaned = normalize(raw)
            if (cleaned.isEmpty()) return VatNumber(cleaned)

            val digitsOnly = cleaned.all { it.isDigit() }
            val normalized = if (digitsOnly) {
                when (cleaned.length) {
                    9 -> "BE0$cleaned"
                    10 -> "BE$cleaned"
                    else -> cleaned
                }
            } else {
                cleaned
            }

            return VatNumber(normalized)
        }

        fun normalize(raw: String): String = raw
            .trim()
            .uppercase()
            .replace(Regex("[^A-Z0-9]"), "")

        /**
         * Attempt to recover a valid VAT from a potentially over-long AI extraction.
         *
         * First tries the normal [from] path. If the result is invalid and the normalized
         * value is longer than the expected length for its country code, truncates from
         * the right and validates the checksum (e.g., mod-97 for Belgian VATs).
         * Returns null if no valid VAT can be recovered.
         */
        fun tryNormalize(raw: String?): VatNumber? {
            val normal = from(raw)
            if (normal == null) return null
            if (normal.value.isEmpty()) return null
            if (normal.isValid) return normal

            val cleaned = normal.normalized
            if (cleaned.length < 3) return null
            val countryCode = cleaned.substring(0, 2)
            val body = cleaned.substring(2)

            val expectedLength = COUNTRY_VAT_BODY_LENGTHS[countryCode] ?: return null
            if (body.length <= expectedLength) return null

            val truncated = VatNumber("$countryCode${body.substring(0, expectedLength)}")
            return truncated.takeIf { it.isValid }
        }

        /**
         * Expected body length (characters after country code) for EU VATs with fixed-length formats.
         * Variable-length countries (BG, CZ, LT, RO, IE) are excluded — truncation is unsafe for those.
         */
        private val COUNTRY_VAT_BODY_LENGTHS = mapOf(
            "AT" to 9,  // U + 8 digits
            "BE" to 10, // 10 digits
            "DE" to 9,  // 9 digits
            "DK" to 8,  // 8 digits
            "EE" to 9,  // 9 digits
            "EL" to 9,  // 9 digits (Greece)
            "ES" to 9,  // letter + 7 digits + letter
            "FI" to 8,  // 8 digits
            "FR" to 11, // 2 chars + 9 digits
            "GR" to 9,  // 9 digits (Greece alt)
            "HR" to 11, // 11 digits
            "HU" to 8,  // 8 digits
            "IT" to 11, // 11 digits
            "LU" to 8,  // 8 digits
            "LV" to 11, // 11 digits
            "MT" to 8,  // 8 digits
            "NL" to 12, // 9 digits + B + 2 digits
            "PL" to 10, // 10 digits
            "PT" to 9,  // 9 digits
            "SE" to 12, // 12 digits
            "SI" to 8,  // 8 digits
            "SK" to 10, // 10 digits
        )

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
        if (digits.length != BELGIAN_COMPANY_NUMBER_LENGTH) return number
        return "${digits.substring(0, BELGIAN_COMPANY_FIRST_GROUP_END)}." +
            "${digits.substring(BELGIAN_COMPANY_FIRST_GROUP_END, BELGIAN_COMPANY_SECOND_GROUP_END)}." +
            digits.substring(BELGIAN_COMPANY_SECOND_GROUP_END, BELGIAN_COMPANY_NUMBER_LENGTH)
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

@OptIn(ExperimentalUuidApi::class)
@Serializable
@JvmInline
value class PeppolRegistrationId(val value: Uuid) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate(): PeppolRegistrationId = PeppolRegistrationId(Uuid.random())
        fun parse(value: String): PeppolRegistrationId = PeppolRegistrationId(Uuid.parse(value))
    }
}
