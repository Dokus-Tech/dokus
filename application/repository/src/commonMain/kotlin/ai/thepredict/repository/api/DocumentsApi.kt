package ai.thepredict.repository.api

import ai.thepredict.apispec.DocumentsRemoteService
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.repository.helpers.ServiceProvider
import kotlinx.rpc.withService
import kotlin.coroutines.CoroutineContext

interface DocumentsApi {

    companion object : ApiCompanion<DocumentsApi, ServerEndpoint.Documents> {
        override fun create(
            coroutineContext: CoroutineContext,
            endpoint: ServerEndpoint.Documents,
        ): DocumentsApi {
            return DocumentsApiImpl(coroutineContext, endpoint)
        }

        override fun create(
            coroutineContext: CoroutineContext,
            endpoint: ServerEndpoint.Gateway,
        ): DocumentsApi {
            return DocumentsApiImpl(coroutineContext, endpoint)
        }
    }
}

private class DocumentsApiImpl(
    coroutineContext: CoroutineContext,
    endpoint: ServerEndpoint,
) : DocumentsApi {

    private val serviceProvider = ServiceProvider<DocumentsRemoteService>(
        coroutineContext = coroutineContext,
        endpoint = endpoint,
        createService = { withService() }
    )
}