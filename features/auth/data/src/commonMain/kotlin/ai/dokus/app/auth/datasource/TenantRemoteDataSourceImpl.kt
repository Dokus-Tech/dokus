package ai.dokus.app.auth.datasource

import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.AvatarUploadResponse
import ai.dokus.foundation.domain.model.CompanyAvatar
import ai.dokus.foundation.domain.model.CreateTenantRequest
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.TenantSettings
import ai.dokus.foundation.domain.routes.Tenants
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.onUpload
import io.ktor.client.plugins.resources.delete
import io.ktor.client.plugins.resources.get
import io.ktor.client.plugins.resources.post
import io.ktor.client.plugins.resources.put
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.uuid.ExperimentalUuidApi

/**
 * HTTP implementation of TenantRemoteDataSource.
 * Uses authenticated Ktor HttpClient with type-safe routing to communicate with the tenant service.
 */
@OptIn(ExperimentalUuidApi::class)
internal class TenantRemoteDataSourceImpl(
    private val httpClient: HttpClient,
) : TenantRemoteDataSource {

    override suspend fun listMyTenants(): Result<List<Tenant>> {
        return runCatching {
            httpClient.get(Tenants()).body()
        }
    }

    override suspend fun createTenant(request: CreateTenantRequest): Result<Tenant> {
        return runCatching {
            httpClient.post(Tenants()) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun getTenant(id: TenantId): Result<Tenant> {
        return runCatching {
            httpClient.get(Tenants.Id(id = id.value.toString())).body()
        }
    }

    override suspend fun getTenantSettings(): Result<TenantSettings> {
        return runCatching {
            httpClient.get(Tenants.Settings()).body()
        }
    }

    override suspend fun updateTenantSettings(settings: TenantSettings): Result<Unit> {
        return runCatching {
            httpClient.put(Tenants.Settings()) {
                contentType(ContentType.Application.Json)
                setBody(settings)
            }
        }
    }

    // ===== Avatar Operations =====

    override suspend fun uploadAvatar(
        imageBytes: ByteArray,
        filename: String,
        contentType: String,
        onProgress: (Float) -> Unit
    ): Result<AvatarUploadResponse> {
        return runCatching {
            httpClient.submitFormWithBinaryData(
                url = "/api/v1/tenants/avatar",
                formData = formData {
                    append(
                        key = "file",
                        value = imageBytes,
                        headers = Headers.build {
                            append(
                                HttpHeaders.ContentDisposition,
                                "form-data; name=\"file\"; filename=\"$filename\""
                            )
                            append(HttpHeaders.ContentType, contentType)
                        }
                    )
                }
            ) {
                onUpload { bytesSentTotal, contentLength ->
                    val progress = if (contentLength != null && contentLength > 0) {
                        bytesSentTotal.toFloat() / contentLength.toFloat()
                    } else {
                        0f
                    }
                    onProgress(progress.coerceIn(0f, 1f))
                }
            }.body()
        }
    }

    override suspend fun getAvatar(): Result<CompanyAvatar?> {
        return runCatching {
            val response: HttpResponse = httpClient.get(Tenants.Avatar())
            if (response.status == HttpStatusCode.NotFound) {
                null
            } else {
                response.body()
            }
        }
    }

    override suspend fun deleteAvatar(): Result<Unit> {
        return runCatching {
            httpClient.delete(Tenants.Avatar())
        }
    }
}
