package ai.thepredict.repository.api

import ai.thepredict.apispec.UserApi
import ai.thepredict.domain.configuration.ServerEndpoint
import ai.thepredict.domain.model.CreateUserRequest
import ai.thepredict.domain.model.UpdateUserRequest
import ai.thepredict.domain.model.User
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

class UserApiImpl(
    private val client: HttpClient,
) : UserApi {
    override suspend fun getUser(userId: String): Result<User> {
        return runCatching {
            client.get("/api/v1/users/$userId").body()
        }
    }

    override suspend fun updateUser(userId: String, request: UpdateUserRequest): Result<User> {
        return runCatching {
            client.put("/api/v1/users/$userId") {
                setBody(request)
            }.body()
        }
    }

    override suspend fun deleteUser(userId: String): Result<Unit> {
        return runCatching {
            client.delete("/api/v1/users/$userId")
        }
    }

    override suspend fun createUser(request: CreateUserRequest): Result<User> {
        return runCatching {
            client.post("/api/v1/users") {
                setBody(request)
            }.body()
        }
    }

    override suspend fun checkUserExistsByEmail(email: String): Result<Boolean> {
        return runCatching {
            client.get("/api/v1/users/exists") {
                parameter("email", email)
            }.body()
        }
    }
}

internal fun UserApi.Companion.create(httpClient: HttpClient, endpoint: ServerEndpoint): UserApi {
    return UserApiImpl(
        client = httpClient,
    )
}