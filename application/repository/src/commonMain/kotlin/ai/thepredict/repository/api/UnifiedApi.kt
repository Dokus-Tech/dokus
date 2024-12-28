package ai.thepredict.repository.api

import ai.thepredict.configuration.ServerEndpoint
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class UnifiedApi private constructor(
    identityApi: IdentityApi,
    contactsApi: ContactsApi,
) : IdentityApi by identityApi,
    ContactsApi by contactsApi {

    companion object {
        fun create(
            coroutineContext: CoroutineContext,
            gateway: ServerEndpoint.Gateway,
        ): UnifiedApi {
            return UnifiedApi(
                IdentityApi.create(coroutineContext, gateway),
                ContactsApi.create(coroutineContext, gateway),
            )
        }

        fun create(
            coroutineContext: CoroutineContext,
            identity: ServerEndpoint.Identity,
            contacts: ServerEndpoint.Contacts,
        ): UnifiedApi {
            return UnifiedApi(
                IdentityApi.create(coroutineContext, identity),
                ContactsApi.create(coroutineContext, contacts),
            )
        }
    }
}

suspend fun UnifiedApi.Companion.create(
    gateway: ServerEndpoint.Gateway,
): UnifiedApi {
    return create(coroutineContext, gateway)
}

suspend fun UnifiedApi.Companion.create(
    identity: ServerEndpoint.Identity,
    contacts: ServerEndpoint.Contacts,
): UnifiedApi {
    return create(coroutineContext, identity, contacts)
}