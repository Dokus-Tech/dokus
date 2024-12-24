package ai.thepredict.identity.api

import kotlinx.rpc.krpc.ktor.server.RPCRoute

fun RPCRoute.registerIdentityRemoteServices() {
    registerService<IdentityRemoteService> { ctx ->
        IdentityRemoteServiceImpl(ctx)
    }
}