package ai.thepredict.repository.api

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.contacts.api.ContactsRemoteService
import ai.thepredict.domain.Contact
import ai.thepredict.domain.api.OperationResult
import ai.thepredict.repository.helpers.ServiceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.withService
import kotlin.coroutines.CoroutineContext

interface ContactsApi {
    suspend fun getAll(): Result<Flow<Contact>>

    suspend fun get(id: Contact.Id): Result<Contact>

    suspend fun find(query: String): Result<Flow<Contact>>

    suspend fun create(create: Contact): OperationResult

    suspend fun update(contact: Contact): OperationResult

    suspend fun delete(id: Contact.Id): OperationResult

    companion object : ApiCompanion<ContactsApi, ServerEndpoint.Contacts> {
        override fun create(
            coroutineContext: CoroutineContext,
            endpoint: ServerEndpoint.Contacts,
        ): ContactsApi {
            return ContactsApiImpl(coroutineContext, endpoint)
        }

        override fun create(
            coroutineContext: CoroutineContext,
            endpoint: ServerEndpoint.Gateway,
        ): ContactsApi {
            return ContactsApiImpl(coroutineContext, endpoint)
        }
    }
}

private class ContactsApiImpl(
    coroutineContext: CoroutineContext,
    endpoint: ServerEndpoint,
) : ContactsApi {

    private val serviceProvider = ServiceProvider<ContactsRemoteService>(
        coroutineContext = coroutineContext,
        endpoint = endpoint,
        createService = { withService() }
    )

    override suspend fun getAll(): Result<Flow<Contact>> {
        return serviceProvider.withService {
            getAll()
        }
    }

    override suspend fun get(id: Contact.Id): Result<Contact> {
        return serviceProvider.withService {
            get(id)
        }
    }

    override suspend fun find(query: String): Result<Flow<Contact>> {
        return serviceProvider.withService {
            find(query)
        }
    }

    override suspend fun create(create: Contact): OperationResult {
        return serviceProvider.withServiceOrFailure {
            create(create)
        }
    }

    override suspend fun update(contact: Contact): OperationResult {
        return serviceProvider.withServiceOrFailure {
            update(contact)
        }
    }

    override suspend fun delete(id: Contact.Id): OperationResult {
        return serviceProvider.withServiceOrFailure {
            delete(id)
        }
    }
}