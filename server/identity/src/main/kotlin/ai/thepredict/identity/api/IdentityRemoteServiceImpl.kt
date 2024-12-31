package ai.thepredict.identity.api

import ai.thepredict.common.UserIdGetter
import ai.thepredict.database.Database
import ai.thepredict.database.tables.UserEntity
import ai.thepredict.database.tables.findByEmail
import ai.thepredict.database.tables.getById
import ai.thepredict.domain.NewUser
import ai.thepredict.domain.User
import ai.thepredict.domain.Workspace
import ai.thepredict.domain.api.OperationResult
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.identity.mappers.asUserApi
import ai.thepredict.identity.mappers.asWorkspaceApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.Throws

class IdentityRemoteServiceImpl(
    override val coroutineContext: CoroutineContext,
    private val userIdGetter: UserIdGetter,
) : IdentityRemoteService {

    @Throws(PredictException.UserAlreadyExists::class)
    override suspend fun createUser(newUser: NewUser): User {
        val existingUser = UserEntity.findByEmail(newUser.email)
        if (existingUser != null) throw PredictException.UserAlreadyExists

        val user = Database.transaction {
            UserEntity.new {
                name = newUser.name
                email = newUser.email
                passwordHash = newUser.password
            }
        }
        return user.asUserApi
    }

    override suspend fun myWorkspaces(): Flow<Workspace> {
        val workspaces = UserEntity.getById(userIdGetter.get())?.workspaces
        return (workspaces?.asFlow() ?: emptyFlow()).map { it.asWorkspaceApi }
    }

    override suspend fun createWorkspace(workspace: Workspace): OperationResult {
        return OperationResult.OperationNotAvailable
    }

    override suspend fun deleteWorkspace(organisationId: Workspace.Id): OperationResult {
        return OperationResult.OperationNotAvailable
    }
}