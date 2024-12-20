package ai.thepredict.contacts

import ai.thepredict.configuration.ServerEndpoints
import ai.thepredict.contacts.api.ContactsRemoteService
import ai.thepredict.contacts.api.ContactsRemoteServiceImpl
import ai.thepredict.contacts.api.registerContactsRemoteServices
import ai.thepredict.contacts.api.registerRemoteServices
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.rpc.krpc.ktor.server.RPC
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json

fun main() {
    embeddedServer(
        Netty,
        port = ServerEndpoints.Contacts.internalPort,
        host = "0.0.0.0"
    ) {
//        install(WebSockets) {
//            pingPeriod = 15.seconds
//            timeout = 15.seconds
//            maxFrameSize = Long.MAX_VALUE
//            masking = false
//        }
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(RPC)

    routing {
        rpc {
            rpcConfig {
                serialization {
                    json()
                }
            }

            registerContactsRemoteServices()
        }

        get("/info") {
            call.respondText("Contacts: Hey")
        }
    }
}