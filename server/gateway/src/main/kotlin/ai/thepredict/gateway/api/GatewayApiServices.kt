package ai.thepredict.gateway.api

import ai.thepredict.configuration.ServerEndpoints
import ai.thepredict.gateway.client.createClient
import ai.thepredict.shared.api.ContactsApi
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.ktor.server.RPCRoute

fun RPCRoute.registerServices() {
    registerService<ContactsApi> { ctx ->
        ContactsApiImpl(ctx)
    }
}