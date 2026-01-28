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
        private const val MIN_PAGE_COUNT = 1
        private const val MAX_PAGE_COUNT = 10
        private const val DEFAULT_PAGE_COUNT = 3

        private val SUPPORTED_IMAGE_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/bmp",
            "image/tiff"
        )
    }

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
     * Result of document-to-image conversion with pagination support.
     */
    data class DocumentImages(
        val images: List<DocumentImage>,
        val totalPages: Int,
        val startPage: Int,
        val endPage: Int,
        val hasMorePages: Boolean,
        val nextPageStart: Int?
    ) {
        val processedPages: Int get() = images.size
    }

    /**
     * Convert a document to a list of images for vision model processing.
     *
     * @param documentBytes Raw document content (PDF or image)
     * @param mimeType MIME type of the document
     * @param startPage First page to render, 1-indexed (default: 1)
     * @param pageCount Number of pages to render from startPage (default: 3, max: 10)
     * @param dpi Resolution for PDF rendering (72-300)
     * @return DocumentImages containing the rendered page images with pagination metadata
     * @throws UnsupportedDocumentTypeException if the document type is not supported
     */
    suspend fun getDocumentImages(
        documentBytes: ByteArray,
        mimeType: String,
        startPage: Int = 1,
        pageCount: Int = DEFAULT_PAGE_COUNT,
        dpi: Int = DEFAULT_DPI
    ): DocumentImages = withContext(Dispatchers.IO) {
        val clampedDpi = dpi.coerceIn(MIN_DPI, MAX_DPI)
        val clampedPageCount = pageCount.coerceIn(MIN_PAGE_COUNT, MAX_PAGE_COUNT)

        when (mimeType) {
            "application/pdf" -> {
                logger.debug("Rendering PDF pages (startPage=$startPage, pageCount=$clampedPageCount, dpi=$clampedDpi)")
                renderPdfToImages(documentBytes, startPage, clampedPageCount, clampedDpi)
            }

            in SUPPORTED_IMAGE_TYPES -> {
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

    private fun renderPdfToImages(
        pdfBytes: ByteArray,
        startPage: Int,
        pageCount: Int,
        dpi: Int
    ): DocumentImages {
        return Loader.loadPDF(pdfBytes).use { document ->
            val totalPages = document.numberOfPages

            // Clamp startPage to valid range
            val actualStartPage = startPage.coerceIn(1, totalPages)

            // Calculate end page
            val actualEndPage = (actualStartPage + pageCount - 1).coerceAtMost(totalPages)

            val renderer = PDFRenderer(document)
            val images = (actualStartPage..actualEndPage).map { pageNum ->
                val image = renderer.renderImageWithDPI(pageNum - 1, dpi.toFloat(), ImageType.RGB)
                val imageBytes = ByteArrayOutputStream().use { baos ->
                    ImageIO.write(image, "PNG", baos)
                    baos.toByteArray()
                }
                DocumentImage(pageNumber = pageNum, imageBytes = imageBytes)
            }

            val hasMorePages = actualEndPage < totalPages

            logger.debug("Rendered pages $actualStartPage-$actualEndPage of $totalPages (hasMore=$hasMorePages)")

            DocumentImages(
                images = images,
                totalPages = totalPages,
                startPage = actualStartPage,
                endPage = actualEndPage,
                hasMorePages = hasMorePages,
                nextPageStart = if (hasMorePages) actualEndPage + 1 else null
            )
        }
    }

    private fun wrapImageAsDocument(imageBytes: ByteArray, mimeType: String): DocumentImages {
        val pngBytes = if (mimeType == "image/png") {
            imageBytes
        } else {
            convertToPng(imageBytes)
        }

        return DocumentImages(
            images = listOf(DocumentImage(pageNumber = 1, imageBytes = pngBytes)),
            totalPages = 1,
            startPage = 1,
            endPage = 1,
            hasMorePages = false,
            nextPageStart = null
        )
    }

    private fun convertToPng(imageBytes: ByteArray): ByteArray {
        val image = ImageIO.read(imageBytes.inputStream())
            ?: throw IllegalArgumentException("Could not read image data")

        return ByteArrayOutputStream().use { baos ->
            ImageIO.write(image, "PNG", baos)
            baos.toByteArray()
        }
    }
}

class UnsupportedDocumentTypeException(message: String) : RuntimeException(message)
