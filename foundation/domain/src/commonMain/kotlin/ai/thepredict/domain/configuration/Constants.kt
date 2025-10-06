package ai.thepredict.domain.configuration

import ai.thepredict.app.platform.BuildConfig

/**
 * Server endpoint configuration using BuildKonfig
 *
 * Configure via gradle properties:
 * - Production (default): ./gradlew build -> api.thepredict.ai:443
 * - Local development: ./gradlew build -PENV=local -> 127.0.0.1:8000
 * - Android emulator: ./gradlew build -PENV=localAndroid -> 10.0.2.2:8000
 * - Custom: ./gradlew build -PAPI_HOST=staging.example.com -PAPI_PORT=8080
 */
object ServerEndpoint {
    val host: String = BuildConfig.API_HOST
    val port: Int = BuildConfig.API_PORT
    val isLocal: Boolean = BuildConfig.API_IS_LOCAL
}