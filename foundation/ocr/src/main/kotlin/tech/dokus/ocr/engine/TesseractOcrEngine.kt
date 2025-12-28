package tech.dokus.ocr.engine

import tech.dokus.ocr.OcrEngine
import tech.dokus.ocr.OcrFailureReason
import tech.dokus.ocr.OcrInput
import tech.dokus.ocr.OcrLanguage
import tech.dokus.ocr.OcrResult
import tech.dokus.ocr.pdf.PdfToImageConverter
import tech.dokus.ocr.process.ProcessExecutor
import tech.dokus.ocr.process.ProcessResult
import tech.dokus.ocr.util.MimeTypes
import tech.dokus.ocr.util.TempFileManager
import tech.dokus.ocr.util.TextNormalizer
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Tesseract-based OCR engine using CLI.
 *
 * Supports:
 * - Direct image OCR (JPEG, PNG, TIFF, BMP, GIF, WebP)
 * - PDF OCR via pdftoppm conversion
 *
 * Requirements:
 * - tesseract CLI installed and on PATH
 * - pdftoppm CLI installed and on PATH (for PDF support)
 * - Language packs: tesseract-ocr-eng, tesseract-ocr-fra, tesseract-ocr-nld
 *
 * Usage:
 * ```kotlin
 * val engine = TesseractOcrEngine()
 * val result = engine.extractText(OcrInput(
 *     filePath = Path.of("document.pdf"),
 *     mimeType = "application/pdf"
 * ))
 * ```
 */
class TesseractOcrEngine : OcrEngine {

    companion object {
        private const val TESSERACT_COMMAND = "tesseract"
        private const val ENGINE_NAME = "tesseract"

        /** Maximum file size: 50 MB */
        const val MAX_FILE_SIZE_BYTES: Long = 50L * 1024 * 1024

        /** Absolute maximum pages allowed */
        const val ABSOLUTE_MAX_PAGES: Int = 50

        /** Minimum output length to be considered non-empty */
        private const val MIN_OUTPUT_LENGTH = 10
    }

    private val pdfConverter = PdfToImageConverter()

    override fun extractText(input: OcrInput): OcrResult {
        val startTime = System.currentTimeMillis()

        // 1. Validate input
        val validationError = validateInput(input)
        if (validationError != null) {
            return validationError
        }

        // 2. Check engine availability
        if (!isEngineAvailable()) {
            return OcrResult.Failure(
                OcrFailureReason.ENGINE_NOT_FOUND,
                "tesseract not found on PATH",
                null
            )
        }

        // 3. Route based on MIME type
        val result = when {
            MimeTypes.isImage(input.mimeType) -> processImage(input)
            MimeTypes.isPdf(input.mimeType) -> processPdf(input)
            else -> OcrResult.Failure(
                OcrFailureReason.UNSUPPORTED_FORMAT,
                "Unsupported MIME type: ${input.mimeType}",
                null
            )
        }

        // 4. Add timing to success results
        return when (result) {
            is OcrResult.Success -> result.copy(
                durationMs = System.currentTimeMillis() - startTime
            )
            is OcrResult.Failure -> result
        }
    }

    /**
     * Validate input before processing.
     */
    private fun validateInput(input: OcrInput): OcrResult.Failure? {
        // Check file exists
        if (!Files.exists(input.filePath)) {
            return OcrResult.Failure(
                OcrFailureReason.PROCESS_ERROR,
                "File not found: ${input.filePath}",
                null
            )
        }

        // Check file size
        val fileSize = Files.size(input.filePath)
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            return OcrResult.Failure(
                OcrFailureReason.FILE_TOO_LARGE,
                "File size $fileSize bytes exceeds limit of $MAX_FILE_SIZE_BYTES bytes",
                null
            )
        }

        // Check max pages limit
        if (input.maxPages > ABSOLUTE_MAX_PAGES || input.maxPages < 1) {
            return OcrResult.Failure(
                OcrFailureReason.TOO_MANY_PAGES,
                "maxPages must be between 1 and $ABSOLUTE_MAX_PAGES, got ${input.maxPages}",
                null
            )
        }

        // Check MIME type support
        if (!MimeTypes.isSupported(input.mimeType)) {
            return OcrResult.Failure(
                OcrFailureReason.UNSUPPORTED_FORMAT,
                "Unsupported MIME type: ${input.mimeType}",
                null
            )
        }

        return null
    }

    /**
     * Check if tesseract is available on PATH.
     */
    private fun isEngineAvailable(): Boolean = ProcessExecutor.commandExists(TESSERACT_COMMAND)

    /**
     * Process a single image file.
     */
    private fun processImage(input: OcrInput): OcrResult {
        val result = runTesseract(input.filePath.toFile(), input.languages, input.timeout)

        return when {
            result.timedOut -> OcrResult.Failure(
                OcrFailureReason.TIMEOUT,
                result.stderr,
                null
            )
            result.exitCode != 0 -> OcrResult.Failure(
                OcrFailureReason.PROCESS_ERROR,
                result.stderr,
                result.exitCode
            )
            result.stdout.isBlank() || result.stdout.length < MIN_OUTPUT_LENGTH -> OcrResult.Failure(
                OcrFailureReason.EMPTY_OUTPUT,
                null,
                null
            )
            else -> OcrResult.Success(
                text = TextNormalizer.normalize(result.stdout),
                pages = 1,
                engine = ENGINE_NAME,
                durationMs = 0 // Will be overwritten
            )
        }
    }

    /**
     * Process a PDF file by converting to images first.
     */
    private fun processPdf(input: OcrInput): OcrResult {
        // Check pdftoppm availability
        if (!pdfConverter.isAvailable()) {
            return OcrResult.Failure(
                OcrFailureReason.ENGINE_NOT_FOUND,
                "pdftoppm not found on PATH (required for PDF support)",
                null
            )
        }

        return TempFileManager.withTempDir { tempDir ->
            // Allocate 1/3 of timeout for PDF conversion, 2/3 for OCR
            val conversionTimeout = (input.timeout.inWholeMilliseconds / 3)
                .coerceAtLeast(5000)
                .milliseconds

            val conversionResult = pdfConverter.convert(
                pdfPath = input.filePath,
                outputDir = tempDir,
                maxPages = input.maxPages,
                timeout = conversionTimeout
            )

            when (conversionResult) {
                is PdfToImageConverter.ConversionOutcome.Failure -> {
                    return@withTempDir OcrResult.Failure(
                        conversionResult.reason,
                        conversionResult.stderr,
                        conversionResult.exitCode
                    )
                }
                is PdfToImageConverter.ConversionOutcome.Success -> {
                    val imageFiles = conversionResult.result.imageFiles

                    if (imageFiles.isEmpty()) {
                        return@withTempDir OcrResult.Failure(
                            OcrFailureReason.EMPTY_OUTPUT,
                            "No pages extracted from PDF",
                            null
                        )
                    }

                    // Note: maxPages is enforced by pdftoppm via -f 1 -l maxPages flags
                    // so we don't need a post-facto check here

                    // OCR each page with remaining timeout
                    val remainingTimeout = (input.timeout.inWholeMilliseconds * 2 / 3)
                        .coerceAtLeast(3000)
                    val perPageTimeout = (remainingTimeout / imageFiles.size)
                        .coerceAtLeast(3000)
                        .milliseconds

                    val pageTexts = mutableListOf<String>()

                    for (imageFile in imageFiles) {
                        val ocrResult = runTesseract(imageFile, input.languages, perPageTimeout)

                        when {
                            ocrResult.timedOut -> {
                                return@withTempDir OcrResult.Failure(
                                    OcrFailureReason.TIMEOUT,
                                    ocrResult.stderr,
                                    null
                                )
                            }
                            ocrResult.exitCode != 0 -> {
                                return@withTempDir OcrResult.Failure(
                                    OcrFailureReason.PROCESS_ERROR,
                                    ocrResult.stderr,
                                    ocrResult.exitCode
                                )
                            }
                            else -> {
                                pageTexts.add(ocrResult.stdout)
                            }
                        }
                    }

                    val combinedText = TextNormalizer.combinePages(pageTexts)

                    // Check if all pages are empty (excluding page markers)
                    val textWithoutMarkers = combinedText.replace(Regex("=== PAGE \\d+ ==="), "")
                    if (textWithoutMarkers.isBlank() || textWithoutMarkers.length < MIN_OUTPUT_LENGTH) {
                        return@withTempDir OcrResult.Failure(
                            OcrFailureReason.EMPTY_OUTPUT,
                            null,
                            null
                        )
                    }

                    OcrResult.Success(
                        text = combinedText,
                        pages = imageFiles.size,
                        engine = ENGINE_NAME,
                        durationMs = 0 // Will be overwritten
                    )
                }
            }
        }
    }

    /**
     * Run tesseract on an image file.
     * Command: tesseract <image> stdout -l eng+fra+nld
     */
    private fun runTesseract(
        imageFile: File,
        languages: Set<OcrLanguage>,
        timeout: Duration
    ): ProcessResult {
        val langParam = languages.joinToString("+") { it.tesseractCode }

        val command = listOf(
            TESSERACT_COMMAND,
            imageFile.absolutePath,
            "stdout", // Output to stdout instead of file
            "-l", langParam
        )

        return ProcessExecutor.execute(command, timeout)
    }
}
