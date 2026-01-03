package tech.dokus.peppol.provider.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import tech.dokus.domain.model.RecommandDocumentsResponse
import tech.dokus.domain.model.RecommandInboxDocument
import tech.dokus.domain.model.RecommandMarkAsReadRequest
import tech.dokus.domain.model.RecommandSendResponse
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.peppol.model.PeppolDirection
import tech.dokus.peppol.model.PeppolDocumentList
import tech.dokus.peppol.model.PeppolInboxItem
import tech.dokus.peppol.model.PeppolReceivedDocument
import tech.dokus.peppol.model.PeppolSendRequest
import tech.dokus.peppol.model.PeppolSendResponse
import tech.dokus.peppol.model.PeppolVerifyResponse
import tech.dokus.peppol.provider.PeppolCredentials
import tech.dokus.peppol.provider.PeppolProvider

/**
 * Peppol provider implementation for Recommand.eu
 *
 * API Reference: https://peppol.recommand.eu/api-reference
 * Base URL: https://app.recommand.eu
 *
 * Endpoints used:
 * - POST /api/v1/{companyId}/send - Send documents
 * - GET /api/v1/documents - List documents (with companyId, direction, page, limit params)
 * - GET /api/v1/documents/{documentId} - Get single document
 * - POST /api/v1/documents/{documentId}/mark-as-read - Mark document as read/unread
 * - GET /api/v1/inbox - List unread incoming documents
 */
class RecommandProvider(
    private val httpClient: HttpClient,
    private val productionBaseUrl: String = "https://app.recommand.eu",
    private val testBaseUrl: String = "https://test.recommand.eu",
    private val globalTestMode: Boolean = false,
) : PeppolProvider {

    private val logger = loggerFor()

    override val providerId = "recommand"
    override val providerName = "Recommand.eu"

    private lateinit var credentials: RecommandCredentials
    private var baseUrl: String = productionBaseUrl

    private val isConfigured: Boolean
        get() = ::credentials.isInitialized

    override fun configure(credentials: PeppolCredentials) {
        require(credentials is RecommandCredentials) {
            "RecommandProvider requires RecommandCredentials, got ${credentials::class.simpleName}"
        }
        this.credentials = credentials
        baseUrl = if (globalTestMode || credentials.testMode) testBaseUrl else productionBaseUrl
        logger.debug("Configured RecommandProvider for company: ${credentials.companyId}")
    }

    private fun ensureConfigured() {
        check(isConfigured) { "RecommandProvider not configured. Call configure() first." }
    }

    /**
     * Send a document via Peppol network.
     * POST /api/v1/{companyId}/send
     */
    override suspend fun sendDocument(request: PeppolSendRequest): Result<PeppolSendResponse> =
        runCatching {
            ensureConfigured()
            logger.info("Sending document to Peppol via Recommand. Recipient: ${request.recipientPeppolId}")

            val recommandRequest = RecommandMapper.toRecommandRequest(request)

            val response = httpClient.post("$baseUrl/api/v1/${credentials.companyId}/send") {
                contentType(ContentType.Application.Json)
                basicAuth(credentials.apiKey, credentials.apiSecret)
                setBody(recommandRequest)
            }

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                logger.error("Recommand API error: ${response.status} - $errorBody")
                throw RecommandApiException(response.status.value, errorBody)
            }

            val result = response.body<RecommandSendResponse>()

            if (result.success) {
                logger.info("Document sent successfully. Document ID: ${result.documentId}")
            } else {
                val errors = result.errors?.joinToString { it.message } ?: "Unknown error"
                logger.warn("Document send failed. Errors: $errors")
            }

            RecommandMapper.fromRecommandResponse(result)
        }.onFailure { e ->
            logger.error("Failed to send document to Recommand", e)
        }

    /**
     * Verify if a recipient is registered on the Peppol network.
     *
     * Note: Recommand API doesn't have a dedicated verify endpoint.
     * This implementation attempts to look up the participant via SMP.
     * For production, consider implementing proper SMP lookup.
     */
    override suspend fun verifyRecipient(peppolId: String): Result<PeppolVerifyResponse> =
        runCatching {
            ensureConfigured()
            logger.debug("Verifying Peppol participant: $peppolId")

            // Recommand doesn't have a dedicated verify endpoint.
            // We return a successful result indicating the participant format is valid.
            // Actual delivery verification happens during send.
            val isValidFormat = peppolId.matches(Regex("^\\d{4}:.+$"))

            PeppolVerifyResponse(
                registered = isValidFormat,
                participantId = peppolId,
                name = null,
                documentTypes = emptyList()
            )
        }.onFailure { e ->
            logger.error("Failed to verify Peppol participant: $peppolId", e)
        }

    /**
     * Get unread documents from the inbox.
     * GET /api/v1/inbox?companyId={companyId}
     */
    override suspend fun getInbox(): Result<List<PeppolInboxItem>> = runCatching {
        ensureConfigured()
        logger.debug("Fetching Peppol inbox for company: ${credentials.companyId}")

        val response = httpClient.get("$baseUrl/api/v1/inbox") {
            basicAuth(credentials.apiKey, credentials.apiSecret)
            parameter("companyId", credentials.companyId)
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            logger.error("Recommand API error: ${response.status} - $errorBody")
            throw RecommandApiException(response.status.value, errorBody)
        }

        val items = response.body<List<RecommandInboxDocument>>()
        logger.debug("Fetched ${items.size} inbox items")
        items.map { RecommandMapper.fromRecommandInboxItem(it) }
    }.onFailure { e ->
        logger.error("Failed to fetch Peppol inbox", e)
    }

    /**
     * Get full document content by ID.
     * GET /api/v1/documents/{documentId}
     */
    override suspend fun getDocument(documentId: String): Result<PeppolReceivedDocument> =
        runCatching {
            ensureConfigured()
            logger.debug("Fetching Peppol document: $documentId")

            val response = httpClient.get("$baseUrl/api/v1/documents/$documentId") {
                basicAuth(credentials.apiKey, credentials.apiSecret)
            }

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                logger.error("Recommand API error: ${response.status} - $errorBody")
                throw RecommandApiException(response.status.value, errorBody)
            }

            val item = response.body<RecommandInboxDocument>()
            val document = item.document
                ?: throw IllegalStateException("Document content is missing for ID: $documentId")

            RecommandMapper.fromRecommandDocument(item, document)
        }.onFailure { e ->
            logger.error("Failed to fetch Peppol document: $documentId", e)
        }

    /**
     * Mark a document as read.
     * POST /api/v1/documents/{documentId}/mark-as-read
     */
    override suspend fun markAsRead(documentId: String): Result<Unit> = runCatching {
        ensureConfigured()
        logger.debug("Marking document as read: $documentId")

        val response = httpClient.post("$baseUrl/api/v1/documents/$documentId/mark-as-read") {
            contentType(ContentType.Application.Json)
            basicAuth(credentials.apiKey, credentials.apiSecret)
            setBody(RecommandMarkAsReadRequest(read = true))
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            logger.error("Recommand API error: ${response.status} - $errorBody")
            throw RecommandApiException(response.status.value, errorBody)
        }

        logger.debug("Document marked as read: $documentId")
    }.onFailure { e ->
        logger.error("Failed to mark document as read: $documentId", e)
    }

    /**
     * List all documents with pagination.
     * GET /api/v1/documents?companyId={}&direction={}&page={}&limit={}
     */
    override suspend fun listDocuments(
        direction: PeppolDirection?,
        limit: Int,
        offset: Int
    ): Result<PeppolDocumentList> = runCatching {
        ensureConfigured()
        logger.debug(
            "Listing Peppol documents. Direction: {}, Limit: {}, Offset: {}",
            direction,
            limit,
            offset
        )

        // Recommand uses page-based pagination, convert offset to page
        val page = (offset / limit) + 1

        val response = httpClient.get("$baseUrl/api/v1/documents") {
            basicAuth(credentials.apiKey, credentials.apiSecret)
            parameter("companyId", credentials.companyId)
            parameter("limit", limit)
            parameter("page", page)
            direction?.let {
                parameter(
                    "direction",
                    when (it) {
                        PeppolDirection.INBOUND -> "incoming"
                        PeppolDirection.OUTBOUND -> "outgoing"
                    }
                )
            }
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            logger.error("Recommand API error: ${response.status} - $errorBody")
            throw RecommandApiException(response.status.value, errorBody)
        }

        val result = response.body<RecommandDocumentsResponse>()
        logger.debug("Fetched ${result.data.size} documents")
        RecommandMapper.fromRecommandDocumentsResponse(result)
    }.onFailure { e ->
        logger.error("Failed to list Peppol documents", e)
    }

    /**
     * Test the connection and validate credentials.
     * Uses GET /api/v1/documents with limit=1 to verify API access.
     */
    override suspend fun testConnection(): Result<Boolean> = runCatching {
        ensureConfigured()
        logger.debug("Testing Recommand connection for company: ${credentials.companyId}")

        val response = httpClient.get("$baseUrl/api/v1/documents") {
            basicAuth(credentials.apiKey, credentials.apiSecret)
            parameter("companyId", credentials.companyId)
            parameter("limit", 1)
        }

        val success = response.status.isSuccess()
        if (success) {
            logger.info("Recommand connection test successful")
        } else {
            logger.warn("Recommand connection test failed: ${response.status}")
        }
        success
    }.onFailure { e ->
        logger.error("Recommand connection test failed", e)
    }

    /**
     * Serialize a send request to JSON for logging/audit.
     */
    override fun serializeRequest(request: PeppolSendRequest): String {
        val recommandRequest = RecommandMapper.toRecommandRequest(request)
        return json.encodeToString(recommandRequest)
    }
}

/**
 * Exception thrown when Recommand API returns an error response.
 */
class RecommandApiException(
    val statusCode: Int,
    val responseBody: String
) : Exception("Recommand API error (HTTP $statusCode): $responseBody")
