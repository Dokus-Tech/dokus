package ai.thepredict.domain

import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class AuthCredentials(
    val userId: String,
    val passwordHash: String,
) {
    companion object {
        val empty = AuthCredentials("", "")
    }
}

val AuthCredentials.isValid: Boolean get() = userId.isNotEmpty() && passwordHash.isNotEmpty()

@OptIn(ExperimentalUuidApi::class)
val AuthCredentials.userUUID: Uuid get() = Uuid.parse(userId)