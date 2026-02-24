package tech.dokus.peppol.provider

import tech.dokus.domain.enums.PeppolTransmissionDirection
import tech.dokus.peppol.model.PeppolDocumentList
import tech.dokus.peppol.model.PeppolInboxItem
import tech.dokus.peppol.model.PeppolReceivedDocument
import tech.dokus.peppol.model.PeppolSendRequest
import tech.dokus.peppol.model.PeppolSendResponse
import tech.dokus.peppol.model.PeppolVerifyResponse

/**
 * Generic interface for Peppol Access Point providers.
 *
 * Implementations handle communication with specific providers like:
 * - Recommand.eu
 * - Storecove
 * - Basware
 * - etc.
 */
interface PeppolProvider {
    /** Provider identifier (e.g., "recommand", "storecove") */
    val providerId: String

    /** Human-readable provider name */
    val providerName: String

    /**
     * Configure the provider with tenant-specific credentials.
     * Must be called before any other operations.
     */
    fun configure(credentials: PeppolCredentials)

    /**
     * Send a document via Peppol network.
     *
     * @param request The document to send
     * @return Result containing the send response or error
     */
    suspend fun sendDocument(
        request: PeppolSendRequest,
        idempotencyKey: String? = null
    ): Result<PeppolSendResponse>

    /**
     * Verify if a recipient is registered on the Peppol network.
     *
     * @param peppolId The recipient's Peppol ID (format: scheme:identifier)
     * @return Result containing verification response
     */
    suspend fun verifyRecipient(peppolId: String): Result<PeppolVerifyResponse>

    /**
     * Get unread documents from the inbox.
     *
     * @return Result containing list of unread inbox items
     */
    suspend fun getInbox(): Result<List<PeppolInboxItem>>

    /**
     * Get full document content by ID.
     *
     * @param documentId The provider-specific document ID
     * @return Result containing the full document
     */
    suspend fun getDocument(documentId: String): Result<PeppolReceivedDocument>

    /**
     * Mark a document as read.
     *
     * @param documentId The provider-specific document ID
     */
    suspend fun markAsRead(documentId: String): Result<Unit>

    /**
     * List all documents with pagination.
     *
     * @param direction Filter by direction (INBOUND/OUTBOUND)
     * @param limit Number of documents to return
     * @param offset Pagination offset
     * @param isUnread Filter by read status: null = all, true = unread only, false = read only
     * @return Result containing paginated document list
     */
    suspend fun listDocuments(
        direction: PeppolTransmissionDirection? = null,
        limit: Int = 50,
        offset: Int = 0,
        isUnread: Boolean? = null
    ): Result<PeppolDocumentList>

    /**
     * Test the connection and validate credentials.
     *
     * @return Result indicating if connection is successful
     */
    suspend fun testConnection(): Result<Boolean>

    /**
     * Serialize a send request to JSON for logging/audit.
     */
    fun serializeRequest(request: PeppolSendRequest): String
}
