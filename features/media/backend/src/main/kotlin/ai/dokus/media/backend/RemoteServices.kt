package ai.dokus.media.backend

import ai.dokus.foundation.domain.rpc.MediaRemoteService
import ai.dokus.foundation.ktor.security.AuthInfoProvider
import ai.dokus.foundation.ktor.security.JwtValidator
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import org.koin.core.parameter.parametersOf
import org.koin.ktor.ext.get
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import io.ktor.server.routing.Route

private val logger = LoggerFactory.getLogger("RemoteServices")

fun Route.withRemoteServices() {
    val jwtValidator by inject<JwtValidator>()

    rpc("/rpc") {
        rpcConfig {
            serialization {
                json()
            }
        }

        registerService<MediaRemoteService> {
            val authInfoProvider = AuthInfoProvider(call, jwtValidator)
            get<MediaRemoteService> { parametersOf(authInfoProvider) }
        }
    }

    logger.info("Media RPC APIs registered at /rpc")
}
