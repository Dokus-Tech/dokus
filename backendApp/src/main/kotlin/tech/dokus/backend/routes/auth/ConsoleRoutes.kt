package tech.dokus.backend.routes.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.auth.UserRepository
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.model.auth.ConsoleClientSummary
import tech.dokus.domain.routes.Console
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal

internal fun Route.consoleRoutes() {
    val userRepository by inject<UserRepository>()
    val tenantRepository by inject<TenantRepository>()

    authenticateJwt {
        /**
         * GET /api/v1/console/clients
         * Lists tenant summaries available to the current user as ACCOUNTANT.
         */
        get<Console.Clients> {
            val principal = dokusPrincipal

            val accountantTenantIds = userRepository.getUserTenants(principal.userId)
                .filter { it.isActive && it.role == UserRole.Accountant }
                .map { it.tenantId }

            if (accountantTenantIds.isEmpty()) {
                call.respond(HttpStatusCode.OK, emptyList<ConsoleClientSummary>())
                return@get
            }

            val tenantsById = tenantRepository.findByIds(accountantTenantIds)
                .associateBy { it.id }

            val clients = accountantTenantIds
                .mapNotNull { id -> tenantsById[id] }
                .map { tenant ->
                    ConsoleClientSummary(
                        tenantId = tenant.id,
                        companyName = tenant.displayName,
                        vatNumber = tenant.vatNumber.takeIf { it.value.isNotBlank() }
                    )
                }
                .sortedBy { it.companyName.value.lowercase() }

            call.respond(HttpStatusCode.OK, clients)
        }
    }
}
