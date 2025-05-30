package ai.thepredict.apispec.service

import ai.thepredict.domain.model.AuthCredentials
import ai.thepredict.domain.model.NewUser
import ai.thepredict.domain.model.NewWorkspace
import ai.thepredict.domain.model.User
import ai.thepredict.domain.model.Workspace
import ai.thepredict.domain.api.OperationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc

@Rpc
interface IdentityRemoteService : RemoteService {

    suspend fun authenticate(email: String, password: String): User

    suspend fun createUser(newUser: NewUser): User

    fun myWorkspaces(authCredentials: AuthCredentials): Flow<Workspace>

    suspend fun createWorkspace(
        authCredentials: AuthCredentials,
        newWorkspace: NewWorkspace,
    ): Workspace

    suspend fun deleteWorkspace(
        authCredentials: AuthCredentials,
        organisationId: Workspace.Id,
    ): OperationResult
}