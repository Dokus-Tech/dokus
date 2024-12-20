package ai.thepredict.contacts.api

import ai.thepredict.domain.Contact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.coroutines.CoroutineContext

class ContactsServerApiImpl(override val coroutineContext: CoroutineContext) : ContactsServerApi {
    override suspend fun myContacts(): Flow<Contact> {
        return flowOf(Contact(name = "Artem"))
    }
}