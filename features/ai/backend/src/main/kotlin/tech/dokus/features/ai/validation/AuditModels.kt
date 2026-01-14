package tech.dokus.features.ai.validation

import kotlinx.serialization.Serializable

/**
 * Types of validation checks performed by the auditor.
 */
enum class CheckType {
    /** Mathematical verification (subtotal + VAT = total) */
    MATH,

    /** Belgian OGM (Structured Communication) checksum */
    CHECKSUM_OGM,

    /** IBAN checksum verification */
    CHECKSUM_IBAN,

    /** VAT rate sanity check (standard Belgian rates) */
    VAT_RATE,

    /** Company existence in KBO/CBE registry */
    COMPANY_EXISTS,

    /** Company name matches registry */
    COMPANY_NAME
}

/**
 * Severity of a validation check failure.
 */
enum class Severity {
    /** Critical failure - extraction is likely wrong */
    CRITICAL,

    /** Warning - potential issue but not definitive */
    WARNING,

    /** Informational - no action required */
    INFO
}

/**
 * Overall status of an audit.
 */
enum class AuditStatus {
    /** All checks passed */
    PASSED,

    /** Some warnings but no critical failures */
    WARNINGS_ONLY,

    /** At least one critical check failed */
    FAILED
}

/**
 * Result of a single validation check.
 */
@Serializable
data class AuditCheck(
    /** Type of check performed */
    val type: CheckType,

    /** Field that was checked */
    val field: String,

    /** Whether the check passed */
    val passed: Boolean,

    /** Severity of the check */
    val severity: Severity,

    /** Human-readable message describing the result */
    val message: String,

    /** Hint for self-correction (Layer 4 retry prompts) */
    val hint: String? = null,

    /** Expected value (if applicable) */
    val expected: String? = null,

    /** Actual extracted value */
    val actual: String? = null
) {
    companion object {
        /**
         * Create a passing check with INFO severity.
         */
        fun passed(
            type: CheckType,
            field: String,
            message: String
        ) = AuditCheck(
            type = type,
            field = field,
            passed = true,
            severity = Severity.INFO,
            message = message
        )

        /**
         * Create a failing check with CRITICAL severity.
         */
        fun criticalFailure(
            type: CheckType,
            field: String,
            message: String,
            hint: String,
            expected: String? = null,
            actual: String? = null
        ) = AuditCheck(
            type = type,
            field = field,
            passed = false,
            severity = Severity.CRITICAL,
            message = message,
            hint = hint,
            expected = expected,
            actual = actual
        )

        /**
         * Create a failing check with WARNING severity.
         */
        fun warning(
            type: CheckType,
            field: String,
            message: String,
            hint: String? = null,
            expected: String? = null,
            actual: String? = null
        ) = AuditCheck(
            type = type,
            field = field,
            passed = false,
            severity = Severity.WARNING,
            message = message,
            hint = hint,
            expected = expected,
            actual = actual
        )

        /**
         * Create an INFO check for missing/incomplete data.
         */
        fun incomplete(
            type: CheckType,
            field: String,
            message: String
        ) = AuditCheck(
            type = type,
            field = field,
            passed = true, // Can't fail if data is missing
            severity = Severity.INFO,
            message = message
        )
    }
}

/**
 * Complete audit report from Layer 3 validation.
 */
@Serializable
data class AuditReport(
    /** All checks performed */
    val checks: List<AuditCheck>,

    /** Overall status of the audit */
    val overallStatus: AuditStatus
) {
    /** Number of checks that passed */
    val passedCount: Int = checks.count { it.passed }

    /** Number of checks that failed */
    val failedCount: Int = checks.count { !it.passed }

    /** Critical failures requiring attention */
    val criticalFailures: List<AuditCheck> = checks.filter {
        !it.passed && it.severity == Severity.CRITICAL
    }

    /** Warning-level failures */
    val warnings: List<AuditCheck> = checks.filter {
        !it.passed && it.severity == Severity.WARNING
    }

    /** Whether the audit passed (no critical failures) */
    val isValid: Boolean = criticalFailures.isEmpty()

    /** Whether there are any issues (failures or warnings) */
    val hasIssues: Boolean = failedCount > 0

    companion object {
        /**
         * Create an audit report from a list of checks.
         */
        fun fromChecks(checks: List<AuditCheck>): AuditReport {
            val hasCritical = checks.any { !it.passed && it.severity == Severity.CRITICAL }
            val hasWarnings = checks.any { !it.passed && it.severity == Severity.WARNING }

            val status = when {
                hasCritical -> AuditStatus.FAILED
                hasWarnings -> AuditStatus.WARNINGS_ONLY
                else -> AuditStatus.PASSED
            }

            return AuditReport(checks = checks, overallStatus = status)
        }

        /**
         * Empty audit report (no checks performed).
         */
        val EMPTY = AuditReport(
            checks = emptyList(),
            overallStatus = AuditStatus.PASSED
        )
    }
}
