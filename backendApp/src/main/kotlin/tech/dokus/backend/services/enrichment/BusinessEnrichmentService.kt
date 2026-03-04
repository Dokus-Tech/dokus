package tech.dokus.backend.services.enrichment

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.database.repository.enrichment.BusinessDescriptionRepository
import tech.dokus.database.repository.enrichment.BusinessDescriptionRow
import tech.dokus.domain.enums.EnrichmentEntityType
import tech.dokus.domain.enums.EnrichmentStatus
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.features.ai.agents.BusinessEnrichmentAgent
import tech.dokus.features.ai.models.EnrichBusinessInput
import tech.dokus.foundation.backend.storage.AvatarStorageService
import tech.dokus.foundation.backend.utils.loggerFor
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

/**
 * Service orchestrating business enrichment:
 * 1. Creates pending enrichment records
 * 2. Runs AI agent to find website, summary, activities, logo
 * 3. Downloads and stores logo via AvatarStorageService
 * 4. Updates database records
 */
@OptIn(ExperimentalUuidApi::class)
class BusinessEnrichmentService(
    private val enrichmentRepo: BusinessDescriptionRepository,
    private val enrichmentAgent: BusinessEnrichmentAgent,
    private val tenantRepository: TenantRepository,
    private val contactRepository: ContactRepository,
    private val avatarStorageService: AvatarStorageService,
    private val httpClient: HttpClient
) {
    private val logger = loggerFor()

    /**
     * Dispatch enrichment for an entity. Creates a PENDING record if none exists.
     * The actual processing happens via the background worker.
     */
    suspend fun dispatchEnrichment(
        tenantId: TenantId,
        entityType: EnrichmentEntityType,
        entityId: UUID,
        companyName: String,
        vatNumber: String? = null
    ) {
        val id = enrichmentRepo.createPending(tenantId, entityType, entityId)
        if (id != null) {
            logger.info("Dispatched enrichment for $entityType:$entityId (tenant=$tenantId)")
        }
    }

    /**
     * Process a single enrichment record. Called by the background worker.
     */
    suspend fun processEnrichment(record: BusinessDescriptionRow) {
        val tenantId = TenantId.parse(record.tenantId.toString())

        try {
            logger.info("Processing enrichment ${record.id} for ${record.entityType}:${record.entityId}")

            // Determine company name from tenant or contact
            val companyName = resolveCompanyName(record, tenantId)
            if (companyName.isNullOrBlank()) {
                logger.warn("No company name found for enrichment ${record.id}, marking as failed")
                enrichmentRepo.markFailed(record.id, tenantId)
                return
            }

            // Resolve VAT number and country if available
            val vatNumber = resolveVatNumber(record, tenantId)

            // Run AI agent
            val input = EnrichBusinessInput(
                companyName = companyName,
                vatNumber = vatNumber,
                country = null, // Could be resolved from address in future
                entityType = record.entityType,
                entityId = record.entityId.toString(),
                tenantId = record.tenantId.toString()
            )

            val result = enrichmentAgent.enrich(input)

            // Store logo if found (non-fatal)
            val logoUrl = result.logoUrl
            if (!logoUrl.isNullOrBlank()) {
                runCatching {
                    downloadAndStoreLogo(tenantId, record.entityType, record.entityId, logoUrl)
                }.onFailure { e ->
                    logger.warn("Failed to download/store logo for enrichment ${record.id}: ${e.message}")
                }
            }

            // Update company website on tenant settings if applicable
            if (!result.websiteUrl.isNullOrBlank() && record.entityType == EnrichmentEntityType.Tenant) {
                runCatching {
                    tenantRepository.updateCompanyWebsite(tenantId, result.websiteUrl)
                }.onFailure { e ->
                    logger.warn("Failed to update company website: ${e.message}")
                }
            }

            // Mark enrichment as completed
            enrichmentRepo.markCompleted(
                id = record.id,
                tenantId = tenantId,
                websiteUrl = result.websiteUrl,
                summary = result.summary,
                activities = result.activities
            )

            logger.info("Enrichment ${record.id} completed: website=${result.websiteUrl != null}, summary=${result.summary != null}")

        } catch (e: Exception) {
            logger.error("Enrichment ${record.id} failed", e)
            enrichmentRepo.markFailed(record.id, tenantId)
        }
    }

    /**
     * Download a logo from URL, convert if needed, and store via AvatarStorageService.
     */
    private suspend fun downloadAndStoreLogo(
        tenantId: TenantId,
        entityType: EnrichmentEntityType,
        entityId: UUID,
        logoUrl: String
    ) {
        val response = httpClient.get(logoUrl) {
            header("User-Agent", "Mozilla/5.0 (compatible; DokusBot/1.0)")
        }

        val bytes = response.bodyAsBytes()
        if (bytes.isEmpty()) {
            logger.warn("Empty logo response from $logoUrl")
            return
        }

        val contentTypeHeader = response.headers[HttpHeaders.ContentType]
        val contentType = contentTypeHeader?.let {
            runCatching { ContentType.parse(it) }.getOrNull()?.let { ct -> "${ct.contentType}/${ct.contentSubtype}" }
        } ?: guessContentType(logoUrl)

        // Skip unsupported formats (SVG, ICO) - AvatarStorageService only accepts jpeg/png/webp/gif
        val supportedTypes = setOf("image/jpeg", "image/png", "image/webp", "image/gif")
        if (contentType !in supportedTypes) {
            logger.info("Skipping logo with unsupported format: $contentType from $logoUrl")
            return
        }

        val uploadResult = avatarStorageService.uploadAvatar(tenantId, bytes, contentType)

        // Store the avatar key on the entity
        when (entityType) {
            EnrichmentEntityType.Tenant -> {
                tenantRepository.updateAvatarStorageKey(tenantId, uploadResult.storageKeyPrefix)
            }
            EnrichmentEntityType.Contact -> {
                val contactId = ContactId.parse(entityId.toString())
                contactRepository.updateAvatarStorageKey(contactId, tenantId, uploadResult.storageKeyPrefix)
            }
        }

        logger.info("Logo stored for $entityType:$entityId, key=${uploadResult.storageKeyPrefix}")
    }

    private suspend fun resolveCompanyName(record: BusinessDescriptionRow, tenantId: TenantId): String? {
        return when (record.entityType) {
            EnrichmentEntityType.Tenant -> {
                tenantRepository.findById(tenantId)?.legalName?.value
            }
            EnrichmentEntityType.Contact -> {
                val contactId = ContactId.parse(record.entityId.toString())
                contactRepository.getContact(contactId, tenantId).getOrNull()?.name?.value
            }
        }
    }

    private suspend fun resolveVatNumber(record: BusinessDescriptionRow, tenantId: TenantId): String? {
        return when (record.entityType) {
            EnrichmentEntityType.Tenant -> {
                tenantRepository.findById(tenantId)?.vatNumber?.value
            }
            EnrichmentEntityType.Contact -> {
                val contactId = ContactId.parse(record.entityId.toString())
                contactRepository.getContact(contactId, tenantId).getOrNull()?.vatNumber?.value
            }
        }
    }

    private fun guessContentType(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains(".png") -> "image/png"
            lower.contains(".jpg") || lower.contains(".jpeg") -> "image/jpeg"
            lower.contains(".webp") -> "image/webp"
            lower.contains(".gif") -> "image/gif"
            lower.contains(".svg") -> "image/svg+xml"
            lower.contains(".ico") -> "image/x-icon"
            else -> "application/octet-stream"
        }
    }
}
