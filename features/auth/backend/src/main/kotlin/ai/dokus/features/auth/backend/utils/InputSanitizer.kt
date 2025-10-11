package ai.dokus.features.auth.backend.utils

import java.util.regex.Pattern

object InputSanitizer {
    // Patterns for validation
    private val EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    )

    private val MATRICULE_PATTERN = Pattern.compile(
        "^[A-Z0-9]{5,10}$"
    )

    private val PHONE_PATTERN = Pattern.compile(
        "^\\+?[0-9]{8,15}$"
    )

    private val SQL_INJECTION_PATTERN = Pattern.compile(
        "('.+--)|(--)|(\\|\\|)|(;)|(\\*)|(\\b(ALTER|CREATE|DELETE|DROP|EXEC(UTE)?|INSERT|SELECT|UNION|UPDATE)\\b)",
        Pattern.CASE_INSENSITIVE
    )

    private val XSS_PATTERN = Pattern.compile(
        "<script[^>]*>.*?</script>|<iframe.*?>.*?</iframe>|javascript:|on\\w+\\s*=",
        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
    )

    /**
     * Sanitize general text input - removes HTML/script tags and dangerous characters
     */
    fun sanitizeText(input: String?): String? {
        if (input.isNullOrBlank()) return input

        return input
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
            .trim()
    }

    /**
     * Sanitize input for use in SQL queries (beyond parameterized queries)
     */
    fun sanitizeForSql(input: String?): String? {
        if (input.isNullOrBlank()) return input

        // Check for SQL injection patterns
        if (SQL_INJECTION_PATTERN.matcher(input).find()) {
            throw IllegalArgumentException("Potential SQL injection detected")
        }

        return input
            .replace("'", "''")  // Escape single quotes
            .replace("\\", "\\\\") // Escape backslashes
            .replace("%", "\\%")   // Escape wildcards
            .replace("_", "\\_")
            .trim()
    }

    /**
     * Validate and sanitize email addresses
     */
    fun sanitizeEmail(email: String?): String {
        if (email.isNullOrBlank()) {
            throw IllegalArgumentException("Email cannot be empty")
        }

        val trimmed = email.trim().lowercase()

        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw IllegalArgumentException("Invalid email format")
        }

        // Additional check for dangerous content
        if (XSS_PATTERN.matcher(trimmed).find()) {
            throw IllegalArgumentException("Email contains invalid characters")
        }

        return trimmed
    }

    /**
     * Validate and sanitize matricule
     */
    fun sanitizeMatricule(matricule: String?): String {
        if (matricule.isNullOrBlank()) {
            throw IllegalArgumentException("Matricule cannot be empty")
        }

        val trimmed = matricule.trim().uppercase()

        if (!MATRICULE_PATTERN.matcher(trimmed).matches()) {
            throw IllegalArgumentException("Invalid matricule format")
        }

        return trimmed
    }

    /**
     * Validate and sanitize phone numbers
     */
    fun sanitizePhoneNumber(phone: String?): String? {
        if (phone.isNullOrBlank()) return null

        val cleaned = phone.replace("[^0-9+]".toRegex(), "")

        if (cleaned.isNotEmpty() && !PHONE_PATTERN.matcher(cleaned).matches()) {
            throw IllegalArgumentException("Invalid phone number format")
        }

        return cleaned.ifEmpty { null }
    }

    /**
     * Sanitize search queries - allows limited wildcards but prevents injection
     */
    fun sanitizeSearchQuery(query: String?): String? {
        if (query.isNullOrBlank()) return null

        // Remove dangerous characters but keep wildcards for search
        var sanitized = query.trim()

        // Check for injection attempts
        if (SQL_INJECTION_PATTERN.matcher(sanitized).find()) {
            throw IllegalArgumentException("Invalid search query")
        }

        // Escape special characters except wildcards
        sanitized = sanitized
            .replace("'", "")
            .replace("\"", "")
            .replace(";", "")
            .replace("--", "")
            .replace("/*", "")
            .replace("*/", "")
            .replace("xp_", "")
            .replace("sp_", "")

        return sanitized.take(100) // Limit query length
    }

    /**
     * Validate unit code format
     */
    fun sanitizeUnitCode(unitCode: String?): String {
        if (unitCode.isNullOrBlank()) {
            throw IllegalArgumentException("Unit code cannot be empty")
        }

        val trimmed = unitCode.trim().uppercase()

        // Unit codes should be alphanumeric with possible dashes
        if (!trimmed.matches(Regex("^[A-Z0-9-]{2,20}$"))) {
            throw IllegalArgumentException("Invalid unit code format")
        }

        return trimmed
    }

    /**
     * Sanitize name fields (first name, last name)
     */
    fun sanitizeName(name: String?): String {
        if (name.isNullOrBlank()) {
            throw IllegalArgumentException("Name cannot be empty")
        }

        val trimmed = name.trim()

        // Names should only contain letters, spaces, hyphens, and apostrophes
        if (!trimmed.matches(Regex("^[a-zA-ZÀ-ÿ\\s'-]{1,50}$"))) {
            throw IllegalArgumentException("Invalid name format")
        }

        // Check for XSS attempts
        if (XSS_PATTERN.matcher(trimmed).find()) {
            throw IllegalArgumentException("Name contains invalid characters")
        }

        return trimmed
    }

    /**
     * Check if input contains potential XSS attack vectors
     */
    fun containsXss(input: String?): Boolean {
        return input != null && XSS_PATTERN.matcher(input).find()
    }

    /**
     * Check if input contains potential SQL injection
     */
    fun containsSqlInjection(input: String?): Boolean {
        return input != null && SQL_INJECTION_PATTERN.matcher(input).find()
    }
}