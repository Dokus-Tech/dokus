package ai.thepredict.repository.api

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.contacts.api.ContactsRemoteService
import ai.thepredict.identity.api.IdentityRemoteService
import kotlinx.coroutines.Job
import kotlinx.rpc.withService
import kotlin.coroutines.CoroutineContext

class UnifiedApi private constructor(
    override val coroutineContext: CoroutineContext,
    identityRemoteService: IdentityRemoteService,
    contactsRemoteService: ContactsRemoteService,
) : IdentityRemoteService by identityRemoteService,
    ContactsRemoteService by contactsRemoteService {

    companion object {
        suspend fun create(gateway: ServerEndpoint.Gateway = ServerEndpoint.Gateway()): UnifiedApi {
            val client = createClient(gateway)
            return UnifiedApi(
                Job(),
                client.withService<IdentityRemoteService>(),
                client.withService<ContactsRemoteService>(),
            )
        }

        suspend fun create(
            identity: ServerEndpoint.Identity,
            contacts: ServerEndpoint.Contacts,
        ): UnifiedApi {
            return UnifiedApi(
                Job(),
                createClient(identity).withService<IdentityRemoteService>(),
                createClient(contacts).withService<ContactsRemoteService>(),
            )
        }
    }
}