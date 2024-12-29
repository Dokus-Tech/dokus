package ai.thepredict.identity.api

import ai.thepredict.common.withUserIdGetter
import kotlinx.rpc.krpc.ktor.server.RPCRoute

fun RPCRoute.registerIdentityRemoteServices() {
    registerService<IdentityRemoteService> { ctx ->
        IdentityRemoteServiceImpl(ctx, withUserIdGetter())
    }
}