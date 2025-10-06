package ai.dokus.foundation.apispec

import ai.dokus.foundation.domain.model.InfoSchema

interface InfoApi {
    companion object;

    // Return Result to handle exceptions properly
    suspend fun getApiInfo(): Result<InfoSchema>
}
