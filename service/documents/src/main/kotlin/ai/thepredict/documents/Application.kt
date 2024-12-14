package ai.thepredict.documents

import ai.thepredict.shared.Greeting
import ai.thepredict.shared.ServerEndpoints
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main() {
    embeddedServer(
        Netty,
        port = ServerEndpoints.Documents.port,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    routing {
        get("/") {
            call.respondText("Documents: ${Greeting().greet()}")
        }
    }
}