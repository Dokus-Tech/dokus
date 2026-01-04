package tech.dokus.features.ai.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import tech.dokus.foundation.backend.utils.loggerFor
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Service for converting documents (PDFs and images) to PNG images for vision model processing.
 *
 * This service is used by the document processing pipeline to prepare document images
 * for analysis by vision-capable LLMs (qwen3-vl). It replaces the need for OCR by
 * allowing the vision model to analyze document images directly.
 *
 * Supported formats:
 * - PDF: Renders each page to PNG at the specified DPI
 * - Images (JPEG, PNG, WebP, GIF, BMP, TIFF): Passed through as-is or converted to PNG
 */
class DocumentImageService {
    private val logger = loggerFor()

    companion object {
        private const val MIN_DPI = 72
        private const val MAX_DPI = 300
        private const val DEFAULT_DPI = 150
        private const val MIN_MAX_PAGES = 1
        private const val MAX_MAX_PAGES = 50
        private const val DEFAULT_MAX_PAGES = 10

        // Supported image MIME types
        private val SUPPORTED_IMAGE_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/bmp",
            "image/tiff"
        )
    }

    /**
     * Represents a single page/image from a document, ready for vision model processing.
     *
     * @property pageNumber 1-indexed page number (always 1 for single images)
     * @property imageBytes PNG image data
     * @property mimeType MIME type (always "image/png" after processing)
     */
    data class DocumentImage(
        val pageNumber: Int,
        val imageBytes: ByteArray,
        val mimeType: String = "image/png"
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DocumentImage) return false
            return pageNumber == other.pageNumber &&
                imageBytes.contentEquals(other.imageBytes) &&
                mimeType == other.mimeType
        }

        override fun hashCode(): Int {
            var result = pageNumber
            result = 31 * result + imageBytes.contentHashCode()
            result = 31 * result + mimeType.hashCode()
            return result
        }
    }

    /**
     * Result of document-to-image conversion.
     *
     * @property images List of page images, ordered by page number
     * @property totalPages Total pages in the source document
     * @property processedPages Number of pages that were actually converted (may be limited by maxPages)
     */
    data class DocumentImages(
        val images: List<DocumentImage>,
        val totalPages: Int,
        val processedPages: Int
    )

    /**
     * Convert a document to a list of images for vision model processing.
     *
     * @param documentBytes Raw document content (PDF or image)
     * @param mimeType MIME type of the document
     * @param maxPages Maximum number of pages to render (for PDFs)
     * @param dpi Resolution for PDF rendering (72-300)
     * @return DocumentImages containing the rendered page images
     * @throws UnsupportedDocumentTypeException if the document type is not supported
     */
    suspend fun getDocumentImages(
        documentBytes: ByteArray,
        mimeType: String,
        maxPages: Int = DEFAULT_MAX_PAGES,
        dpi: Int = DEFAULT_DPI
    ): DocumentImages = withContext(Dispatchers.IO) {
        val clampedDpi = dpi.coerceIn(MIN_DPI, MAX_DPI)
        val clampedMaxPages = maxPages.coerceIn(MIN_MAX_PAGES, MAX_MAX_PAGES)

        when {
            mimeType == "application/pdf" -> {
                logger.debug("Rendering PDF to images (maxPages=$clampedMaxPages, dpi=$clampedDpi)")
                renderPdfToImages(documentBytes, clampedMaxPages, clampedDpi)
            }
            mimeType in SUPPORTED_IMAGE_TYPES -> {
                logger.debug("Processing image document (type=$mimeType)")
                wrapImageAsDocument(documentBytes, mimeType)
            }
            else -> {
                throw UnsupportedDocumentTypeException(
                    "Unsupported document type: $mimeType. " +
                        "Supported types: application/pdf, ${SUPPORTED_IMAGE_TYPES.joinToString()}"
                )
            }
        }
    }

    /**
     * Render PDF pages to PNG images.
     */
    private fun renderPdfToImages(
        pdfBytes: ByteArray,
        maxPages: Int,
        dpi: Int
    ): DocumentImages {
        return Loader.loadPDF(pdfBytes).use { document ->
            val totalPages = document.numberOfPages
            val pagesToRender = minOf(totalPages, maxPages)

            if (totalPages > maxPages) {
                logger.info("PDF has $totalPages pages, limiting to $maxPages for processing")
            }

            val renderer = PDFRenderer(document)
            val images = (1..pagesToRender).map { pageNum ->
                val image = renderer.renderImageWithDPI(pageNum - 1, dpi.toFloat(), ImageType.RGB)
                val imageBytes = ByteArrayOutputStream().use { baos ->
                    ImageIO.write(image, "PNG", baos)
                    baos.toByteArray()
                }
                DocumentImage(pageNumber = pageNum, imageBytes = imageBytes)
            }

            logger.debug("Rendered $pagesToRender/$totalPages pages from PDF")

            DocumentImages(
                images = images,
                totalPages = totalPages,
                processedPages = pagesToRender
            )
        }
    }

    /**
     * Wrap a single image as a document with one page.
     * For non-PNG images, converts to PNG for consistency.
     */
    private fun wrapImageAsDocument(imageBytes: ByteArray, mimeType: String): DocumentImages {
        val pngBytes = if (mimeType == "image/png") {
            imageBytes
        } else {
            // Convert to PNG for consistency
            convertToPng(imageBytes)
        }

        return DocumentImages(
            images = listOf(DocumentImage(pageNumber = 1, imageBytes = pngBytes)),
            totalPages = 1,
            processedPages = 1
        )
    }

    /**
     * Convert an image to PNG format.
     */
    private fun convertToPng(imageBytes: ByteArray): ByteArray {
        val image = ImageIO.read(imageBytes.inputStream())
            ?: throw IllegalArgumentException("Could not read image data")

        return ByteArrayOutputStream().use { baos ->
            ImageIO.write(image, "PNG", baos)
            baos.toByteArray()
        }
    }
}

/**
 * Exception thrown when a document type is not supported for vision processing.
 */
class UnsupportedDocumentTypeException(message: String) : RuntimeException(message)
