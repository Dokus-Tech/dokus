package tech.dokus.features.ai.extraction

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Index of all extraction test fixtures.
 * Loaded from index.json in the fixtures directory.
 */
@Serializable
data class FixtureIndex(
    val fixtures: List<FixtureReference>
)

/**
 * Reference to a single test fixture.
 */
@Serializable
data class FixtureReference(
    /** Unique identifier for this fixture */
    val id: String,
    /** Path relative to extraction-fixtures directory */
    val path: String,
    /** Document type: INVOICE, BILL, RECEIPT, EXPENSE */
    val documentType: String,
    /** Human-readable description */
    val description: String,
    /** Tags for filtering tests */
    val tags: List<String> = emptyList(),
    /** Whether this fixture is enabled for testing */
    val enabled: Boolean = true
)

/**
 * Per-fixture configuration for comparison behavior.
 * Loaded from config.json in each fixture directory.
 */
@Serializable
data class FixtureConfig(
    /** Document type for extraction */
    val documentType: String,
    /** MIME type of the document file */
    val mimeType: String = "application/pdf",
    /** Maximum pages to process */
    val maxPages: Int? = null,
    /** DPI for PDF rendering */
    val dpi: Int? = null,
    /** Minimum confidence threshold */
    val minConfidence: Double = 0.7,
    /** Default comparison mode for fields */
    val fieldComparisonMode: FieldComparisonMode = FieldComparisonMode.FUZZY,
    /** Fields that must be present and non-null */
    val requiredFields: List<String> = emptyList(),
    /** Fields to skip during comparison */
    val ignoredFields: List<String> = listOf("confidence", "provenance", "extractedText"),
    /** Tolerance for numeric comparisons (e.g., 0.02 for 2 cents) */
    val amountTolerance: Double = 0.02,
    /** Tolerance for date comparisons in days */
    val dateTolerance: Int = 0
)

/**
 * Field comparison modes for handling AI non-determinism.
 */
@Serializable
enum class FieldComparisonMode {
    /** Field must match exactly */
    EXACT,
    /** Allow minor variations (whitespace, case) */
    FUZZY,
    /** Allow semantically equivalent values (VAT normalization, etc.) */
    SEMANTIC,
    /** Compare as numbers with tolerance */
    NUMERIC,
    /** Skip this field during comparison */
    SKIP
}

/**
 * Expected extraction result.
 * Loaded from expected.json in each fixture directory.
 */
@Serializable
data class ExpectedExtraction(
    /** Document type that was classified */
    val documentType: String,
    /** The expected extraction result as JSON */
    val extraction: JsonElement,
    /** Minimum confidence expected (optional override) */
    val minConfidence: Double? = null,
    /** Per-field comparison mode overrides */
    val fieldOverrides: Map<String, FieldComparisonMode> = emptyMap()
)

/**
 * Result of comparing expected vs actual extraction.
 */
data class ComparisonResult(
    val passed: Boolean,
    val fieldMismatches: List<FieldMismatch>
)

/**
 * Details about a field that didn't match expectations.
 */
data class FieldMismatch(
    val field: String,
    val expected: String,
    val actual: String,
    val reason: String
)
