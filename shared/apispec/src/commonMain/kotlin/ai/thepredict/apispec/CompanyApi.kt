package ai.thepredict.apispec

import ai.thepredict.domain.model.Company
import ai.thepredict.domain.model.CreateCompanyRequest
import ai.thepredict.domain.model.UpdateCompanyRequest
import kotlin.Result

interface CompanyApi {
    companion object;

    // Return Result to handle exceptions properly
    suspend fun getCompanies(): Result<List<Company>>
    suspend fun createCompany(request: CreateCompanyRequest): Result<Company>

    suspend fun getCompany(companyId: String): Result<Company>
    suspend fun updateCompany(companyId: String, request: UpdateCompanyRequest): Result<Company>
    suspend fun deleteCompany(companyId: String): Result<Unit>
    suspend fun checkCompanyExists(companyId: String): Result<Boolean>
}
