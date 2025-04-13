package ai.thepredict.gateway

import kotlinx.rpc.krpc.ktor.server.KrpcRoute
import kotlinx.rpc.krpc.serialization.json.json

fun KrpcRoute.configure() {
    rpcConfig {
        serialization {
            json()
        }
    }
}