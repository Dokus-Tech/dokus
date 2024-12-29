package ai.thepredict.documents.api

import ai.thepredict.common.withUserIdGetter
import kotlinx.rpc.krpc.ktor.server.RPCRoute

fun RPCRoute.registerDocumentsRemoteServices() {
    registerService<DocumentsRemoteService> { ctx ->
        DocumentsRemoteServiceImpl(ctx, withUserIdGetter())
    }
}