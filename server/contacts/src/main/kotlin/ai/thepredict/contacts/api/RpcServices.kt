package ai.thepredict.contacts.api

import kotlinx.rpc.krpc.ktor.server.RPCRoute

fun RPCRoute.registerContactsRemoteServices() {
    registerService<ContactsRemoteService> { ctx ->
        ContactsRemoteServiceImpl(ctx)
    }
}