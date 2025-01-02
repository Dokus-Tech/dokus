package ai.thepredict.repository

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.repository.api.ContactsApi
import ai.thepredict.repository.api.DocumentsApi
import ai.thepredict.repository.api.IdentityApi
import ai.thepredict.repository.api.PredictionApi
import ai.thepredict.repository.api.SimulationApi
import ai.thepredict.repository.api.UnifiedApi
import kotlinx.coroutines.CoroutineScope
import org.kodein.di.DI
import org.kodein.di.bindFactory
import org.kodein.di.instance

val repositoryDiModule by DI.Module("repository") {
    bindFactory<CoroutineScope, UnifiedApi> { scope ->
        UnifiedApi.create(
            scope.coroutineContext,
            ServerEndpoint.Identity(),
            ServerEndpoint.Contacts(),
            ServerEndpoint.Documents(),
            ServerEndpoint.Prediction(),
            ServerEndpoint.Simulation()
        )
    }

    bindFactory<CoroutineScope, IdentityApi> { scope ->
        instance<UnifiedApi> { scope }
    }

    bindFactory<CoroutineScope, ContactsApi> { scope ->
        instance<UnifiedApi> { scope }
    }

    bindFactory<CoroutineScope, DocumentsApi> { scope ->
        instance<UnifiedApi> { scope }
    }

    bindFactory<CoroutineScope, PredictionApi> { scope ->
        instance<UnifiedApi> { scope }
    }

    bindFactory<CoroutineScope, SimulationApi> { scope ->
        instance<UnifiedApi> { scope }
    }
}