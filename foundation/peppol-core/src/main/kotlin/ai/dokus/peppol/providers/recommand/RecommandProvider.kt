package ai.dokus.peppol.providers.recommand

import ai.dokus.foundation.domain.model.RecommandDocumentsResponse
import ai.dokus.foundation.domain.model.RecommandInboxDocument
import ai.dokus.foundation.domain.model.RecommandSendResponse
import ai.dokus.foundation.domain.model.RecommandVerifyRequest
import ai.dokus.foundation.domain.model.RecommandVerifyResponse
import ai.dokus.peppol.model.PeppolDirection
import ai.dokus.peppol.model.PeppolDocumentList
import ai.dokus.peppol.model.PeppolInboxItem
import ai.dokus.peppol.model.PeppolReceivedDocument
import ai.dokus.peppol.model.PeppolSendRequest
import ai.dokus.peppol.model.PeppolSendResponse
import ai.dokus.peppol.model.PeppolVerifyResponse
import ai.dokus.peppol.provider.PeppolCredentials
import ai.dokus.peppol.provider.PeppolProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Peppol provider implementation for Recommand.eu
 *
 * Documentation: https://recommand.eu/en/docs
 * API Reference: https://peppol.recommand.eu/api-reference
 */
class RecommandProvider(
    private val httpClient: HttpClient,
    private val baseUrl: String = "https://app.recommand.eu"
) : PeppolProvider {

    private val logger = LoggerFactory.getLogger(RecommandProvider::class.java)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    override val providerId = "recommand"
    override val providerName = "Recommand.eu"

    private lateinit var credentials: RecommandCredentials

    override fun configure(credentials: PeppolCredentials) {
        require(credentials is RecommandCredentials) {
            "RecommandProvider requires RecommandCredentials, got ${credentials::class.simpleName}"
        }
        this.credentials = credentials
        logger.debug("Configured RecommandProvider for company: ${credentials.companyId}")
    }

    override suspend fun sendDocument(request: PeppolSendRequest): Result<PeppolSendResponse> = runCatching {
        logger.info("Sending document to Peppol via Recommand. Recipient: ${request.recipientPeppolId}")

        val recommandRequest = RecommandMapper.toRecommandRequest(request)

        val response = httpClient.post("$baseUrl/api/peppol/${credentials.companyId}/send") {
            contentType(ContentType.Application.Json)
            basicAuth(credentials.apiKey, credentials.apiSecret)
            setBody(recommandRequest)
        }

        val result = response.body<RecommandSendResponse>()

        if (result.success) {
            logger.info("Document sent successfully. Document ID: ${result.documentId}")
        } else {
            logger.warn("Document send failed. Errors: ${result.errors?.map { it.message }}")
        }

        RecommandMapper.fromRecommandResponse(result)
    }.onFailure { e ->
        logger.error("Failed to send document to Recommand", e)
    }

    override suspend fun verifyRecipient(peppolId: String): Result<PeppolVerifyResponse> = runCatching {
        logger.debug("Verifying Peppol participant: $peppolId")

        val response = httpClient.post("$baseUrl/api/peppol/${credentials.companyId}/verify") {
            contentType(ContentType.Application.Json)
            basicAuth(credentials.apiKey, credentials.apiSecret)
            setBody(RecommandVerifyRequest(peppolId))
        }

        val result = response.body<RecommandVerifyResponse>()
        RecommandMapper.fromRecommandVerifyResponse(result)
    }.onFailure { e ->
        logger.error("Failed to verify Peppol participant: $peppolId", e)
    }

    override suspend fun getInbox(): Result<List<PeppolInboxItem>> = runCatching {
        logger.debug("Fetching Peppol inbox for company: ${credentials.companyId}")

        val response = httpClient.get("$baseUrl/api/peppol/${credentials.companyId}/inbox") {
            basicAuth(credentials.apiKey, credentials.apiSecret)
        }

        val items = response.body<List<RecommandInboxDocument>>()
        items.map { RecommandMapper.fromRecommandInboxItem(it) }
    }.onFailure { e ->
        logger.error("Failed to fetch Peppol inbox", e)
    }

    override suspend fun getDocument(documentId: String): Result<PeppolReceivedDocument> = runCatching {
        logger.debug("Fetching Peppol document: $documentId")

        val response = httpClient.get("$baseUrl/api/peppol/${credentials.companyId}/documents/$documentId") {
            basicAuth(credentials.apiKey, credentials.apiSecret)
        }

        val item = response.body<RecommandInboxDocument>()
        val document = item.document ?: throw IllegalStateException("Document content is missing")

        RecommandMapper.fromRecommandDocument(item, document)
    }.onFailure { e ->
        logger.error("Failed to fetch Peppol document: $documentId", e)
    }

    override suspend fun markAsRead(documentId: String): Result<Unit> = runCatching {
        logger.debug("Marking document as read: $documentId")

        httpClient.post("$baseUrl/api/peppol/${credentials.companyId}/documents/$documentId/read") {
            basicAuth(credentials.apiKey, credentials.apiSecret)
        }
        Unit
    }.onFailure { e ->
        logger.error("Failed to mark document as read: $documentId", e)
    }

    override suspend fun listDocuments(
        direction: PeppolDirection?,
        limit: Int,
        offset: Int
    ): Result<PeppolDocumentList> = runCatching {
        logger.debug("Listing Peppol documents. Direction: $direction")

        val response = httpClient.get("$baseUrl/api/peppol/${credentials.companyId}/documents") {
            basicAuth(credentials.apiKey, credentials.apiSecret)
            direction?.let {
                parameter("direction", when (it) {
                    PeppolDirection.INBOUND -> "received"
                    PeppolDirection.OUTBOUND -> "sent"
                })
            }
            parameter("limit", limit)
            parameter("offset", offset)
        }

        val result = response.body<RecommandDocumentsResponse>()
        RecommandMapper.fromRecommandDocumentsResponse(result)
    }.onFailure { e ->
        logger.error("Failed to list Peppol documents", e)
    }

    override suspend fun testConnection(): Result<Boolean> = runCatching {
        logger.debug("Testing Recommand connection for company: ${credentials.companyId}")

        // Try to list documents with limit 1 to test credentials
        val response = httpClient.get("$baseUrl/api/peppol/${credentials.companyId}/documents") {
            basicAuth(credentials.apiKey, credentials.apiSecret)
            parameter("limit", 1)
        }

        // If we get here without exception, connection is successful
        response.status.value in 200..299
    }.onFailure { e ->
        logger.error("Recommand connection test failed", e)
    }

    override fun serializeRequest(request: PeppolSendRequest): String {
        val recommandRequest = RecommandMapper.toRecommandRequest(request)
        return json.encodeToString(recommandRequest)
    }
}
