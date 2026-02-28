package tech.dokus.backend.routes.cashflow

import tech.dokus.backend.security.requireTenantAccess

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import org.koin.ktor.ext.inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tech.dokus.backend.routes.cashflow.documents.addDownloadUrl
import tech.dokus.backend.services.documents.DocumentTruthService
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.domain.enums.DocumentIntakeOutcome
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.DocumentIntakeResult
import tech.dokus.domain.routes.Documents
import tech.dokus.foundation.backend.security.authenticateJwt
import tech.dokus.foundation.backend.security.dokusPrincipal
import tech.dokus.foundation.backend.storage.DocumentUploadValidator
import tech.dokus.foundation.backend.storage.DocumentStorageService as MinioDocumentStorageService

/** Allowed prefixes for document storage */
private val ALLOWED_PREFIXES = setOf("documents", "invoices", "expenses", "receipts")

/**
 * Document upload endpoint.
 *
 * POST /api/v1/documents/upload
 */
internal fun Route.documentUploadRoutes() {
    val minioStorage by inject<MinioDocumentStorageService>()
    val documentRepository by inject<DocumentRepository>()
    val truthService by inject<DocumentTruthService>()
    val uploadValidator by inject<DocumentUploadValidator>()
    val logger = LoggerFactory.getLogger("DocumentUploadRoutes")

    authenticateJwt {
        post<Documents.Upload> {
            val tenantId = requireTenantAccess().tenantId
            logger.info("Document upload request from tenant: $tenantId")

            val payload = call.readUploadPayload()
            validateUploadPayload(payload, uploadValidator, logger)

            val intake = truthService.intakeBytes(
                tenantId = tenantId,
                filename = payload.filename,
                contentType = payload.contentType,
                prefix = payload.prefix,
                fileBytes = payload.fileBytes,
                sourceChannel = DocumentSource.Upload
            )

            val document = documentRepository.getById(tenantId, intake.documentId)
                ?: throw DokusException.InternalError("Failed to retrieve intake document")

            val documentWithUrl = addDownloadUrl(
                document = document,
                minioStorage = minioStorage,
                logger = logger
            )

            val response = DocumentIntakeResult(
                document = documentWithUrl,
                intake = intake.toOutcomeDto()
            )

            val statusCode = when (intake.outcome) {
                DocumentIntakeOutcome.NewDocument -> HttpStatusCode.Created
                DocumentIntakeOutcome.LinkedToExisting,
                DocumentIntakeOutcome.PendingMatchReview -> HttpStatusCode.OK
            }

            call.respond(statusCode, response)
        }
    }
}

private data class UploadPayload(
    val fileBytes: ByteArray,
    val filename: String,
    val contentType: String,
    val prefix: String
)

private suspend fun ApplicationCall.readUploadPayload(): UploadPayload {
    val multipart = receiveMultipart()
    var fileBytes: ByteArray? = null
    var filename: String? = null
    var contentType: String? = null
    var prefix = "documents"

    multipart.forEachPart { part ->
        when (part) {
            is PartData.FileItem -> {
                filename = part.originalFileName ?: "unknown"
                contentType = part.contentType?.toString() ?: "application/octet-stream"
                fileBytes = part.readBytesWithLimit(DocumentUploadValidator.DEFAULT_MAX_FILE_SIZE_BYTES)
            }

            is PartData.FormItem -> {
                if (part.name == "prefix") {
                    val requestedPrefix = part.value.ifBlank { "documents" }
                    if (requestedPrefix !in ALLOWED_PREFIXES) {
                        throw DokusException.BadRequest(
                            "Invalid prefix. Allowed values: ${ALLOWED_PREFIXES.joinToString()}"
                        )
                    }
                    prefix = requestedPrefix
                }
            }

            else -> Unit
        }
        part.dispose()
    }

    if (fileBytes == null || filename == null) {
        throw DokusException.BadRequest("No file provided in request")
    }

    return UploadPayload(
        fileBytes = fileBytes!!,
        filename = filename!!,
        contentType = contentType ?: "application/octet-stream",
        prefix = prefix
    )
}

private fun validateUploadPayload(
    payload: UploadPayload,
    uploadValidator: DocumentUploadValidator,
    logger: Logger
) {
    val validationError = uploadValidator.validate(
        fileContent = payload.fileBytes,
        filename = payload.filename,
        mimeType = payload.contentType,
    )
    if (validationError != null) {
        logger.warn("File validation failed: $validationError")
        throw DokusException.Validation.Generic(validationError)
    }
}
