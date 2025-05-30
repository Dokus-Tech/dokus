package ai.thepredict.apispec

import ai.thepredict.domain.model.User
import ai.thepredict.domain.model.Role

interface CompanyMembersApi {
    suspend fun listCompanyMembers(companyId: String, offset: Int = 0, limit: Int = 10): List<User>
    suspend fun checkCompanyMember(companyId: String, userId: String): Boolean
    suspend fun updateUserCompanyRole(companyId: String, userId: String, role: Role): User
}