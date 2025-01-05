package ai.thepredict.identity.api

import ai.thepredict.common.UserIdGetter
import ai.thepredict.database.Database
import ai.thepredict.database.tables.UserEntity
import ai.thepredict.database.tables.findByEmail
import ai.thepredict.database.tables.getById
import ai.thepredict.data.AuthCredentials
import ai.thepredict.data.NewUser
import ai.thepredict.data.NewWorkspace
import ai.thepredict.data.User
import ai.thepredict.data.Workspace
import ai.thepredict.domain.api.OperationResult
import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.data.userUUID
import ai.thepredict.database.tables.WorkspaceEntity
import ai.thepredict.database.tables.authenticated
import ai.thepredict.database.tables.getAllForUserId
import ai.thepredict.domain.usecases.validators.ValidateNewUserUseCase
import ai.thepredict.domain.usecases.validators.ValidateNewWorkspaceUseCase
import ai.thepredict.identity.mappers.asUserApi
import ai.thepredict.identity.mappers.asWorkspaceApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.CoroutineContext
import kotlin.uuid.ExperimentalUuidApi

class IdentityRemoteServiceImpl(
    override val coroutineContext: CoroutineContext,
    private val userIdGetter: UserIdGetter,
    private val validateNewUserUseCase: ValidateNewUserUseCase = ValidateNewUserUseCase(),
    private val validateNewWorkspaceUseCase: ValidateNewWorkspaceUseCase = ValidateNewWorkspaceUseCase(),
) : IdentityRemoteService {

    @Throws(PredictException.NotAuthenticated::class)
    override suspend fun authenticate(email: String, password: String): User {
        val existingUser = UserEntity.findByEmail(email)

        if (existingUser == null || existingUser.passwordHash != password) throw PredictException.NotAuthenticated
        return existingUser.asUserApi
    }

    @Throws(PredictException::class)
    override suspend fun createUser(newUser: NewUser): User {
        validateNewUserUseCase(newUser)

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

    @Throws(PredictException.NotAuthenticated::class)
    override suspend fun myWorkspaces(authCredentials: AuthCredentials): Flow<Workspace> {
        val user = UserEntity.authenticated(authCredentials)

        return WorkspaceEntity
            .getAllForUserId(user.userId)
            .map { it.asWorkspaceApi }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun createWorkspace(
        authCredentials: AuthCredentials,
        newWorkspace: NewWorkspace,
    ): Workspace {
        val user = UserEntity.authenticated(authCredentials)
        validateNewWorkspaceUseCase(newWorkspace)

        val workspaceEntity = Database.transaction {
            WorkspaceEntity.new {
                name = newWorkspace.name
                legalName = newWorkspace.legalName
                taxNumber = newWorkspace.taxNumber
                owner = user
            }
        }

        return workspaceEntity.asWorkspaceApi
    }

    override suspend fun deleteWorkspace(
        authCredentials: AuthCredentials,
        organisationId: Workspace.Id,
    ): OperationResult {
        return OperationResult.OperationNotAvailable
    }
}