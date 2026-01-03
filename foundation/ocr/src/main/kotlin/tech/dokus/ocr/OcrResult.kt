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
     * @property errorMessage Human-readable error message with details
     * @property timeoutDetails Structured timeout information when reason is TIMEOUT
     */
    data class Failure(
        val reason: OcrFailureReason,
        val stderr: String?,
        val exitCode: Int?,
        val errorMessage: String? = null,
        val timeoutDetails: TimeoutDetails? = null
    ) : OcrResult() {
        /**
         * Get a descriptive error message for logging/display.
         */
        fun toErrorString(): String = errorMessage ?: "OCR failed: $reason"
    }
}

/**
 * Stage at which a timeout occurred during OCR processing.
 */
enum class TimeoutStage {
    /** pdftoppm timed out during PDF to image conversion */
    PDF_CONVERSION,

    /** tesseract timed out while processing a specific page */
    OCR_PAGE,

    /** tesseract timed out while processing a single image input */
    OCR_IMAGE
}

/**
 * Detailed information about a timeout failure.
 *
 * @property stage Which stage of OCR processing timed out
 * @property timeoutMs The timeout value in milliseconds that was exceeded
 * @property pagesProcessed Number of pages successfully processed before timeout
 * @property totalPages Total number of pages to process (null if unknown)
 */
data class TimeoutDetails(
    val stage: TimeoutStage,
    val timeoutMs: Long,
    val pagesProcessed: Int,
    val totalPages: Int?
)
