package ai.thepredict.identity.api

import ai.thepredict.apispec.IdentityRemoteService
import ai.thepredict.common.withUserIdGetter
import kotlinx.rpc.krpc.ktor.server.KrpcRoute

fun KrpcRoute.registerIdentityRemoteServices() {
    registerService<IdentityRemoteService> { ctx ->
        IdentityRemoteServiceImpl(ctx, withUserIdGetter())
    }
}