package ai.thepredict.identity.api

import ai.thepredict.domain.AuthCredentials
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

    suspend fun authenticate(email: String, password: String): User

    suspend fun createUser(newUser: NewUser): User

    suspend fun myWorkspaces(authCredentials: AuthCredentials): Flow<Workspace>

    suspend fun createWorkspace(
        authCredentials: AuthCredentials,
        workspace: Workspace,
    ): OperationResult

    suspend fun deleteWorkspace(
        authCredentials: AuthCredentials,
        organisationId: Workspace.Id,
    ): OperationResult
}