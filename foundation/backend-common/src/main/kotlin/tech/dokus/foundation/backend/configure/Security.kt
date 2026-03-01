package tech.dokus.foundation.backend.configure

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.hsts.HSTS
import io.ktor.server.plugins.requestvalidation.RequestValidation
import tech.dokus.foundation.backend.config.SecurityConfig
import kotlin.time.Duration.Companion.days

fun Application.configureSecurity(securityConfig: SecurityConfig) {
    withRequestValidation()
    withCors(securityConfig.cors)
    withDefaultHeaders()
    withHsts()
    withProxySupport()
    withCallId()
}

fun Application.withCors(corsConfig: SecurityConfig.Cors) {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)

        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowHeader(HttpHeaders.XRequestId)
        allowHeader("X-Tenant-Id")

        allowCredentials = true
        maxAgeInSeconds = 3600

        // Handle "*" for allowing any host (development/internal deployments)
        if (corsConfig.allowedHosts.contains("*")) {
            anyHost()
        } else {
            corsConfig.allowedHosts.forEach { allowHost(host = it, schemes = listOf("http", "https")) }
        }
    }
}

fun Application.withDefaultHeaders() {
    install(DefaultHeaders) {
        header("X-Content-Type-Options", "nosniff")
        header("X-Frame-Options", "DENY")
        header("X-XSS-Protection", "1; mode=block")
        header("Referrer-Policy", "strict-origin-when-cross-origin")
        header("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline';")
        header("Permissions-Policy", "geolocation=(), microphone=(), camera=()")
        header("Cache-Control", "no-store, no-cache, must-revalidate, private")
    }
}

fun Application.withCallId() {
    install(CallId) {
        header(HttpHeaders.XRequestId)
        generate {
            "req-${System.currentTimeMillis()}-${(1000..9999).random()}"
        }
        verify { callId: String ->
            callId.isNotEmpty()
        }
    }
}

fun Application.withProxySupport() {
    install(ForwardedHeaders)
    install(XForwardedHeaders)
}

fun Application.withHsts() {
    install(HSTS) {
        includeSubDomains = true
        maxAgeInSeconds = 365.days.inWholeSeconds
        preload = true
    }
}

fun Application.withRequestValidation() {
    install(RequestValidation) {
        // TODO: Body size limit - 1MB max
    }
}
