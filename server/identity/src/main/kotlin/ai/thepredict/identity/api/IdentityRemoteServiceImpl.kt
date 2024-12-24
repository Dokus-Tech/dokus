package ai.thepredict.identity.api

import ai.thepredict.domain.Workspace
import ai.thepredict.domain.api.OperationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.coroutines.CoroutineContext

class IdentityRemoteServiceImpl(
    override val coroutineContext: CoroutineContext,
) : IdentityRemoteService {
    override suspend fun allWorkspaces(): Flow<Workspace> {
        return emptyFlow()
    }

    override suspend fun createWorkspace(workspace: Workspace): OperationResult {
        return OperationResult.OperationNotAvailable
    }

    override suspend fun deleteWorkspace(organisationId: Workspace.Id): OperationResult {
        return OperationResult.OperationNotAvailable
    }

}