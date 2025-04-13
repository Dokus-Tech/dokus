package ai.thepredict.documents.api

import ai.thepredict.apispec.DocumentsRemoteService
import ai.thepredict.common.withUserIdGetter
import kotlinx.rpc.krpc.ktor.server.KrpcRoute

fun KrpcRoute.registerDocumentsRemoteServices() {
    registerService<DocumentsRemoteService> { ctx ->
        DocumentsRemoteServiceImpl(ctx, withUserIdGetter())
    }
}