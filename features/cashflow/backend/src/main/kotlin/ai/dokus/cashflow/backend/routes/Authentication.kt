package ai.dokus.cashflow.backend.routes

// Re-export authentication utilities from foundation module for convenience
import ai.dokus.foundation.ktor.security.AuthMethod
import ai.dokus.foundation.ktor.security.DokusPrincipal
import ai.dokus.foundation.ktor.security.authenticateApiKey
import ai.dokus.foundation.ktor.security.authenticateJwt
import ai.dokus.foundation.ktor.security.authenticateJwtOptional
import ai.dokus.foundation.ktor.security.authenticatedTenantId
import ai.dokus.foundation.ktor.security.authenticatedUserId
import ai.dokus.foundation.ktor.security.dokusPrincipal
import ai.dokus.foundation.ktor.security.dokusPrincipalOrNull
import ai.dokus.foundation.ktor.security.withPrincipal
import ai.dokus.foundation.ktor.security.withTenant
