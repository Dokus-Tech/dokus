package tech.dokus.ocr.util

/**
 * MIME type classification for OCR-supported formats.
 */
internal object MimeTypes {

    private val IMAGE_TYPES = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/gif",
        "image/webp",
        "image/tiff",
        "image/bmp"
    )

    private val PDF_TYPES = setOf(
        "application/pdf"
    )

    fun isImage(mimeType: String): Boolean = mimeType.lowercase() in IMAGE_TYPES

    fun isPdf(mimeType: String): Boolean = mimeType.lowercase() in PDF_TYPES

    fun isSupported(mimeType: String): Boolean = isImage(mimeType) || isPdf(mimeType)
}
