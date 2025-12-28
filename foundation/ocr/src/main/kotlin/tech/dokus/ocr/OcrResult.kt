package tech.dokus.ocr

/**
 * Result of an OCR extraction operation.
 */
sealed class OcrResult {

    /**
     * Successful OCR extraction.
     *
     * @property text Extracted and normalized text content
     * @property pages Number of pages processed
     * @property engine Name of the OCR engine used (e.g., "tesseract")
     * @property durationMs Time taken for the OCR operation in milliseconds
     */
    data class Success(
        val text: String,
        val pages: Int,
        val engine: String,
        val durationMs: Long
    ) : OcrResult()

    /**
     * Failed OCR extraction.
     *
     * @property reason Categorized failure reason
     * @property stderr Standard error output from the OCR process, if available
     * @property exitCode Exit code from the OCR process, if available
     */
    data class Failure(
        val reason: OcrFailureReason,
        val stderr: String?,
        val exitCode: Int?
    ) : OcrResult()
}
