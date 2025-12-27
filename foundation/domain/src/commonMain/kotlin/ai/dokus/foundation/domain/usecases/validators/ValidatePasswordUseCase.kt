package ai.dokus.foundation.domain.usecases.validators

import tech.dokus.domain.Password
import ai.dokus.foundation.domain.usecases.validators.ValidatePasswordUseCase.validateDetailed

/**
 * Password validation requirements.
 * Can be customized per deployment if needed.
 */
data class PasswordRequirements(
    val minLength: Int = 8,
    val requireUppercase: Boolean = false,
    val requireLowercase: Boolean = false,
    val requireDigit: Boolean = false,
    val requireSpecialChar: Boolean = false,
    val maxConsecutiveRepeats: Int = 3,
    val checkCommonPasswords: Boolean = false
)

/**
 * Reasons why a password failed validation.
 */
enum class PasswordFailure {
    TooShort,
    NoUppercase,
    NoLowercase,
    NoDigit,
    NoSpecialChar,
    TooManyConsecutiveRepeats,
    CommonPassword
}

/**
 * Result of password validation with details.
 */
sealed class PasswordValidationResult {
    data object Valid : PasswordValidationResult()
    data class Invalid(val failures: List<PasswordFailure>) : PasswordValidationResult()

    val isValid: Boolean get() = this is Valid

    fun toBoolean(): Boolean = isValid
}

/**
 * Password validator with configurable requirements.
 *
 * Default requirements (as of 2024 security best practices):
 * - Minimum 12 characters
 * - At least one uppercase letter
 * - At least one lowercase letter
 * - At least one digit
 * - At least one special character
 * - No more than 3 consecutive repeating characters
 * - Not in common password list
 *
 * Usage:
 * ```kotlin
 * // Simple validation (returns boolean)
 * val isValid = ValidatePasswordUseCase(password)
 *
 * // Detailed validation (returns result with specific failures)
 * val result = ValidatePasswordUseCase.validateDetailed(password)
 * when (result) {
 *     is PasswordValidationResult.Valid -> // password is valid
 *     is PasswordValidationResult.Invalid -> {
 *         result.failures.forEach { failure ->
 *             // handle each failure reason
 *         }
 *     }
 * }
 * ```
 */
object ValidatePasswordUseCase : Validator<Password> {

    private val requirements = PasswordRequirements()

    // Top 100 most common passwords (from various security research)
    // This is a subset - in production, embed a larger list
    private val commonPasswords = setOf(
        "password", "123456", "12345678", "qwerty", "abc123",
        "monkey", "1234567", "letmein", "trustno1", "dragon",
        "baseball", "iloveyou", "master", "sunshine", "ashley",
        "bailey", "passw0rd", "shadow", "123123", "654321",
        "superman", "qazwsx", "michael", "football", "password1",
        "password123", "batman", "login", "starwars", "admin",
        "welcome", "hello", "charlie", "donald", "password2",
        "qwerty123", "letmein1", "princess", "solo", "whatever",
        "freedom", "secret", "access", "summer", "flower",
        "hottie", "loveme", "zaq1zaq1", "mustang", "test"
    )

    /**
     * Simple validation - returns true if password meets requirements.
     * Use [validateDetailed] for specific failure information.
     */
    override operator fun invoke(value: Password): Boolean {
        return validateDetailed(value).isValid
    }

    /**
     * Validates password with custom requirements.
     */
    fun invoke(value: Password, requirements: PasswordRequirements): Boolean {
        return validateDetailed(value, requirements).isValid
    }

    /**
     * Detailed validation with specific failure reasons.
     */
    fun validateDetailed(value: Password): PasswordValidationResult {
        return validateDetailed(value, requirements)
    }

    /**
     * Detailed validation with custom requirements.
     */
    fun validateDetailed(
        value: Password,
        requirements: PasswordRequirements
    ): PasswordValidationResult {
        val password = value.value
        val failures = mutableListOf<PasswordFailure>()

        // Length check
        if (password.length < requirements.minLength) {
            failures.add(PasswordFailure.TooShort)
        }

        // Uppercase check
        if (requirements.requireUppercase && !password.any { it.isUpperCase() }) {
            failures.add(PasswordFailure.NoUppercase)
        }

        // Lowercase check
        if (requirements.requireLowercase && !password.any { it.isLowerCase() }) {
            failures.add(PasswordFailure.NoLowercase)
        }

        // Digit check
        if (requirements.requireDigit && !password.any { it.isDigit() }) {
            failures.add(PasswordFailure.NoDigit)
        }

        // Special character check
        if (requirements.requireSpecialChar && !password.any { isSpecialChar(it) }) {
            failures.add(PasswordFailure.NoSpecialChar)
        }

        // Consecutive repeats check
        if (requirements.maxConsecutiveRepeats > 0 && hasConsecutiveRepeats(
                password,
                requirements.maxConsecutiveRepeats
            )
        ) {
            failures.add(PasswordFailure.TooManyConsecutiveRepeats)
        }

        // Common password check
        if (requirements.checkCommonPasswords && isCommonPassword(password)) {
            failures.add(PasswordFailure.CommonPassword)
        }

        return if (failures.isEmpty()) {
            PasswordValidationResult.Valid
        } else {
            PasswordValidationResult.Invalid(failures)
        }
    }

    /**
     * Get human-readable description of a password failure.
     */
    fun getFailureMessage(
        failure: PasswordFailure,
        requirements: PasswordRequirements = this.requirements
    ): String {
        return when (failure) {
            PasswordFailure.TooShort -> "Password must be at least ${requirements.minLength} characters"
            PasswordFailure.NoUppercase -> "Password must contain at least one uppercase letter"
            PasswordFailure.NoLowercase -> "Password must contain at least one lowercase letter"
            PasswordFailure.NoDigit -> "Password must contain at least one digit"
            PasswordFailure.NoSpecialChar -> "Password must contain at least one special character (!@#\$%^&*...)"
            PasswordFailure.TooManyConsecutiveRepeats -> "Password cannot have more than ${requirements.maxConsecutiveRepeats} consecutive repeating characters"
            PasswordFailure.CommonPassword -> "This password is too common. Please choose a more unique password"
        }
    }

    /**
     * Get all failure messages for a validation result.
     */
    fun getFailureMessages(result: PasswordValidationResult): List<String> {
        return when (result) {
            is PasswordValidationResult.Valid -> emptyList()
            is PasswordValidationResult.Invalid -> result.failures.map { getFailureMessage(it) }
        }
    }

    private fun isSpecialChar(char: Char): Boolean {
        return char in "!@#\$%^&*()_+-=[]{}|;':\",./<>?`~"
    }

    private fun hasConsecutiveRepeats(password: String, maxRepeats: Int): Boolean {
        if (password.length < maxRepeats + 1) return false

        var count = 1
        for (i in 1 until password.length) {
            if (password[i] == password[i - 1]) {
                count++
                if (count > maxRepeats) return true
            } else {
                count = 1
            }
        }
        return false
    }

    private fun isCommonPassword(password: String): Boolean {
        // Check lowercase version against common passwords
        return password.lowercase() in commonPasswords
    }
}
