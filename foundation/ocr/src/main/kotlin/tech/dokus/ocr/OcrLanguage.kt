package tech.dokus.ocr

/**
 * Supported OCR languages.
 * Maps to Tesseract language codes.
 */
enum class OcrLanguage(val tesseractCode: String) {
    ENG("eng"),
    FRA("fra"),
    NLD("nld")
}
