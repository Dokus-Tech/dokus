package tech.dokus.backend.security

import io.ktor.server.routing.RoutingContext
import io.ktor.util.AttributeKey
import tech.dokus.database.repository.auth.FirmRepository
import tech.dokus.domain.enums.FirmRole
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.foundation.backend.security.dokusPrincipal

const val FirmHeaderName = "X-Firm-Id"

private val FirmAccessAttributeKey = AttributeKey<FirmAccessContext>("dokus.firm.access")
private val FirmClientAccessAttributeKey = AttributeKey<FirmClientAccessContext>("dokus.firm.client.access")

data class FirmAccessContext(
    val firmId: FirmId,
    val role: FirmRole,
)

data class FirmClientAccessContext(
    val firmId: FirmId,
    val tenantId: TenantId,
    val role: FirmRole,
)

val RoutingContext.firmAccessOrNull: FirmAccessContext?
    get() = if (call.attributes.contains(FirmAccessAttributeKey)) {
        call.attributes[FirmAccessAttributeKey]
    } else {
        null
    }

suspend fun RoutingContext.requireFirmAccess(): FirmAccessContext {
    firmAccessOrNull?.let { return it }

    val firmId = resolveFirmIdFromRequest()
    val membership = dokusPrincipal.firmMemberships
        .firstOrNull { it.firmId == firmId }
        ?: throw DokusException.NotAuthorized("You do not have access to firm $firmId")

    return FirmAccessContext(
        firmId = firmId,
        role = membership.role,
    ).also {
        call.attributes.put(FirmAccessAttributeKey, it)
    }
}

suspend fun RoutingContext.requireFirmClientAccess(
    firmRepository: FirmRepository,
    tenantId: TenantId = resolveTenantIdFromRequest(),
): FirmClientAccessContext {
    if (call.attributes.contains(FirmClientAccessAttributeKey)) {
        return call.attributes[FirmClientAccessAttributeKey]
    }

    val firmAccess = requireFirmAccess()
    val hasAccess = firmRepository.hasActiveAccess(firmAccess.firmId, tenantId)
    if (!hasAccess) {
        throw DokusException.NotAuthorized(
            "Firm ${firmAccess.firmId} does not have active access to tenant $tenantId"
        )
    }

    return FirmClientAccessContext(
        firmId = firmAccess.firmId,
        tenantId = tenantId,
        role = firmAccess.role,
    ).also {
        call.attributes.put(FirmClientAccessAttributeKey, it)
    }
}

fun FirmAccessContext.requireAnyRole(vararg roles: FirmRole): FirmAccessContext {
    if (!roles.contains(role)) {
        val required = roles.joinToString(", ") { it.dbValue }
        throw DokusException.NotAuthorized("One of roles [$required] is required")
    }
    return this
}

private fun RoutingContext.resolveFirmIdFromRequest(): FirmId {
    val firmRaw = call.request.headers[FirmHeaderName]
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: call.parameters["firmId"]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        ?: call.parameters["firm_id"]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        ?: throw DokusException.BadRequest("Missing required header $FirmHeaderName")

    return runCatching { FirmId.parse(firmRaw) }
        .getOrElse { throw DokusException.BadRequest("Invalid firm id in $FirmHeaderName") }
}

private fun RoutingContext.resolveTenantIdFromRequest(): TenantId {
    val tenantRaw = call.request.headers[TenantHeaderName]
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: call.parameters["tenantId"]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        ?: call.parameters["tenant_id"]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        ?: throw DokusException.BadRequest("Missing tenant id")

    return runCatching { TenantId.parse(tenantRaw) }
        .getOrElse { throw DokusException.BadRequest("Invalid tenant id") }
}
