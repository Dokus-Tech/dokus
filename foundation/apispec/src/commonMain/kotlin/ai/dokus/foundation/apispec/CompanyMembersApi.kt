package ai.dokus.foundation.apispec

import ai.dokus.foundation.domain.model.User
import ai.dokus.foundation.domain.model.Role
import kotlin.Result

interface CompanyMembersApi {
    companion object {}

    // Return Result to handle exceptions properly
    suspend fun listCompanyMembers(
        companyId: String,
        offset: Int = 0,
        limit: Int = 10
    ): Result<List<User>>

    suspend fun checkCompanyMember(companyId: String, userId: String): Result<Boolean>
    suspend fun updateUserCompanyRole(companyId: String, userId: String, role: Role): Result<User>
}
