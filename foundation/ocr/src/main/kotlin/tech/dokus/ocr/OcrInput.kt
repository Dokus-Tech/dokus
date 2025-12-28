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
 * @property dpi Resolution for PDF to image conversion (default 150, lower = faster)
 * @property languages Set of languages to use for OCR
 * @property timeout Base timeout for the OCR operation
 * @property perPageBudget Additional time budget per page for multi-page PDFs
 * @property maxTimeout Hard cap on total timeout regardless of page count
 */
data class OcrInput(
    val filePath: Path,
    val mimeType: String,
    val maxPages: Int = 10,
    val dpi: Int = 150,
    val languages: Set<OcrLanguage> = setOf(OcrLanguage.ENG, OcrLanguage.FRA, OcrLanguage.NLD),
    val timeout: Duration = 60.seconds,
    val perPageBudget: Duration = 10.seconds,
    val maxTimeout: Duration = 180.seconds
)
