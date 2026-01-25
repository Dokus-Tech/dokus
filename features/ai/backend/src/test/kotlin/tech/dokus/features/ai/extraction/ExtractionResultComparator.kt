package tech.dokus.features.ai.extraction

import kotlinx.serialization.json.*
import kotlin.math.abs

/**
 * Compares extraction results against expected fixtures.
 * Handles AI non-determinism through configurable comparison modes.
 */
class ExtractionResultComparator {

    /**
     * Compare actual extraction result against expected.
     */
    fun compare(
        expected: ExpectedExtraction,
        actual: JsonElement,
        config: FixtureConfig
    ): ComparisonResult {
        val mismatches = mutableListOf<FieldMismatch>()

        val expectedObj = expected.extraction as? JsonObject
            ?: return ComparisonResult(
                passed = false,
                fieldMismatches = listOf(
                    FieldMismatch("root", "JsonObject", actual.toString(), "Expected JSON object")
                )
            )

        val actualObj = actual as? JsonObject
            ?: return ComparisonResult(
                passed = false,
                fieldMismatches = listOf(
                    FieldMismatch("root", expectedObj.toString(), actual.toString(), "Actual is not JSON object")
                )
            )

        // Compare each expected field
        for ((fieldName, expectedValue) in expectedObj) {
            if (fieldName in config.ignoredFields) continue

            val actualValue = actualObj[fieldName]
            val mode = expected.fieldOverrides[fieldName] ?: config.fieldComparisonMode

            val mismatch = compareField(
                fieldName = fieldName,
                expected = expectedValue,
                actual = actualValue,
                mode = mode,
                config = config
            )

            if (mismatch != null) {
                mismatches.add(mismatch)
            }
        }

        // Check required fields are present
        for (requiredField in config.requiredFields) {
            if (actualObj[requiredField] == null || actualObj[requiredField] is JsonNull) {
                mismatches.add(
                    FieldMismatch(
                        field = requiredField,
                        expected = "non-null",
                        actual = "null or missing",
                        reason = "Required field is missing"
                    )
                )
            }
        }

        return ComparisonResult(
            passed = mismatches.isEmpty(),
            fieldMismatches = mismatches
        )
    }

    private fun compareField(
        fieldName: String,
        expected: JsonElement,
        actual: JsonElement?,
        mode: FieldComparisonMode,
        config: FixtureConfig
    ): FieldMismatch? {
        if (mode == FieldComparisonMode.SKIP) return null

        if (actual == null || actual is JsonNull) {
            return if (expected is JsonNull || expected.toString() == "null") {
                null
            } else {
                FieldMismatch(fieldName, expected.toString(), "null", "Missing value")
            }
        }

        return when (mode) {
            FieldComparisonMode.EXACT -> compareExact(fieldName, expected, actual)
            FieldComparisonMode.FUZZY -> compareFuzzy(fieldName, expected, actual)
            FieldComparisonMode.SEMANTIC -> compareSemantic(fieldName, expected, actual)
            FieldComparisonMode.NUMERIC -> compareNumeric(fieldName, expected, actual, config)
            FieldComparisonMode.SKIP -> null
        }
    }

    private fun compareExact(
        fieldName: String,
        expected: JsonElement,
        actual: JsonElement
    ): FieldMismatch? {
        return if (expected == actual) null
        else FieldMismatch(fieldName, expected.toString(), actual.toString(), "Exact match failed")
    }

    private fun compareFuzzy(
        fieldName: String,
        expected: JsonElement,
        actual: JsonElement
    ): FieldMismatch? {
        // Handle arrays
        if (expected is JsonArray && actual is JsonArray) {
            return compareArraysFuzzy(fieldName, expected, actual)
        }

        // Handle objects recursively
        if (expected is JsonObject && actual is JsonObject) {
            return compareObjectsFuzzy(fieldName, expected, actual)
        }

        val expectedStr = expected.asStringOrNull()?.normalize()
            ?: return compareExact(fieldName, expected, actual)
        val actualStr = actual.asStringOrNull()?.normalize()
            ?: return FieldMismatch(fieldName, expected.toString(), actual.toString(), "Type mismatch")

        return if (expectedStr.equals(actualStr, ignoreCase = true)) null
        else FieldMismatch(fieldName, expectedStr, actualStr, "Fuzzy match failed")
    }

    private fun compareArraysFuzzy(
        fieldName: String,
        expected: JsonArray,
        actual: JsonArray
    ): FieldMismatch? {
        if (expected.size != actual.size) {
            return FieldMismatch(
                fieldName,
                "array[${expected.size}]",
                "array[${actual.size}]",
                "Array size mismatch"
            )
        }
        // For arrays, we just check size; detailed element comparison would be complex
        return null
    }

    private fun compareObjectsFuzzy(
        fieldName: String,
        expected: JsonObject,
        actual: JsonObject
    ): FieldMismatch? {
        // Simplified: just check that actual has all expected keys
        val missingKeys = expected.keys - actual.keys
        if (missingKeys.isNotEmpty()) {
            return FieldMismatch(
                fieldName,
                "object with keys: ${expected.keys}",
                "object with keys: ${actual.keys}",
                "Missing keys: $missingKeys"
            )
        }
        return null
    }

    private fun compareSemantic(
        fieldName: String,
        expected: JsonElement,
        actual: JsonElement
    ): FieldMismatch? {
        val expectedStr = expected.asStringOrNull()
            ?: return compareFuzzy(fieldName, expected, actual)
        val actualStr = actual.asStringOrNull()
            ?: return FieldMismatch(fieldName, expected.toString(), actual.toString(), "Type mismatch")

        // VAT number normalization
        if (fieldName.contains("vat", ignoreCase = true) || fieldName.contains("btw", ignoreCase = true)) {
            val normalizedExpected = normalizeVatNumber(expectedStr)
            val normalizedActual = normalizeVatNumber(actualStr)
            return if (normalizedExpected == normalizedActual) null
            else FieldMismatch(fieldName, normalizedExpected, normalizedActual, "VAT normalization mismatch")
        }

        // IBAN normalization
        if (fieldName.contains("iban", ignoreCase = true) || fieldName.contains("bank", ignoreCase = true)) {
            val normalizedExpected = normalizeIban(expectedStr)
            val normalizedActual = normalizeIban(actualStr)
            return if (normalizedExpected == normalizedActual) null
            else FieldMismatch(fieldName, normalizedExpected, normalizedActual, "IBAN normalization mismatch")
        }

        // Date normalization
        if (fieldName.contains("date", ignoreCase = true)) {
            val normalizedExpected = normalizeDate(expectedStr)
            val normalizedActual = normalizeDate(actualStr)
            return if (normalizedExpected == normalizedActual) null
            else FieldMismatch(fieldName, normalizedExpected, normalizedActual, "Date normalization mismatch")
        }

        // OGM/structured communication
        if (fieldName.contains("reference", ignoreCase = true) || fieldName.contains("ogm", ignoreCase = true)) {
            val normalizedExpected = normalizeOgm(expectedStr)
            val normalizedActual = normalizeOgm(actualStr)
            return if (normalizedExpected == normalizedActual) null
            else FieldMismatch(fieldName, normalizedExpected, normalizedActual, "OGM normalization mismatch")
        }

        return compareFuzzy(fieldName, expected, actual)
    }

    private fun compareNumeric(
        fieldName: String,
        expected: JsonElement,
        actual: JsonElement,
        config: FixtureConfig
    ): FieldMismatch? {
        val expectedNum = expected.asDoubleOrNull()
            ?: return FieldMismatch(fieldName, expected.toString(), actual.toString(), "Expected not numeric")
        val actualNum = actual.asDoubleOrNull()
            ?: return FieldMismatch(fieldName, expected.toString(), actual.toString(), "Actual not numeric")

        val diff = abs(expectedNum - actualNum)
        return if (diff <= config.amountTolerance) null
        else FieldMismatch(
            fieldName,
            expectedNum.toString(),
            actualNum.toString(),
            "Numeric difference $diff exceeds tolerance ${config.amountTolerance}"
        )
    }

    // --- Normalization helpers ---

    private fun String.normalize() = this.trim().replace(Regex("\\s+"), " ")

    private fun normalizeVatNumber(vat: String): String =
        vat.replace(Regex("[^A-Za-z0-9]"), "").uppercase()

    private fun normalizeIban(iban: String): String =
        iban.replace(Regex("\\s"), "").uppercase()

    private fun normalizeDate(date: String): String {
        // Convert DD/MM/YYYY or DD-MM-YYYY to YYYY-MM-DD
        val ddmmyyyy = Regex("(\\d{2})[/\\-](\\d{2})[/\\-](\\d{4})")
        return ddmmyyyy.replace(date) { match ->
            "${match.groupValues[3]}-${match.groupValues[2]}-${match.groupValues[1]}"
        }
    }

    private fun normalizeOgm(ogm: String): String =
        ogm.replace(Regex("[^0-9]"), "")

    // --- JSON helpers ---

    private fun JsonElement.asStringOrNull(): String? =
        (this as? JsonPrimitive)?.contentOrNull

    private fun JsonElement.asDoubleOrNull(): Double? {
        return when (this) {
            is JsonPrimitive -> this.doubleOrNull ?: this.contentOrNull?.toDoubleOrNull()
            else -> null
        }
    }
}
