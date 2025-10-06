package ai.dokus.foundation.apispec

import ai.dokus.foundation.domain.model.Company
import ai.dokus.foundation.domain.model.CreateCompanyRequest
import ai.dokus.foundation.domain.model.UpdateCompanyRequest
import kotlin.Result

interface CompanyApi {
    companion object;

    suspend fun getCompanies(): Result<List<Company>>
    suspend fun createCompany(request: CreateCompanyRequest): Result<Company>
    suspend fun getCompany(companyId: String): Result<Company>
    suspend fun updateCompany(companyId: String, request: UpdateCompanyRequest): Result<Company>
    suspend fun deleteCompany(companyId: String): Result<Unit>
    suspend fun checkCompanyExists(companyId: String): Result<Boolean>
}
