package tech.dokus.ocr.util

/**
 * Normalizes OCR output text.
 */
internal object TextNormalizer {

    /**
     * Normalize whitespace and line endings in OCR output.
     * - Convert \r\n and \r to \n
     * - Collapse multiple spaces to single space
     * - Collapse more than 2 consecutive newlines to 2
     * - Trim leading/trailing whitespace
     */
    fun normalize(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex(" +"), " ")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    /**
     * Combine multiple page texts with page markers.
     */
    fun combinePages(pages: List<String>): String {
        return pages.mapIndexed { index, pageText ->
            "=== PAGE ${index + 1} ===\n${normalize(pageText)}"
        }.joinToString("\n\n")
    }
}
