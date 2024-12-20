package ai.thepredict.common

import kotlinx.rpc.krpc.ktor.server.RPCRoute
import kotlinx.rpc.krpc.serialization.json.json

fun RPCRoute.configureRpc() {
    rpcConfig {
        serialization {
            json()
        }
    }
}