package ai.thepredict.gateway

import kotlinx.rpc.krpc.ktor.server.RPCRoute
import kotlinx.rpc.krpc.serialization.json.json

fun RPCRoute.configure() {
    rpcConfig {
        serialization {
            json()
        }
    }
}