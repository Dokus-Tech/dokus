package ai.thepredict.repository

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.repository.api.UnifiedApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.singleton

val repositoryDiModule by DI.Module("repository") {
    GlobalScope.launch { // TODO: Get rid of global scope
//        val api = UnifiedApi.create(ServerEndpoint.Gateway())
//        singleton { api }
    }
}