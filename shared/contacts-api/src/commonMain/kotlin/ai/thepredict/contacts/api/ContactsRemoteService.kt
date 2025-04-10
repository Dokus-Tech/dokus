package ai.thepredict.contacts.api

import ai.thepredict.data.Contact
import ai.thepredict.domain.api.OperationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc

@Rpc
interface ContactsRemoteService : RemoteService {
    fun getAll(): Flow<Contact>

    suspend fun get(id: Contact.Id): Contact

    fun find(query: String): Flow<Contact>

    suspend fun create(create: Contact): OperationResult

    suspend fun update(contact: Contact): OperationResult

    suspend fun delete(id: Contact.Id): OperationResult
}