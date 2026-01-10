package tech.dokus.peppol.provider.client.recommand.model

import kotlinx.serialization.Serializable

/**
 * Generic error response used by many Recommand API endpoints when a request fails.
 *
 * Seen in (non-exhaustive):
 * - `POST /api/v1/{companyId}/send` (path params: `companyId`) with HTTP 400/422
 * - `GET /api/v1/companies` (query params: `enterpriseNumber`, `vatNumber`) with HTTP 500
 * - `GET /api/v1/documents` (query params: `page`, `limit`, `companyId`, `direction`, `search`, `type`, `from`, `to`, `isUnread`, `envelopeId`) with HTTP 500
 * - `GET /api/core/auth/verify` with HTTP 401/500
 */
@Serializable
data class RecommandApiErrorResponse(
    val success: Boolean = false,
    val errors: Map<String, List<String>> = emptyMap(),
)

/**
 * Generic success-only response used by endpoints that return `{ "success": true }`.
 *
 * Seen in (non-exhaustive):
 * - `DELETE /api/v1/companies/{companyId}` (path params: `companyId`)
 * - `DELETE /api/v1/documents/{documentId}` (path params: `documentId`)
 * - `POST /api/v1/documents/{documentId}/mark-as-read` (path params: `documentId`)
 * - `POST /api/v1/documents/{documentId}/labels/{labelId}` (path params: `documentId`, `labelId`)
 * - `DELETE /api/v1/documents/{documentId}/labels/{labelId}` (path params: `documentId`, `labelId`)
 */
@Serializable
data class RecommandApiSuccessResponse(
    val success: Boolean = true,
)

/**
 * Pagination block returned by list endpoints.
 *
 * Used in:
 * - `GET /api/v1/documents` (query params: `page`, `limit`, ...)
 * - `GET /api/v1/suppliers` (query params: `page`, `limit`, `search`)
 * - `GET /api/v1/customers` (query params: `page`, `limit`, `search`)
 */
@Serializable
data class RecommandApiPagination(
    val total: Long,
    val page: Int,
    val limit: Int,
    val totalPages: Int,
)

