package ai.thepredict.gateway.api

import ai.thepredict.configuration.ServerEndpoints
import ai.thepredict.contacts.api.ContactsServerApi
import ai.thepredict.domain.Contact
import ai.thepredict.gateway.client.createClient
import ai.thepredict.shared.api.ContactsApi
import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.withService
import kotlin.coroutines.CoroutineContext

class ContactsApiImpl(
    override val coroutineContext: CoroutineContext,
) : ContactsApi {

    private suspend fun service() = createClient(ServerEndpoints.Contacts).withService<ContactsServerApi>()

    override suspend fun my(): Flow<Contact> {
        return service().myContacts()
    }
}