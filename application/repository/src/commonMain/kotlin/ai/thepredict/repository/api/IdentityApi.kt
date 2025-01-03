package ai.thepredict.repository.api

import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.data.NewUser
import ai.thepredict.data.NewWorkspace
import ai.thepredict.data.User
import ai.thepredict.data.Workspace
import ai.thepredict.domain.api.OperationResult
import ai.thepredict.identity.api.IdentityRemoteService
import ai.thepredict.repository.helpers.ServiceProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.withService
import kotlin.coroutines.CoroutineContext

interface IdentityApi {
    suspend fun authenticate(email: String, password: String): Result<User>

    suspend fun createUser(newUser: NewUser): Result<User>

    suspend fun myWorkspaces(): Result<Flow<Workspace>>

    suspend fun createWorkspace(workspace: NewWorkspace): Result<Workspace>

    suspend fun deleteWorkspace(organisationId: Workspace.Id): OperationResult

    companion object : ApiCompanion<IdentityApi, ServerEndpoint.Identity> {
        override fun create(
            coroutineContext: CoroutineContext,
            endpoint: ServerEndpoint.Identity,
        ): IdentityApi {
            return IdentityApiImpl(coroutineContext, endpoint)
        }

        override fun create(
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

    private val serviceProvider = ServiceProvider<IdentityRemoteService>(
        coroutineContext = coroutineContext,
        endpoint = endpoint,
        createService = { withService() }
    )

    override suspend fun authenticate(email: String, password: String): Result<User> {
        return serviceProvider.withService {
            authenticate(email, password)
        }
    }

    override suspend fun createUser(newUser: NewUser): Result<User> {
        return serviceProvider.withService {
            createUser(newUser)
        }
    }

    override suspend fun myWorkspaces(): Result<Flow<Workspace>> {
        return serviceProvider.withService { authCredentials ->
            require(authCredentials != null)
            myWorkspaces(authCredentials)
        }
    }

    override suspend fun createWorkspace(workspace: NewWorkspace): Result<Workspace> {
        return serviceProvider.withService { authCredentials ->
            require(authCredentials != null)
            createWorkspace(authCredentials, workspace)
        }
    }

    override suspend fun deleteWorkspace(organisationId: Workspace.Id): OperationResult {
        return serviceProvider.withServiceOrFailure { authCredentials ->
            require(authCredentials != null)
            deleteWorkspace(authCredentials, organisationId)
        }
    }
}