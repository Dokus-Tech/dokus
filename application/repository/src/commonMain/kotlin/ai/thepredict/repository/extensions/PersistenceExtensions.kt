package ai.thepredict.repository.extensions

import ai.thepredict.app.platform.Persistence
import ai.thepredict.data.AuthCredentials

val Persistence.authCredentials: AuthCredentials?
    get() {
        val email = email.takeIf { it?.isNotEmpty() == true } ?: return null
        val password = password.takeIf { it?.isNotEmpty() == true } ?: return null
        return AuthCredentials(email, password)
    }