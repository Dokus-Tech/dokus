package ai.thepredict.identity.api

import ai.thepredict.domain.Workspace
import ai.thepredict.domain.api.OperationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc

@Rpc
interface IdentityRemoteService : RemoteService {
    suspend fun allWorkspaces(): Flow<Workspace>

    suspend fun createWorkspace(workspace: Workspace): OperationResult

    suspend fun deleteWorkspace(organisationId: Workspace.Id): OperationResult
}