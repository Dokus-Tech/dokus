package ai.thepredict.repository.api

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.Workspace
import ai.thepredict.domain.api.OperationResult
import ai.thepredict.identity.api.IdentityRemoteService
import ai.thepredict.repository.helpers.ServiceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.rpc.withService
import kotlin.coroutines.CoroutineContext

interface IdentityApi {
    suspend fun myWorkspaces(): Flow<Workspace>

    suspend fun createWorkspace(workspace: Workspace): OperationResult

    suspend fun deleteWorkspace(organisationId: Workspace.Id): OperationResult

    companion object : ApiCompanion<IdentityApi, ServerEndpoint.Identity> {
        override fun create(
            coroutineContext: CoroutineContext,
            endpoint: ServerEndpoint.Identity,
        ): IdentityApi {
            return IdentityApiImpl(coroutineContext, endpoint)
        }

        override fun create(
            coroutineContext: CoroutineContext,
            endpoint: ServerEndpoint.Gateway,
        ): IdentityApi {
            return IdentityApiImpl(coroutineContext, endpoint)
        }
    }
}

private class IdentityApiImpl(
    coroutineContext: CoroutineContext,
    endpoint: ServerEndpoint,
) : IdentityApi {

    private val serviceProvider = ServiceProvider<IdentityRemoteService>(
        coroutineContext = coroutineContext,
        endpoint = endpoint,
        createService = { withService() }
    )

    override suspend fun myWorkspaces(): Flow<Workspace> {
        return serviceProvider.withService(onException = emptyFlow()) {
            myWorkspaces()
        }
    }

    override suspend fun createWorkspace(workspace: Workspace): OperationResult {
        return serviceProvider.withServiceOrFailure {
            createWorkspace(workspace)
        }
    }

    override suspend fun deleteWorkspace(organisationId: Workspace.Id): OperationResult {
        return serviceProvider.withServiceOrFailure {
            deleteWorkspace(organisationId)
        }
    }
}