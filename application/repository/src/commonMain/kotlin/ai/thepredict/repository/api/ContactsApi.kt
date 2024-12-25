package ai.thepredict.repository.api

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.contacts.api.ContactsRemoteService
import ai.thepredict.domain.Contact
import ai.thepredict.domain.api.OperationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.rpc.krpc.streamScoped
import kotlin.coroutines.CoroutineContext

interface ContactsApi {
    suspend fun getAll(): Flow<Contact>

    suspend fun get(id: Contact.Id): Contact?

    suspend fun find(query: String): Flow<Contact>

    suspend fun create(create: Contact): OperationResult

    suspend fun update(contact: Contact): OperationResult

    suspend fun delete(id: Contact.Id): OperationResult
}

internal class ContactsApiImpl(
    override val coroutineContext: CoroutineContext,
    private val endpoint: ServerEndpoint,
) : ServiceProviderImpl<ContactsRemoteService>(coroutineContext, endpoint),
    ContactsApi {

    override suspend fun getAll(): Flow<Contact> {
        val service = getService<ContactsRemoteService>(endpoint).getOrThrow()
        return service.getAll()
//        return withService(onException = emptyFlow()) {
//            streamScoped { getAll() }
//        }
    }

    override suspend fun get(id: Contact.Id): Contact? {
        return withService(onException = null) {
            get(id)
        }
    }

    override suspend fun find(query: String): Flow<Contact> {
        return withService(onException = emptyFlow()) {
            find(query)
        }
    }

    override suspend fun create(create: Contact): OperationResult {
        return withServiceOrFailure {
            create(create)
        }
    }

    override suspend fun update(contact: Contact): OperationResult {
        return withServiceOrFailure {
            update(contact)
        }
    }

    override suspend fun delete(id: Contact.Id): OperationResult {
        return withServiceOrFailure {
            delete(id)
        }
    }
}