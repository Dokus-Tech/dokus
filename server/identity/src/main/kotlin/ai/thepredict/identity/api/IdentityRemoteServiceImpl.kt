package ai.thepredict.identity.api

import ai.thepredict.database.tables.WorkspaceEntity
import ai.thepredict.database.tables.getAll
import ai.thepredict.domain.Workspace
import ai.thepredict.domain.api.OperationResult
import ai.thepredict.identity.mappers.asWorkspaceApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext

class IdentityRemoteServiceImpl(
    override val coroutineContext: CoroutineContext,
) : IdentityRemoteService {

    override suspend fun allWorkspaces(): Flow<Workspace> {
        return WorkspaceEntity.getAll().asFlow().map { it.asWorkspaceApi }
    }

    override suspend fun createWorkspace(workspace: Workspace): OperationResult {
        return OperationResult.OperationNotAvailable
    }

    override suspend fun deleteWorkspace(organisationId: Workspace.Id): OperationResult {
        return OperationResult.OperationNotAvailable
    }

}