package ai.thepredict.contacts.api

import ai.thepredict.common.withUserIdGetter
import kotlinx.rpc.krpc.ktor.server.RPCRoute

fun RPCRoute.registerContactsRemoteServices() {
    registerService<ContactsRemoteService> { ctx ->
        ContactsRemoteServiceImpl(ctx, withUserIdGetter())
    }
}