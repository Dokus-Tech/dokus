package ai.dokus.peppol.backend.client

import ai.dokus.foundation.domain.model.RecommandDocumentsResponse
import ai.dokus.foundation.domain.model.RecommandInboxDocument
import ai.dokus.foundation.domain.model.RecommandSendRequest
import ai.dokus.foundation.domain.model.RecommandSendResponse
import ai.dokus.foundation.domain.model.RecommandVerifyRequest
import ai.dokus.foundation.domain.model.RecommandVerifyResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * HTTP client for Recommand Peppol API.
 * Documentation: https://recommand.eu/en/docs
 */
class RecommandClient(
    private val httpClient: HttpClient,
    private val baseUrl: String = "https://app.recommand.eu"
) {
    private val logger = LoggerFactory.getLogger(RecommandClient::class.java)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Send a document via Peppol network.
     *
     * @param companyId The Recommand company ID
     * @param apiKey The API key
     * @param apiSecret The API secret
     * @param request The send request containing recipient and document
     * @return The send response with document ID or errors
     */
    suspend fun sendDocument(
        companyId: String,
        apiKey: String,
        apiSecret: String,
        request: RecommandSendRequest
    ): Result<RecommandSendResponse> = runCatching {
        logger.info("Sending document to Peppol via Recommand. Company: $companyId, Recipient: ${request.recipient}")

        val response = httpClient.post("$baseUrl/api/peppol/$companyId/send") {
            contentType(ContentType.Application.Json)
            basicAuth(apiKey, apiSecret)
            setBody(request)
        }

        val result = response.body<RecommandSendResponse>()

        if (result.success) {
            logger.info("Document sent successfully. Document ID: ${result.documentId}")
        } else {
            logger.warn("Document send failed. Errors: ${result.errors?.map { it.message }}")
        }

        result
    }.onFailure { e ->
        logger.error("Failed to send document to Recommand", e)
    }

    /**
     * Verify if a recipient is registered on the Peppol network.
     *
     * @param companyId The Recommand company ID
     * @param apiKey The API key
     * @param apiSecret The API secret
     * @param participantId The Peppol participant ID to verify (format: scheme:identifier)
     * @return The verify response indicating if the recipient is registered
     */
    suspend fun verifyRecipient(
        companyId: String,
        apiKey: String,
        apiSecret: String,
        participantId: String
    ): Result<RecommandVerifyResponse> = runCatching {
        logger.debug("Verifying Peppol participant: $participantId")

        val response = httpClient.post("$baseUrl/api/peppol/$companyId/verify") {
            contentType(ContentType.Application.Json)
            basicAuth(apiKey, apiSecret)
            setBody(RecommandVerifyRequest(participantId))
        }

        response.body<RecommandVerifyResponse>()
    }.onFailure { e ->
        logger.error("Failed to verify Peppol participant: $participantId", e)
    }

    /**
     * Get unread documents from the inbox.
     *
     * @param companyId The Recommand company ID
     * @param apiKey The API key
     * @param apiSecret The API secret
     * @return List of unread inbox documents
     */
    suspend fun getInbox(
        companyId: String,
        apiKey: String,
        apiSecret: String
    ): Result<List<RecommandInboxDocument>> = runCatching {
        logger.debug("Fetching Peppol inbox for company: $companyId")

        val response = httpClient.get("$baseUrl/api/peppol/$companyId/inbox") {
            basicAuth(apiKey, apiSecret)
        }

        response.body<List<RecommandInboxDocument>>()
    }.onFailure { e ->
        logger.error("Failed to fetch Peppol inbox", e)
    }

    /**
     * Get a specific document by ID.
     *
     * @param companyId The Recommand company ID
     * @param apiKey The API key
     * @param apiSecret The API secret
     * @param documentId The document ID to fetch
     * @return The inbox document with full content
     */
    suspend fun getDocument(
        companyId: String,
        apiKey: String,
        apiSecret: String,
        documentId: String
    ): Result<RecommandInboxDocument> = runCatching {
        logger.debug("Fetching Peppol document: $documentId")

        val response = httpClient.get("$baseUrl/api/peppol/$companyId/documents/$documentId") {
            basicAuth(apiKey, apiSecret)
        }

        response.body<RecommandInboxDocument>()
    }.onFailure { e ->
        logger.error("Failed to fetch Peppol document: $documentId", e)
    }

    /**
     * List all documents with pagination.
     *
     * @param companyId The Recommand company ID
     * @param apiKey The API key
     * @param apiSecret The API secret
     * @param direction Filter by direction (sent/received)
     * @param limit Number of documents to return
     * @param offset Pagination offset
     * @return Paginated list of documents
     */
    suspend fun listDocuments(
        companyId: String,
        apiKey: String,
        apiSecret: String,
        direction: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<RecommandDocumentsResponse> = runCatching {
        logger.debug("Listing Peppol documents. Company: $companyId, Direction: $direction")

        val response = httpClient.get("$baseUrl/api/peppol/$companyId/documents") {
            basicAuth(apiKey, apiSecret)
            direction?.let { parameter("direction", it) }
            parameter("limit", limit)
            parameter("offset", offset)
        }

        response.body<RecommandDocumentsResponse>()
    }.onFailure { e ->
        logger.error("Failed to list Peppol documents", e)
    }

    /**
     * Mark a document as read.
     *
     * @param companyId The Recommand company ID
     * @param apiKey The API key
     * @param apiSecret The API secret
     * @param documentId The document ID to mark as read
     */
    suspend fun markAsRead(
        companyId: String,
        apiKey: String,
        apiSecret: String,
        documentId: String
    ): Result<Unit> = runCatching {
        logger.debug("Marking document as read: $documentId")

        httpClient.post("$baseUrl/api/peppol/$companyId/documents/$documentId/read") {
            basicAuth(apiKey, apiSecret)
        }
        Unit
    }.onFailure { e ->
        logger.error("Failed to mark document as read: $documentId", e)
    }

    /**
     * Serialize request to JSON string for logging/audit.
     */
    fun serializeRequest(request: RecommandSendRequest): String {
        return json.encodeToString(request)
    }
}
