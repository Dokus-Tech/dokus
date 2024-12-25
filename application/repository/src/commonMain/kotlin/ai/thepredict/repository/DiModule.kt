package ai.thepredict.repository

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.repository.api.UnifiedApi
import org.kodein.di.DI
import org.kodein.di.bindSingleton

val repositoryDiModule by DI.Module("repository") {
    val api = UnifiedApi.create(ServerEndpoint.Identity(), ServerEndpoint.Contacts())
//    val api = UnifiedApi.create(ServerEndpoint.Gateway())
    bindSingleton<UnifiedApi> { api }
}