package tech.dokus.ocr

/**
 * Categorized failure reasons for OCR operations.
 */
enum class OcrFailureReason {
    /** Input file format is not supported for OCR */
    UNSUPPORTED_FORMAT,

    /** Input file exceeds maximum allowed size */
    FILE_TOO_LARGE,

    /** PDF has more pages than the configured limit */
    TOO_MANY_PAGES,

    /** OCR process exceeded timeout */
    TIMEOUT,

    /** OCR process failed with error */
    PROCESS_ERROR,

    /** Required OCR engine (tesseract/pdftoppm) not found on PATH */
    ENGINE_NOT_FOUND,

    /** OCR completed but produced no usable text */
    EMPTY_OUTPUT
}
