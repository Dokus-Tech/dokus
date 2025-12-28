package tech.dokus.ocr.pdf

import tech.dokus.ocr.OcrFailureReason
import tech.dokus.ocr.process.ProcessExecutor
import java.io.File
import java.nio.file.Path
import kotlin.time.Duration

/**
 * Converts PDF pages to images using pdftoppm CLI.
 */
internal class PdfToImageConverter {

    companion object {
        private const val PDFTOPPM_COMMAND = "pdftoppm"
        private const val DPI = 300
        private const val OUTPUT_FORMAT = "png"
    }

    /**
     * Result of a successful PDF to image conversion.
     */
    data class ConversionResult(
        val imageFiles: List<File>,
        val pageCount: Int
    )

    /**
     * Outcome of a conversion attempt.
     */
    sealed class ConversionOutcome {
        data class Success(val result: ConversionResult) : ConversionOutcome()
        data class Failure(
            val reason: OcrFailureReason,
            val stderr: String?,
            val exitCode: Int?
        ) : ConversionOutcome()
    }

    /**
     * Check if pdftoppm is available on PATH.
     */
    fun isAvailable(): Boolean = ProcessExecutor.commandExists(PDFTOPPM_COMMAND)

    /**
     * Convert PDF to images.
     *
     * @param pdfPath Path to the PDF file
     * @param outputDir Directory to write images to
     * @param maxPages Maximum number of pages to convert (1-indexed)
     * @param timeout Timeout for the conversion process
     * @return ConversionOutcome with either success or failure
     */
    fun convert(
        pdfPath: Path,
        outputDir: Path,
        maxPages: Int,
        timeout: Duration
    ): ConversionOutcome {

        val outputPrefix = outputDir.resolve("page").toString()

        // pdftoppm -png -r 300 -f 1 -l <maxPages> input.pdf outputPrefix
        // -f 1 = first page (explicit), -l N = last page to convert
        // This enforces maxPages at render time, not post-facto
        val command = listOf(
            PDFTOPPM_COMMAND,
            "-$OUTPUT_FORMAT",
            "-r", DPI.toString(),
            "-f", "1",                  // Start from first page (explicit)
            "-l", maxPages.toString(),  // Stop at maxPages (hard limit)
            pdfPath.toAbsolutePath().toString(),
            outputPrefix
        )

        val result = ProcessExecutor.execute(command, timeout)

        if (result.timedOut) {
            return ConversionOutcome.Failure(OcrFailureReason.TIMEOUT, result.stderr, null)
        }

        if (result.exitCode != 0) {
            return ConversionOutcome.Failure(
                OcrFailureReason.PROCESS_ERROR,
                result.stderr,
                result.exitCode
            )
        }

        // Find generated image files (page-1.png, page-2.png, etc.)
        val imageFiles = outputDir.toFile()
            .listFiles { _, name -> name.startsWith("page-") && name.endsWith(".png") }
            ?.sortedBy { file ->
                // Extract page number: page-1.png -> 1
                file.nameWithoutExtension.substringAfter("page-").toIntOrNull() ?: 0
            }
            ?: emptyList()

        return ConversionOutcome.Success(ConversionResult(imageFiles, imageFiles.size))
    }
}
