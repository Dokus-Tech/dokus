package ai.thepredict.repository

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.repository.api.ContactsApi
import ai.thepredict.repository.api.IdentityApi
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
        )
    }

    bindFactory<CoroutineScope, ContactsApi> { scope ->
        ContactsApi.create(
            scope.coroutineContext,
            ServerEndpoint.Contacts()
        )
    }

    bindFactory<CoroutineScope, IdentityApi> { scope ->
        IdentityApi.create(
            scope.coroutineContext,
            ServerEndpoint.Identity()
        )
    }
}