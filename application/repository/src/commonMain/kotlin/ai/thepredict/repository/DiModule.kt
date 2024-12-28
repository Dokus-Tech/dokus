package ai.thepredict.repository

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.repository.api.ContactsApi
import ai.thepredict.repository.api.IdentityApi
import ai.thepredict.repository.api.UnifiedApi
import org.kodein.di.DI
import org.kodein.di.bindFactory
import kotlin.coroutines.CoroutineContext

val repositoryDiModule by DI.Module("repository") {
    bindFactory<CoroutineContext, UnifiedApi> { coroutineContext ->
        UnifiedApi.create(
            coroutineContext,
            ServerEndpoint.Gateway()
        )
    }

    bindFactory<CoroutineContext, ContactsApi> { coroutineContext ->
        ContactsApi.create(
            coroutineContext,
            ServerEndpoint.Contacts()
        )
    }

    bindFactory<CoroutineContext, IdentityApi> { coroutineContext ->
        IdentityApi.create(
            coroutineContext,
            ServerEndpoint.Identity()
        )
    }
}