package ai.thepredict.common

import kotlinx.rpc.krpc.ktor.server.KrpcRoute
import kotlinx.rpc.krpc.serialization.json.json

fun KrpcRoute.configureRpc() {
    rpcConfig {
        serialization {
            json()
        }
    }
}