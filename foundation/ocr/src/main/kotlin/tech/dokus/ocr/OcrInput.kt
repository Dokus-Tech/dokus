package tech.dokus.ocr

import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Input configuration for OCR extraction.
 *
 * @property filePath Path to the file to process (PDF or image)
 * @property mimeType MIME type of the file (e.g., "application/pdf", "image/png")
 * @property maxPages Maximum number of pages to process for PDFs (1-50)
 * @property languages Set of languages to use for OCR
 * @property timeout Maximum time allowed for the entire OCR operation
 */
data class OcrInput(
    val filePath: Path,
    val mimeType: String,
    val maxPages: Int = 10,
    val languages: Set<OcrLanguage> = setOf(OcrLanguage.ENG, OcrLanguage.FRA, OcrLanguage.NLD),
    val timeout: Duration = 15.seconds
)
