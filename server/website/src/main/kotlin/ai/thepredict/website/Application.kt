package ai.thepredict.website

import ai.thepredict.common.embeddedServer
import ai.thepredict.configuration.ServerEndpoint
import io.ktor.server.http.content.ignoreFiles
import io.ktor.server.http.content.singlePageApplication
import io.ktor.server.routing.Routing


fun main() {
    embeddedServer(
        endpoint = ServerEndpoint.Website(),
        plugin = null,
        routing = Routing::configureRouting
    ).start()
}

private fun Routing.configureRouting() {
    singlePageApplication {
        useResources = true
        filesPath = "website"
        defaultPage = "index.html"
        ignoreFiles { it.endsWith(".txt") }
    }
}