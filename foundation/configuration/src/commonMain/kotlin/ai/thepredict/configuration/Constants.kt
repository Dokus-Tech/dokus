package ai.thepredict.configuration

import ai.thepredict.app.platform.BuildConfig

/**
 * Server endpoint configuration using BuildKonfig
 *
 * Configure via gradle properties:
 * - Production (default): ./gradlew build
 * - Local development: ./gradlew build -PENV=local
 * - Android emulator: ./gradlew build -PENV=localAndroid
 * - Custom: ./gradlew build -PAPI_HOST=staging.example.com -PAPI_PORT=8080
 */
object ServerEndpoint {
    val host: String = BuildConfig.API_HOST
    val port: Int? = if (BuildConfig.API_PORT == -1) null else BuildConfig.API_PORT
    val isLocal: Boolean = BuildConfig.API_IS_LOCAL
}