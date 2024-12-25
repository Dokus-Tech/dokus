package ai.thepredict.repository

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.repository.api.UnifiedApi
import org.kodein.di.DI
import org.kodein.di.singleton

val repositoryDiModule by DI.Module("repository") {
    val api = UnifiedApi.create(ServerEndpoint.Gateway())
    singleton { api }
}