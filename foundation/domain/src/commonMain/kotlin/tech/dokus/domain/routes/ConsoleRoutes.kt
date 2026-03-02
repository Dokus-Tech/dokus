package tech.dokus.domain.routes

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.ids.TenantId

/**
 * Type-safe route definitions for Bookkeeper Console API.
 * Base path: /api/v1/console
 *
 * SECURITY: All operations require authentication. Data is firm-scoped and
 * requires an active firm-to-client access relation.
 */
@Serializable
@Resource("/api/v1/console")
class Console {
    /**
     * GET /api/v1/console/clients
     * List tenants the current firm can access via Bookkeeper Console.
     */
    @Serializable
    @Resource("clients")
    class Clients(val parent: Console = Console())

    @Serializable
    @Resource("clients/{tenantId}")
    class Client(
        val parent: Console = Console(),
        val tenantId: TenantId
    ) {
        @Serializable
        @Resource("documents")
        @Suppress("LongParameterList")
        class Documents(
            val parent: Client,
            val filter: DocumentListFilter? = null,
            val documentStatus: DocumentStatus? = null,
            val documentType: DocumentType? = null,
            val ingestionStatus: IngestionStatus? = null,
            val page: Int = 0,
            val limit: Int = 20
        )

        @Serializable
        @Resource("documents/{documentId}")
        class Document(
            val parent: Client,
            val documentId: String
        )
    }

    @Serializable
    @Resource("invite-links")
    class InviteLinks(val parent: Console = Console()) {
        @Serializable
        @Resource("resolve")
        class Resolve(
            val parent: InviteLinks = InviteLinks(),
            val token: String
        )

        @Serializable
        @Resource("accept")
        class Accept(val parent: InviteLinks = InviteLinks())
    }
}
