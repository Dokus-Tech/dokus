package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.utils.decodeJwtPayload
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class JwtTokenDataSchema(
    @SerialName("sub")
    val id: String,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    @SerialName("email")
    val email: String,
    @SerialName("roles")
    val roles: Map<String, Role>
) {
    companion object {
        fun from(token: String): Result<JwtTokenDataSchema> {
            return runCatching { decodeJwtPayload<JwtTokenDataSchema>(token) }
        }
    }
}

/**
 * Generic paginated response wrapper.
 * items: list of data items
 * total: total number of items matching the query
 * page: current page number (1-based)
 * size: number of items per page
 * pages: total number of pages (nullable)
 */
@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    val size: Int,
    val pages: Int? = null
)

/**
 * Types of processes that can be tracked.
 */
@Serializable
enum class ProcessType {
    document_upload,
    document_extraction,
    transaction_matching,
    bulk_processing
}

/**
 * Process information returned by upload/extraction endpoints.
 */
@Serializable
data class Process(
    val id: String,
    @SerialName("company_id") val companyId: String,
    @SerialName("user_id") val userId: String? = null,
    val type: ProcessType,
    val status: ProcessStatus,
    @SerialName("input_data") val inputData: Map<String, JsonElement>? = null,
    @SerialName("result_data") val resultData: Map<String, JsonElement>? = null,
    @SerialName("error_message") val errorMessage: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

/**
 * Result details for document upload.
 */
@Serializable
data class DocumentUploadResult(
    val process: Process,
    @SerialName("uploaded_files") val uploadedFiles: List<String>
)

/**
 * Response schema for document upload endpoint.
 */
@Serializable
data class DocumentUploadResponse(
    val success: Boolean,
    val message: String,
    val result: DocumentUploadResult? = null,
    @SerialName("process_id") val processId: String
)

/**
 * Result details for transaction upload.
 */
@Serializable
data class TransactionUploadResult(
    val process: Process,
    @SerialName("uploaded_files") val uploadedFiles: List<String>,
    @SerialName("transactions_created") val transactionsCreated: List<String>,
    @SerialName("total_transactions") val totalTransactions: Int
)

/**
 * Response schema for transaction upload endpoint.
 */
@Serializable
data class TransactionUploadResponse(
    val success: Boolean,
    val message: String,
    val result: TransactionUploadResult? = null,
    @SerialName("process_id") val processId: String
)