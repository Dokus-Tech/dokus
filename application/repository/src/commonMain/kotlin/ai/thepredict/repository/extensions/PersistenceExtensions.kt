package ai.thepredict.repository.extensions

import ai.thepredict.app.platform.Persistence
import ai.thepredict.domain.model.AuthCredentials
import ai.thepredict.domain.model.User

var Persistence.authCredentials: AuthCredentials?
    get() {
        val userId = userId.takeIf { it?.isNotEmpty() == true } ?: return null
        val token = jwtToken.takeIf { it?.isNotEmpty() == true } ?: return null
        return AuthCredentials(userId, token)
    }
    set(value) {
        jwtToken = value?.jwtToken
        userId = value?.userId
    }

var Persistence.user: User?
    get() {
        val userId = userId.takeIf { it?.isNotEmpty() == true } ?: return null
        val firstName = firstName.takeIf { it?.isNotEmpty() == true } ?: return null
        val lastName = lastName.takeIf { it?.isNotEmpty() == true } ?: return null
        val email = email.takeIf { it?.isNotEmpty() == true } ?: return null
        return User(
            id = userId,
            firstName = firstName,
            lastName = lastName,
            email = email
        )
    }
    set(value) {
        userId = value?.id
        firstName = value?.firstName
        lastName = value?.lastName
        email = value?.email
    }

//var Persistence.selectedWorkspaceId: Workspace.Id?
//    get() {
//        val id = selectedWorkspace.takeIf { it != null } ?: return null
//        return Workspace.Id(id)
//    }
//    set(value) {
//        selectedWorkspace = value?.value
//    }