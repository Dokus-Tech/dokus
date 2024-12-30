package ai.thepredict.identity.api

import ai.thepredict.domain.NewUser
import ai.thepredict.domain.User
import ai.thepredict.domain.Workspace
import ai.thepredict.domain.api.OperationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Rpc
interface IdentityRemoteService : RemoteService {

    suspend fun createUser(newUser: NewUser): Flow<User>

    suspend fun myWorkspaces(): Flow<Workspace>

    suspend fun createWorkspace(workspace: Workspace): OperationResult

    suspend fun deleteWorkspace(organisationId: Workspace.Id): OperationResult
}