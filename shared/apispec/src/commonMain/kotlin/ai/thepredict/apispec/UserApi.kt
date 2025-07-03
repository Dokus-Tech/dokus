package ai.thepredict.apispec

import ai.thepredict.domain.model.CreateUserRequest
import ai.thepredict.domain.model.UpdateUserRequest
import ai.thepredict.domain.model.User

interface UserApi {
    companion object;

    // Return Result to handle exceptions properly
    suspend fun getUser(userId: String): Result<User>
    suspend fun updateUser(userId: String, request: UpdateUserRequest): Result<User>
    suspend fun deleteUser(userId: String): Result<Unit>
    suspend fun createUser(request: CreateUserRequest): Result<User>
    suspend fun checkUserExistsByEmail(email: String): Result<Boolean>
}
