package ai.thepredict.domain.model.old

import ai.thepredict.domain.model.JwtTokenDataSchema
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class AuthCredentials(
    val userId: String,
    val jwtToken: String = ""
) {
    companion object {
        val empty = AuthCredentials("", "")

        fun from(jwt: JwtTokenDataSchema, rawToken: String): AuthCredentials {
            return AuthCredentials(userId = jwt.id, jwtToken = rawToken)
        }
    }
}

val AuthCredentials.isValid: Boolean get() = userId.isNotEmpty() && jwtToken.isNotEmpty()

@OptIn(ExperimentalUuidApi::class)
val AuthCredentials.userUUID: Uuid get() = Uuid.parse(userId)