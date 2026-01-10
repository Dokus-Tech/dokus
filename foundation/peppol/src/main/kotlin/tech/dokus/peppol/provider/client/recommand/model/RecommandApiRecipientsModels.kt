package tech.dokus.peppol.provider.client.recommand.model

import kotlinx.serialization.Serializable

/**
 * Request body for verifying whether a recipient is registered in Peppol.
 *
 * Used in:
 * - `POST /api/v1/verify`
 */
@Serializable
data class RecommandVerifyRecipientRequest(
    val peppolAddress: String,
)

/**
 * Response for verifying whether a recipient is registered in Peppol.
 *
 * Returned by:
 * - `POST /api/v1/verify`
 */
@Serializable
data class RecommandVerifyRecipientResponse(
    val success: Boolean,
    val isValid: Boolean,
    val smpUrl: String,
    val serviceMetadataReferences: List<String>,
    val smpHostnames: List<String>,
)

/**
 * Request body for verifying whether a recipient supports a specific document type.
 *
 * Used in:
 * - `POST /api/v1/verify-document-support`
 */
@Serializable
data class RecommandVerifyDocumentSupportRequest(
    val peppolAddress: String,
    val documentType: String,
)

/**
 * Response for verifying whether a recipient supports a specific document type.
 *
 * Returned by:
 * - `POST /api/v1/verify-document-support`
 */
@Serializable
data class RecommandVerifyDocumentSupportResponse(
    val success: Boolean,
    val isValid: Boolean,
    val smpUrl: String,
)

/**
 * Request body for searching the Peppol directory.
 *
 * Used in:
 * - `POST /api/v1/search-peppol-directory`
 */
@Serializable
data class RecommandSearchPeppolDirectoryRequest(
    val query: String,
)

/**
 * Directory search result entry.
 *
 * Used in:
 * - `POST /api/v1/search-peppol-directory` response field `results[]`
 */
@Serializable
data class RecommandPeppolDirectoryResult(
    val peppolAddress: String,
    val name: String,
    val supportedDocumentTypes: List<String>,
)

/**
 * Response for searching the Peppol directory.
 *
 * Returned by:
 * - `POST /api/v1/search-peppol-directory`
 */
@Serializable
data class RecommandSearchPeppolDirectoryResponse(
    val success: Boolean,
    val results: List<RecommandPeppolDirectoryResult>,
)

