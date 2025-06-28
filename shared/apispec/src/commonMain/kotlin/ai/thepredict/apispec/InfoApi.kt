package ai.thepredict.apispec

import ai.thepredict.domain.model.InfoSchema

interface InfoApi {
    companion object;

    suspend fun getApiInfo(): InfoSchema
}