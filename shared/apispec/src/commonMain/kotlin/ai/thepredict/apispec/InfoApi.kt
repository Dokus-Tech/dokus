package ai.thepredict.apispec

import ai.thepredict.domain.model.InfoSchema

interface InfoApi {
    suspend fun getApiInfo(): InfoSchema
}