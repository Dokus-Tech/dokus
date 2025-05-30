package ai.thepredict.contacts.api

import ai.thepredict.apispec.service.ContactsRemoteService
import ai.thepredict.common.withUserIdGetter
import kotlinx.rpc.krpc.ktor.server.KrpcRoute

fun KrpcRoute.registerContactsRemoteServices() {
    registerService<ContactsRemoteService> { ctx ->
        ContactsRemoteServiceImpl(ctx, withUserIdGetter())
    }
}