package ai.thepredict.repository.api

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.contacts.api.ContactsRemoteService
import ai.thepredict.identity.api.IdentityRemoteService
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

class UnifiedApi private constructor(
    override val coroutineContext: CoroutineContext,
    identityRemoteService: IdentityRemoteService,
    contactsRemoteService: ContactsRemoteService,
) : IdentityRemoteService by identityRemoteService,
    ContactsRemoteService by contactsRemoteService {

    companion object {
        fun create(gateway: ServerEndpoint.Gateway = ServerEndpoint.Gateway()): UnifiedApi {
            return UnifiedApi(
                Job(),
                IdentityApi(Job(), gateway),
                ContactsApi(Job(), gateway),
            )
        }

        fun create(
            identity: ServerEndpoint.Identity,
            contacts: ServerEndpoint.Contacts,
        ): UnifiedApi {
            return UnifiedApi(
                Job(),
                IdentityApi(Job(), identity),
                ContactsApi(Job(), contacts),
            )
        }
    }
}