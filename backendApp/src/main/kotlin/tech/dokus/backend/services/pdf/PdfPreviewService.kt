package tech.dokus.backend.services.pdf

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentPagePreviewDto
import tech.dokus.domain.model.DocumentPagesResponse
import tech.dokus.foundation.ktor.storage.DocumentStorageService
import tech.dokus.foundation.ktor.storage.ObjectStorage
import tech.dokus.foundation.ktor.utils.loggerFor
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * Service for rendering PDF pages to PNG images with MinIO caching.
 *
 * Compose Multiplatform cannot render PDFs directly, so this service
 * provides pre-rendered page images via authenticated API endpoints.
 *
 * Caching:
 * - Rendered pages are cached in MinIO at: pdf_previews/{tenantId}/{documentId}/dpi-{dpi}/page-{page}.png
 * - Cache is checked before rendering; repeat requests serve cached images
 * - No cache invalidation needed (PDFs are immutable)
 *
 * MVP Note: Returns ByteArray for simplicity.
 * Follow-up: Stream from MinIO + streaming response to avoid memory spikes on large pages.
 */
class PdfPreviewService(
    private val objectStorage: ObjectStorage,
    private val documentStorage: DocumentStorageService
) {
    private val logger = loggerFor()

    companion object {
        private const val CACHE_PREFIX = "pdf_previews"
        private const val MIN_DPI = 72
        private const val MAX_DPI = 300
        private const val DEFAULT_DPI = 150
        private const val MIN_MAX_PAGES = 1
        private const val MAX_MAX_PAGES = 50
        private const val DEFAULT_MAX_PAGES = 10
    }

    /**
     * Clamp DPI to safe range (72-300).
     */
    fun clampDpi(dpi: Int): Int = dpi.coerceIn(MIN_DPI, MAX_DPI)

    /**
     * Clamp maxPages to safe range (1-50).
     */
    fun clampMaxPages(maxPages: Int): Int = maxPages.coerceIn(MIN_MAX_PAGES, MAX_MAX_PAGES)

    /**
     * Generate cache key for a rendered page.
     * Format: pdf_previews/{tenantId}/{documentId}/dpi-{dpi}/page-{page}.png
     */
    fun generateCacheKey(tenantId: TenantId, documentId: DocumentId, dpi: Int, page: Int): String {
        return "$CACHE_PREFIX/$tenantId/$documentId/dpi-$dpi/page-$page.png"
    }

    /**
     * Get total page count for a PDF document.
     *
     * @param storageKey The storage key for the PDF document
     * @return Total number of pages in the PDF
     * @throws IllegalArgumentException if document is not a valid PDF
     */
    suspend fun getPageCount(storageKey: String): Int = withContext(Dispatchers.IO) {
        val pdfBytes = documentStorage.downloadDocument(storageKey)
        Loader.loadPDF(pdfBytes).use { document ->
            document.numberOfPages
        }
    }

    /**
     * Get a rendered page image, using cache if available.
     *
     * @param tenantId Tenant owning the document (for cache key)
     * @param documentId Document ID (for cache key)
     * @param storageKey Storage key to fetch original PDF
     * @param page 1-based page number
     * @param dpi Resolution for rendering
     * @return PNG image bytes
     * @throws IllegalArgumentException if page is out of bounds
     */
    suspend fun getPageImage(
        tenantId: TenantId,
        documentId: DocumentId,
        storageKey: String,
        page: Int,
        dpi: Int
    ): ByteArray {
        val clampedDpi = clampDpi(dpi)
        val cacheKey = generateCacheKey(tenantId, documentId, clampedDpi, page)

        // Check cache first
        if (objectStorage.exists(cacheKey)) {
            logger.debug("Cache hit for page {} of document {}", page, documentId)
            return objectStorage.get(cacheKey)
        }

        logger.debug("Cache miss for page {} of document {}, rendering...", page, documentId)

        // Render page
        val imageBytes = withContext(Dispatchers.IO) {
            val pdfBytes = documentStorage.downloadDocument(storageKey)
            renderPage(pdfBytes, page, clampedDpi)
        }

        // Store in cache
        try {
            objectStorage.put(cacheKey, imageBytes, "image/png")
            logger.debug("Cached page {} of document {}", page, documentId)
        } catch (e: Exception) {
            // Log but don't fail - caching is best-effort
            logger.warn("Failed to cache page {} of document {}: {}", page, documentId, e.message)
        }

        return imageBytes
    }

    /**
     * List available pages for a document with preview URLs.
     *
     * @param tenantId Tenant owning the document
     * @param documentId Document ID
     * @param storageKey Storage key to fetch original PDF
     * @param dpi Resolution for rendered pages
     * @param maxPages Maximum pages to include in response
     * @return DocumentPagesResponse with page metadata and URLs
     */
    suspend fun listPages(
        tenantId: TenantId,
        documentId: DocumentId,
        storageKey: String,
        dpi: Int,
        maxPages: Int
    ): DocumentPagesResponse {
        val clampedDpi = clampDpi(dpi)
        val clampedMaxPages = clampMaxPages(maxPages)

        val totalPages = getPageCount(storageKey)
        val renderedPages = minOf(totalPages, clampedMaxPages)

        val pages = (1..renderedPages).map { pageNum ->
            DocumentPagePreviewDto(
                page = pageNum,
                imageUrl = "/api/v1/documents/$documentId/pages/$pageNum.png?dpi=$clampedDpi"
            )
        }

        return DocumentPagesResponse(
            documentId = documentId,
            dpi = clampedDpi,
            totalPages = totalPages,
            renderedPages = renderedPages,
            pages = pages
        )
    }

    /**
     * Render a specific page from PDF bytes to PNG.
     *
     * @param pdfBytes Raw PDF content
     * @param page 1-based page number
     * @param dpi Resolution for rendering
     * @return PNG image bytes
     * @throws IllegalArgumentException if page is out of bounds
     */
    private fun renderPage(pdfBytes: ByteArray, page: Int, dpi: Int): ByteArray {
        require(page >= 1) { "Page number must be >= 1, got: $page" }

        return Loader.loadPDF(pdfBytes).use { document ->
            val totalPages = document.numberOfPages
            require(page <= totalPages) {
                "Page $page is out of bounds. Document has $totalPages pages."
            }

            val renderer = PDFRenderer(document)
            // PDFBox uses 0-based page index
            val image = renderer.renderImageWithDPI(page - 1, dpi.toFloat(), ImageType.RGB)

            ByteArrayOutputStream().use { baos ->
                ImageIO.write(image, "PNG", baos)
                baos.toByteArray()
            }
        }
    }
}
