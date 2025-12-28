package tech.dokus.domain.model

import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.DocumentId

/**
 * Response for listing available PDF pages for a document.
 *
 * Used by clients to get page preview URLs for PDF documents.
 * Compose Multiplatform cannot render PDFs directly, so the backend
 * provides rendered page images via authenticated endpoints.
 */
@Serializable
data class DocumentPagesResponse(
    /** The document ID these pages belong to */
    val documentId: DocumentId,

    /** The DPI used for rendering these page previews */
    val dpi: Int,

    /** Total number of pages in the PDF */
    val totalPages: Int,

    /** Number of pages actually returned (limited by maxPages) */
    val renderedPages: Int,

    /** List of page preview metadata with URLs */
    val pages: List<DocumentPagePreviewDto>
)

/**
 * Metadata for a single page preview.
 */
@Serializable
data class DocumentPagePreviewDto(
    /** 1-based page number */
    val page: Int,

    /**
     * Relative URL to fetch the page image.
     * Format: "/api/v1/documents/{id}/pages/{page}.png?dpi={dpi}"
     *
     * Uses relative URLs to remain environment-agnostic and ensure
     * authentication is always applied (not direct MinIO URLs).
     */
    val imageUrl: String
)
