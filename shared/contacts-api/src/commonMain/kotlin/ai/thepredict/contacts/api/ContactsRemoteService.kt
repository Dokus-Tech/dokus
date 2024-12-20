package ai.thepredict.contacts.api

import ai.thepredict.domain.Contact
import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc

@Rpc
interface ContactsRemoteService : RemoteService {
    suspend fun myContacts(): Flow<Contact>
}