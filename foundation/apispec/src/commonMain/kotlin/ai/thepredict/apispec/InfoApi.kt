package ai.thepredict.apispec

import ai.thepredict.domain.model.InfoSchema

interface InfoApi {
    companion object;

    // Return Result to handle exceptions properly
    suspend fun getApiInfo(): Result<InfoSchema>
}
