package tech.dokus.domain.routes

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId

@Serializable
@Resource("/api/v1/firms")
class Firms {
    @Serializable
    @Resource("")
    class Create(val parent: Firms = Firms())

    @Serializable
    @Resource("{firmId}")
    class ById(
        val parent: Firms = Firms(),
        val firmId: FirmId
    ) {
        @Serializable
        @Resource("invite-links")
        class InviteLinks(val parent: ById)

        @Serializable
        @Resource("clients/{tenantId}/revoke")
        class RevokeClientAccess(
            val parent: ById,
            val tenantId: TenantId,
        )
    }
}
