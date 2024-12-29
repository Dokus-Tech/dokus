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
        IdentityApi.create(
            scope.coroutineContext,
            ServerEndpoint.Identity()
        )
    }

    bindFactory<CoroutineScope, ContactsApi> { scope ->
        ContactsApi.create(
            scope.coroutineContext,
            ServerEndpoint.Contacts()
        )
    }

    bindFactory<CoroutineScope, DocumentsApi> { scope ->
        DocumentsApi.create(
            scope.coroutineContext,
            ServerEndpoint.Documents()
        )
    }

    bindFactory<CoroutineScope, PredictionApi> { scope ->
        PredictionApi.create(
            scope.coroutineContext,
            ServerEndpoint.Prediction()
        )
    }

    bindFactory<CoroutineScope, SimulationApi> { scope ->
        SimulationApi.create(
            scope.coroutineContext,
            ServerEndpoint.Simulation()
        )
    }
}