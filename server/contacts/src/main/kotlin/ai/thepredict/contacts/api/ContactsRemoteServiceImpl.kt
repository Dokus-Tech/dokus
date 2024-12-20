package ai.thepredict.contacts.api

import ai.thepredict.domain.Contact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.coroutines.CoroutineContext

class ContactsRemoteServiceImpl(
    override val coroutineContext: CoroutineContext,
) : ContactsRemoteService {
    override suspend fun myContacts(): Flow<Contact> {
        return flowOf(Contact(name = "Artem"))
    }
}