package ai.thepredict.apispec

import ai.thepredict.domain.model.Company
import ai.thepredict.domain.model.CreateCompanyRequest
import ai.thepredict.domain.model.UpdateCompanyRequest

interface CompanyApi {
    companion object {}

    suspend fun getCompanies(): List<Company>
    suspend fun createCompany(request: CreateCompanyRequest): Company

    suspend fun getCompany(companyId: String): Company
    suspend fun updateCompany(companyId: String, request: UpdateCompanyRequest): Company
    suspend fun deleteCompany(companyId: String)
    suspend fun checkCompanyExists(companyId: String): Boolean
}