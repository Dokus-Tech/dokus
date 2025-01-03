package ai.thepredict.identity.api

import ai.thepredict.data.AuthCredentials
import ai.thepredict.data.NewUser
import ai.thepredict.data.NewWorkspace
import ai.thepredict.data.User
import ai.thepredict.data.Workspace
import ai.thepredict.domain.api.OperationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc

@Rpc
interface IdentityRemoteService : RemoteService {

    suspend fun authenticate(email: String, password: String): User

    suspend fun createUser(newUser: NewUser): User

    suspend fun myWorkspaces(authCredentials: AuthCredentials): Flow<Workspace>

    suspend fun createWorkspace(
        authCredentials: AuthCredentials,
        workspace: NewWorkspace,
    ): Workspace

    suspend fun deleteWorkspace(
        authCredentials: AuthCredentials,
        organisationId: Workspace.Id,
    ): OperationResult
}