package ai.thepredict.apispec

import ai.thepredict.domain.model.LoginRequest

interface AuthApi {
    companion object;

    suspend fun login(request: LoginRequest): String // Returns JWT token as string
}