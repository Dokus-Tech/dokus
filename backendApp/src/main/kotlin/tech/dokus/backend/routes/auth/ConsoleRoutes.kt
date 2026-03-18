package tech.dokus.backend.routes.auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory
import tech.dokus.backend.routes.cashflow.documents.addDownloadUrl
import tech.dokus.backend.routes.cashflow.documents.toDto
import tech.dokus.backend.security.requireAnyRole
import tech.dokus.backend.security.requireFirmAccess
import tech.dokus.backend.security.requireFirmClientAccess
import tech.dokus.backend.security.requireTenantAccess
import tech.dokus.backend.services.admin.ConsoleService
import tech.dokus.backend.services.auth.FirmInviteTokenService
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.auth.AcceptFirmInviteRequest
import tech.dokus.domain.model.auth.AcceptFirmInviteResponse
import tech.dokus.domain.model.auth.ResolveFirmInviteResponse
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.routes.Console
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
import tech.dokus.foundation.backend.storage.DocumentStorageService as MinioDocumentStorageService

internal fun Route.consoleRoutes() {
    val consoleService by inject<ConsoleService>()
    val minioStorage by inject<MinioDocumentStorageService>()
    val inviteTokenService by inject<FirmInviteTokenService>()
    val logger = LoggerFactory.getLogger("ConsoleRoutes")

    authenticateJwt {
        /**
         * GET /api/v1/console/clients
         * Lists tenant summaries accessible through active firm access links.
         */
        get<Console.Clients> {
            val firmAccess = requireFirmAccess()
            val clients = consoleService.listClientSummaries(firmAccess.firmId)
            call.respond(HttpStatusCode.OK, clients)
        }

        /**
         * GET /api/v1/console/clients/{tenantId}/documents
         */
        get<Console.Client.Documents> { route ->
            requireFirmClientAccess(
                firmRepository = consoleService.firmRepositoryForAccessCheck,
                tenantId = route.parent.tenantId
            )

            val tenantId = route.parent.tenantId
            val page = route.page.coerceAtLeast(0)
            val limit = route.limit.coerceIn(1, 100)
            val filter = route.filter

            if (filter != null && (route.documentStatus != null || route.ingestionStatus != null)) {
                throw DokusException.BadRequest("Do not combine 'filter' with 'documentStatus' or 'ingestionStatus'")
            }

            val (documentsWithInfo, total) = consoleService.listDocuments(
                tenantId = tenantId,
                filter = filter,
                documentStatus = route.documentStatus,
                documentType = route.documentType,
                ingestionStatus = route.ingestionStatus,
                page = page,
                limit = limit,
            )

            val records = documentsWithInfo.map { docInfo ->
                val documentWithUrl = addDownloadUrl(docInfo.document, null, minioStorage, logger)

                DocumentDetailDto(
                    document = documentWithUrl,
                    draft = docInfo.draft?.toDto(),
                    latestIngestion = docInfo.latestIngestion?.toDto(),
                                    )
            }

            call.respond(
                HttpStatusCode.OK,
                PaginatedResponse(
                    items = records,
                    total = total,
                    limit = limit,
                    offset = page * limit
                )
            )
        }

        /**
         * GET /api/v1/console/clients/{tenantId}/documents/{documentId}
         */
        get<Console.Client.Document> { route ->
            requireFirmClientAccess(
                firmRepository = consoleService.firmRepositoryForAccessCheck,
                tenantId = route.parent.tenantId
            )

            val tenantId = route.parent.tenantId
            val documentId = DocumentId.parse(route.documentId)

            val document = consoleService.getDocument(tenantId, documentId)
                ?: throw DokusException.NotFound("Document not found")
            val documentWithUrl = addDownloadUrl(document, null, minioStorage, logger)
            val draft = consoleService.getDraft(documentId, tenantId)
            val latestIngestion = consoleService.getLatestIngestion(documentId, tenantId)

            call.respond(
                HttpStatusCode.OK,
                DocumentDetailDto(
                    document = documentWithUrl,
                    draft = draft?.toDto(),
                    latestIngestion = latestIngestion?.toDto(
                        includeRawExtraction = true,
                        includeTrace = true
                    ),
                                    )
            )
        }

        /**
         * GET /api/v1/console/invite-links/resolve?token=...
         */
        get<Console.InviteLinks.Resolve> { route ->
            val payload = inviteTokenService.parse(route.token)
            val firm = consoleService.findFirmById(payload.firmId)
                ?: throw DokusException.NotFound("Firm not found")

            call.respond(
                HttpStatusCode.OK,
                ResolveFirmInviteResponse(
                    firmId = firm.id,
                    firmName = firm.name,
                    firmVatNumber = firm.vatNumber,
                    expiresAt = payload.expiresAt.toLocalDateTime(TimeZone.UTC)
                )
            )
        }

        /**
         * POST /api/v1/console/invite-links/accept
         * Body: { token }
         */
        post<Console.InviteLinks.Accept> {
            val principal = dokusPrincipal
            val request = call.receive<AcceptFirmInviteRequest>()
            val payload = inviteTokenService.parse(request.token)
            val tenantAccess = requireTenantAccess()
                .requireAnyRole(UserRole.Admin, UserRole.Owner)
            val tenantId = tenantAccess.tenantId

            val activated = consoleService.activateFirmAccess(
                firmId = payload.firmId,
                tenantId = tenantId,
                grantedByUserId = principal.userId,
            )

            if (activated) {
                logger.info(
                    "Firm access granted: firmId={}, tenantId={}, grantedBy={}",
                    payload.firmId,
                    tenantId,
                    principal.userId,
                )
            }

            call.respond(
                HttpStatusCode.OK,
                AcceptFirmInviteResponse(
                    firmId = payload.firmId,
                    tenantId = tenantId,
                    activated = activated,
                )
            )
        }
    }
}
