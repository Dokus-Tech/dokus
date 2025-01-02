package ai.thepredict.contacts.api

import ai.thepredict.common.UserIdGetter
import ai.thepredict.data.Contact
import ai.thepredict.domain.api.OperationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.coroutines.CoroutineContext

class ContactsRemoteServiceImpl(
    override val coroutineContext: CoroutineContext,
    private val userIdGetter: UserIdGetter,
) : ContactsRemoteService {

    override suspend fun getAll(): Flow<Contact> {
        return flowOf(Contact(id = Contact.Id.random, name = "Artem"))
    }

    override suspend fun get(id: Contact.Id): Contact {
        throw NotImplementedError()
    }

    override suspend fun find(query: String): Flow<Contact> {
        throw NotImplementedError()
    }

    override suspend fun create(create: Contact): OperationResult {
        return OperationResult.OperationNotAvailable
    }

    override suspend fun update(contact: Contact): OperationResult {
        return OperationResult.OperationNotAvailable
    }

    override suspend fun delete(id: Contact.Id): OperationResult {
        return OperationResult.OperationNotAvailable
    }
}