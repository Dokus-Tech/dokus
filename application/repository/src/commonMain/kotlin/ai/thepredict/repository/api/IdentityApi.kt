package ai.thepredict.repository.api

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.Workspace
import ai.thepredict.domain.api.OperationResult
import ai.thepredict.identity.api.IdentityRemoteService
import ai.thepredict.repository.helpers.ServiceProvider
import ai.thepredict.repository.helpers.withService
import ai.thepredict.repository.helpers.withServiceOrFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

interface IdentityApi {
    suspend fun allWorkspaces(): Flow<Workspace>

    suspend fun createWorkspace(workspace: Workspace): OperationResult

    suspend fun deleteWorkspace(organisationId: Workspace.Id): OperationResult

    companion object {
        fun create(
            coroutineContext: CoroutineContext,
            endpoint: ServerEndpoint.Identity,
        ): IdentityApi {
            return IdentityApiImpl(coroutineContext, endpoint)
        }

        fun create(
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

    private val serviceProvider by lazy {
        ServiceProvider.create<IdentityRemoteService>(
            coroutineContext,
            endpoint
        )
    }

    override suspend fun allWorkspaces(): Flow<Workspace> {
        return serviceProvider.withService<IdentityRemoteService, Flow<Workspace>>(onException = emptyFlow()) {
            return@withService allWorkspaces()
        }
    }

    override suspend fun createWorkspace(workspace: Workspace): OperationResult {
        return serviceProvider.withServiceOrFailure<IdentityRemoteService> {
            return@withServiceOrFailure createWorkspace(workspace)
        }
    }

    override suspend fun deleteWorkspace(organisationId: Workspace.Id): OperationResult {
        return serviceProvider.withServiceOrFailure<IdentityRemoteService> {
            return@withServiceOrFailure deleteWorkspace(organisationId)
        }
    }
}