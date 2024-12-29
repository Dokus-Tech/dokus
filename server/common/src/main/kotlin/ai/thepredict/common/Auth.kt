package ai.thepredict.common

import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.principal
import kotlinx.rpc.krpc.ktor.server.RPCRoute
import java.util.UUID

fun interface UserIdGetter {
    fun get(): UUID
}

fun RPCRoute.withUserIdGetter(): UserIdGetter {
    return UserIdGetter {
        call.principal<UserIdPrincipal>()?.name?.let(UUID::fromString) ?: UUID.randomUUID()
    }
}