package ai.dokus.app.auth.datasource

import ai.dokus.foundation.domain.ids.ClientId
import ai.dokus.foundation.domain.model.ClientDto
import ai.dokus.foundation.domain.model.ClientStats
import ai.dokus.foundation.domain.model.CreateClientRequest
import ai.dokus.foundation.domain.model.UpdateClientRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * HTTP-based implementation of ClientRemoteDataSource
 * Uses Ktor HttpClient to communicate with the client management API
 */
internal class ClientRemoteDataSourceImpl(
    private val httpClient: HttpClient
) : ClientRemoteDataSource {

    override suspend fun createClient(request: CreateClientRequest): Result<ClientDto> {
        return runCatching {
            httpClient.post("/api/v1/clients") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun getClient(id: ClientId): Result<ClientDto> {
        return runCatching {
            httpClient.get("/api/v1/clients/$id").body()
        }
    }

    override suspend fun listClients(
        search: String?,
        activeOnly: Boolean
    ): Result<List<ClientDto>> {
        return runCatching {
            httpClient.get("/api/v1/clients") {
                search?.let { parameter("search", it) }
                parameter("activeOnly", activeOnly)
            }.body()
        }
    }

    override suspend fun updateClient(
        id: ClientId,
        request: UpdateClientRequest
    ): Result<ClientDto> {
        return runCatching {
            httpClient.put("/api/v1/clients/$id") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
        }
    }

    override suspend fun deleteClient(id: ClientId): Result<Unit> {
        return runCatching {
            httpClient.delete("/api/v1/clients/$id").body()
        }
    }

    override suspend fun findClientByPeppolId(peppolId: String): Result<ClientDto?> {
        return runCatching {
            httpClient.get("/api/v1/clients/by-peppol/$peppolId").body()
        }
    }

    override suspend fun getClientStats(): Result<ClientStats> {
        return runCatching {
            httpClient.get("/api/v1/clients/stats").body()
        }
    }
}
