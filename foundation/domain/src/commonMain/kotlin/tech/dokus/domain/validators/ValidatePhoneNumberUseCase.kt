package tech.dokus.domain.validators

import tech.dokus.domain.PhoneNumber

object ValidatePhoneNumberUseCase : Validator<PhoneNumber> {
    private const val MIN_DIGITS = 7
    private const val MAX_DIGITS = 15

    // Matches optional + prefix, followed by digits and allowed separators (spaces, hyphens, dots, parentheses)
    private val formatRegex = Regex("^\\+?[\\d\\s\\-.()\u00A0]+$")

    override operator fun invoke(value: PhoneNumber): Boolean {
        val phone = value.value.trim()
        if (phone.isEmpty()) return false
        if (!formatRegex.matches(phone)) return false

        // Count only digits for length validation
        val digitCount = phone.count { it.isDigit() }
        return digitCount in MIN_DIGITS..MAX_DIGITS
    }
}
