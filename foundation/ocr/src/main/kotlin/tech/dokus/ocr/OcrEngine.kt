package tech.dokus.ocr

/**
 * OCR engine interface for text extraction from documents.
 *
 * Implementations are stateless and deterministic.
 * This is pure infrastructure: file in → text out (or failure).
 *
 * Usage:
 * ```kotlin
 * val engine: OcrEngine = TesseractOcrEngine()
 * val result = engine.extractText(OcrInput(
 *     filePath = Path.of("/path/to/document.pdf"),
 *     mimeType = "application/pdf"
 * ))
 * when (result) {
 *     is OcrResult.Success -> println(result.text)
 *     is OcrResult.Failure -> println("Failed: ${result.reason}")
 * }
 * ```
 */
interface OcrEngine {

    /**
     * Extract text from a document.
     *
     * @param input OCR input configuration
     * @return Success with extracted text, or Failure with reason
     */
    fun extractText(input: OcrInput): OcrResult
}
