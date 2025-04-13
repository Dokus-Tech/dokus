package ai.thepredict.repository.extensions

import ai.thepredict.domain.model.AuthCredentials
import ai.thepredict.domain.model.User
import ai.thepredict.domain.model.Workspace
import ai.thepredict.app.platform.Persistence

val Persistence.authCredentials: AuthCredentials?
    get() {
        val userId = userId.takeIf { it?.isNotEmpty() == true } ?: return null
        val password = password.takeIf { it?.isNotEmpty() == true } ?: return null
        return AuthCredentials(userId, password)
    }

var Persistence.user: User?
    get() {
        val userId = userId.takeIf { it?.isNotEmpty() == true } ?: return null
        val name = name.takeIf { it?.isNotEmpty() == true } ?: return null
        val email = email.takeIf { it?.isNotEmpty() == true } ?: return null
        val password = password.takeIf { it?.isNotEmpty() == true } ?: return null
        return User(userId, email, name, password)
    }
    set(value) {
        userId = value?.id
        name = value?.name
        email = value?.email
        password = value?.password
    }

var Persistence.selectedWorkspaceId: Workspace.Id?
    get() {
        val id = selectedWorkspace.takeIf { it != null } ?: return null
        return Workspace.Id(id)
    }
    set(value) {
        selectedWorkspace = value?.value
    }