package tech.dokus.peppol.provider.client

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.basicAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import tech.dokus.peppol.config.PeppolProviderConfig
import tech.dokus.peppol.provider.client.recommand.model.RecommandCreateWebhookRequest
import tech.dokus.peppol.provider.client.recommand.model.RecommandCreateWebhookResponse
import tech.dokus.peppol.provider.client.recommand.model.RecommandDeleteWebhookResponse
import tech.dokus.peppol.provider.client.recommand.model.RecommandGetWebhooksResponse
import tech.dokus.peppol.provider.client.recommand.model.RecommandUpdateWebhookRequest
import tech.dokus.peppol.provider.client.recommand.model.RecommandUpdateWebhookResponse
import tech.dokus.peppol.provider.client.recommand.model.RecommandWebhook

class RecommandWebhooksClient(
    private val httpClient: HttpClient
) {
    private val baseUrl = PeppolProviderConfig.Recommand.baseUrl

    suspend fun listWebhooks(
        apiKey: String,
        apiSecret: String,
        companyId: String? = null
    ): Result<List<RecommandWebhook>> = runCatching {
        val response = httpClient.get("$baseUrl/api/v1/webhooks") {
            basicAuth(apiKey, apiSecret)
            companyId?.let { parameter("companyId", it) }
        }

        if (response.status.value == 401) {
            throw RecommandUnauthorizedException(response.status.value, response.bodyAsText())
        }

        if (!response.status.isSuccess()) {
            throw RecommandApiException(response.status.value, response.bodyAsText())
        }

        response.body<RecommandGetWebhooksResponse>().webhooks
    }

    suspend fun createWebhook(
        apiKey: String,
        apiSecret: String,
        url: String,
        companyId: String
    ): Result<RecommandWebhook> = runCatching {
        val response = httpClient.post("$baseUrl/api/v1/webhooks") {
            basicAuth(apiKey, apiSecret)
            contentType(ContentType.Application.Json)
            setBody(RecommandCreateWebhookRequest(url = url, companyId = companyId))
        }

        if (response.status.value == 401) {
            throw RecommandUnauthorizedException(response.status.value, response.bodyAsText())
        }

        if (!response.status.isSuccess()) {
            throw RecommandApiException(response.status.value, response.bodyAsText())
        }

        response.body<RecommandCreateWebhookResponse>().webhook
    }

    suspend fun updateWebhook(
        apiKey: String,
        apiSecret: String,
        webhookId: String,
        url: String,
        companyId: String
    ): Result<RecommandWebhook> = runCatching {
        val response = httpClient.put("$baseUrl/api/v1/webhooks/$webhookId") {
            basicAuth(apiKey, apiSecret)
            contentType(ContentType.Application.Json)
            setBody(RecommandUpdateWebhookRequest(url = url, companyId = companyId))
        }

        if (response.status.value == 401) {
            throw RecommandUnauthorizedException(response.status.value, response.bodyAsText())
        }

        if (!response.status.isSuccess()) {
            throw RecommandApiException(response.status.value, response.bodyAsText())
        }

        response.body<RecommandUpdateWebhookResponse>().webhook
    }

    suspend fun deleteWebhook(
        apiKey: String,
        apiSecret: String,
        webhookId: String
    ): Result<Boolean> = runCatching {
        val response = httpClient.delete("$baseUrl/api/v1/webhooks/$webhookId") {
            basicAuth(apiKey, apiSecret)
        }

        if (response.status.value == 401) {
            throw RecommandUnauthorizedException(response.status.value, response.bodyAsText())
        }

        if (!response.status.isSuccess()) {
            throw RecommandApiException(response.status.value, response.bodyAsText())
        }

        response.body<RecommandDeleteWebhookResponse>().success
    }
}
