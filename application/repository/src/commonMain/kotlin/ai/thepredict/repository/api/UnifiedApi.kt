package ai.thepredict.repository.api

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.identity.api.IdentityRemoteService
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

class UnifiedApi private constructor(
    identityApi: IdentityApi,
    contactsApi: ContactsApi,
) : IdentityApi by identityApi,
    ContactsApi by contactsApi {

    companion object {
        fun create(gateway: ServerEndpoint.Gateway = ServerEndpoint.Gateway()): UnifiedApi {
            return UnifiedApi(
                IdentityApiImpl(Job(), gateway),
                ContactsApiImpl(Job(), gateway),
            )
        }

        fun create(
            identity: ServerEndpoint.Identity,
            contacts: ServerEndpoint.Contacts,
        ): UnifiedApi {
            return UnifiedApi(
                IdentityApiImpl(Job(), identity),
                ContactsApiImpl(Job(), contacts),
            )
        }
    }
}