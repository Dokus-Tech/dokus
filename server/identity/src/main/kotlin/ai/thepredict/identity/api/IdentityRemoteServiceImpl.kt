package ai.thepredict.identity.api

import ai.thepredict.database.tables.UserEntity
import ai.thepredict.database.tables.getById
import ai.thepredict.domain.Workspace
import ai.thepredict.domain.api.OperationResult
import ai.thepredict.identity.mappers.asWorkspaceApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import java.util.UUID
import kotlin.coroutines.CoroutineContext

class IdentityRemoteServiceImpl(
    override val coroutineContext: CoroutineContext,
) : IdentityRemoteService {

    override suspend fun myWorkspaces(): Flow<Workspace> {
        val workspaces = UserEntity.getById(UUID.randomUUID())?.workspaces
        return (workspaces?.asFlow() ?: emptyFlow()).map { it.asWorkspaceApi }
    }

    override suspend fun createWorkspace(workspace: Workspace): OperationResult {
        return OperationResult.OperationNotAvailable
    }

    override suspend fun deleteWorkspace(organisationId: Workspace.Id): OperationResult {
        return OperationResult.OperationNotAvailable
    }
}