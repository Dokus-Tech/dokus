package ai.thepredict.repository.api

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.domain.Workspace
import ai.thepredict.domain.api.OperationResult
import ai.thepredict.identity.api.IdentityRemoteService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.coroutines.CoroutineContext

interface IdentityApi {
    suspend fun allWorkspaces(): Flow<Workspace>

    suspend fun createWorkspace(workspace: Workspace): OperationResult

    suspend fun deleteWorkspace(organisationId: Workspace.Id): OperationResult
}

class IdentityApiImpl(
    override val coroutineContext: CoroutineContext,
    private val endpoint: ServerEndpoint,
) : IdentityApi,
    ServiceProvider<IdentityRemoteService> by ServiceProvider.create(coroutineContext, endpoint) {

    override suspend fun allWorkspaces(): Flow<Workspace> {
        return withService<IdentityRemoteService, Flow<Workspace>>(onException = emptyFlow()) {
            return@withService allWorkspaces()
        }
    }

    override suspend fun createWorkspace(workspace: Workspace): OperationResult {
        return withServiceOrFailure<IdentityRemoteService> {
            return@withServiceOrFailure createWorkspace(workspace)
        }
    }

    override suspend fun deleteWorkspace(organisationId: Workspace.Id): OperationResult {
        return withServiceOrFailure<IdentityRemoteService> {
            return@withServiceOrFailure deleteWorkspace(organisationId)
        }
    }
}