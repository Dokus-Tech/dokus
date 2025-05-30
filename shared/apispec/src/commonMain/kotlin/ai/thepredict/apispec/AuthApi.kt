package ai.thepredict.apispec

import ai.thepredict.domain.model.LoginRequest

interface AuthApi {
    suspend fun login(request: LoginRequest): String // Returns JWT token as string
}