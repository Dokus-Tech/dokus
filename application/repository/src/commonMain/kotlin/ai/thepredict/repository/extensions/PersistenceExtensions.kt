package ai.thepredict.repository.extensions

import ai.thepredict.app.platform.Persistence
import ai.thepredict.data.AuthCredentials
import ai.thepredict.data.User
import kotlin.uuid.ExperimentalUuidApi

val Persistence.authCredentials: AuthCredentials?
    get() {
        val email = email.takeIf { it?.isNotEmpty() == true } ?: return null
        val password = password.takeIf { it?.isNotEmpty() == true } ?: return null
        return AuthCredentials(email, password)
    }

@OptIn(ExperimentalUuidApi::class)
var Persistence.user: User?
    get() {
        val userId = userId.takeIf { it?.isNotEmpty() == true } ?: return null
        val name = name.takeIf { it?.isNotEmpty() == true } ?: return null
        val email = email.takeIf { it?.isNotEmpty() == true } ?: return null
        val password = password.takeIf { it?.isNotEmpty() == true } ?: return null
        return User(userId, email, name, password)
    }
    set(value) {
        userId = value?.id?.value.toString()
        name = value?.name
        email = value?.email
        password = value?.password
    }