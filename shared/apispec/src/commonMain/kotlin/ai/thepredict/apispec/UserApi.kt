package ai.thepredict.apispec

import ai.thepredict.domain.model.CreateUserRequest
import ai.thepredict.domain.model.UpdateUserRequest
import ai.thepredict.domain.model.User

interface UserApi {
    companion object {}

    suspend fun getUser(userId: String): User
    suspend fun updateUser(userId: String, request: UpdateUserRequest): User
    suspend fun deleteUser(userId: String)
    suspend fun createUser(request: CreateUserRequest): User
    suspend fun checkUserExistsByEmail(email: String): Boolean
}