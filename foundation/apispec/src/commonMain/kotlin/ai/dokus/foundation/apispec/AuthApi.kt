package ai.dokus.foundation.apispec

import ai.dokus.foundation.domain.model.LoginRequest

interface AuthApi {
    companion object;

    // Return Result to handle exceptions properly
    suspend fun login(request: LoginRequest): Result<String> // Returns JWT token as string
}
