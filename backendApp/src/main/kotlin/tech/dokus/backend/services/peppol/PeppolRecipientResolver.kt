package tech.dokus.backend.services.peppol

import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.database.repository.peppol.PeppolDirectoryCacheRepository
import tech.dokus.domain.enums.PeppolLookupSource
import tech.dokus.domain.enums.PeppolLookupStatus
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.PeppolResolution
import tech.dokus.domain.model.PeppolStatusResponse
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.peppol.provider.client.RecommandProvider
import tech.dokus.peppol.service.PeppolCredentialResolver

private const val ERROR_MESSAGE_MAX_LENGTH = 500

/**
 * Resolves PEPPOL recipient status for contacts.
 *
 * Uses a cache-first approach:
 * 1. Check cache entry
 * 2. If cache valid (not expired AND snapshots match), return cached
 * 3. If forceRefresh OR cache stale, call Recommand directory search
 * 4. Store result with TTL (14 days for Found/NotFound, 1 day for Error)
 *
 * Staleness detection:
 * - Cache is stale if expiresAt is in the past
 * - Cache is stale if vatNumberSnapshot != contact's current vatNumber
 * - Cache is stale if companyNumberSnapshot != contact's current companyNumber
 */
class PeppolRecipientResolver(
    private val cacheRepository: PeppolDirectoryCacheRepository,
    private val contactRepository: ContactRepository,
    private val credentialResolver: PeppolCredentialResolver,
    private val recommandProvider: RecommandProvider
) {
    private val logger = loggerFor()

    /**
     * Resolve PEPPOL recipient status for a contact.
     *
     * @param tenantId The tenant ID
     * @param contactId The contact ID
     * @param forceRefresh If true, always call Recommand even if cache is valid
     * @return Result containing the resolution (with refreshed=true if fetched this request)
     */
    suspend fun resolveRecipient(
        tenantId: TenantId,
        contactId: ContactId,
        forceRefresh: Boolean = false
    ): Result<Pair<PeppolResolution?, Boolean>> = runCatching {
        logger.debug("Resolving PEPPOL recipient for contact $contactId (forceRefresh=$forceRefresh)")

        // Get contact to check current identifiers
        val contact = contactRepository.getContact(contactId, tenantId).getOrNull()
            ?: return@runCatching Pair(null, false)

        val currentVat = contact.vatNumber?.value
        val currentCompanyNumber = contact.companyNumber

        // Check cache and return if valid
        val cached = cacheRepository.getByContactId(tenantId, contactId).getOrNull()
        if (!needsRefresh(forceRefresh, cached, currentVat, currentCompanyNumber)) {
            logger.debug("Returning cached PEPPOL resolution for contact $contactId")
            return@runCatching Pair(cached, false)
        }

        // Need to refresh - fetch from directory
        logger.info("Fetching fresh PEPPOL status for contact $contactId")
        val resolution = fetchAndCacheResolution(
            tenantId,
            contactId,
            currentVat,
            currentCompanyNumber
        )
        Pair(resolution, true)
    }

    private fun needsRefresh(
        forceRefresh: Boolean,
        cached: PeppolResolution?,
        currentVat: String?,
        currentCompanyNumber: String?
    ): Boolean = when {
        forceRefresh -> true
        cached == null -> true
        cacheRepository.isStale(cached, currentVat, currentCompanyNumber) -> true
        else -> false
    }

    private suspend fun fetchAndCacheResolution(
        tenantId: TenantId,
        contactId: ContactId,
        currentVat: String?,
        currentCompanyNumber: String?
    ): PeppolResolution {
        // Get query identifier (prefer VAT, fallback to company number)
        val query = currentVat ?: currentCompanyNumber
        if (query.isNullOrBlank()) {
            return cacheNotFoundNoIdentifier(tenantId, contactId, currentVat, currentCompanyNumber)
        }

        // Configure provider with credentials from credential resolver
        val credentials = runCatching { credentialResolver.resolve(tenantId) }.getOrNull()
        if (credentials == null) {
            return cacheErrorNoSettings(tenantId, contactId, currentVat, currentCompanyNumber)
        }

        // Configure provider
        recommandProvider.configure(credentials)

        // Search directory and cache result
        return searchAndCacheResult(tenantId, contactId, query, currentVat, currentCompanyNumber)
    }

    private suspend fun cacheNotFoundNoIdentifier(
        tenantId: TenantId,
        contactId: ContactId,
        currentVat: String?,
        currentCompanyNumber: String?
    ): PeppolResolution {
        logger.debug("Contact $contactId has no VAT or company number for PEPPOL lookup")
        return cacheRepository.upsert(
            tenantId = tenantId,
            contactId = contactId,
            status = PeppolLookupStatus.NotFound,
            participantId = null,
            scheme = null,
            supportedDocTypes = emptyList(),
            source = PeppolLookupSource.Directory,
            vatNumberSnapshot = currentVat,
            companyNumberSnapshot = currentCompanyNumber,
            errorMessage = "No VAT or company number available"
        ).getOrThrow()
    }

    private suspend fun cacheErrorNoSettings(
        tenantId: TenantId,
        contactId: ContactId,
        currentVat: String?,
        currentCompanyNumber: String?
    ): PeppolResolution {
        logger.warn("No PEPPOL settings configured for tenant $tenantId")
        return cacheRepository.upsert(
            tenantId = tenantId,
            contactId = contactId,
            status = PeppolLookupStatus.Error,
            participantId = null,
            scheme = null,
            supportedDocTypes = emptyList(),
            source = PeppolLookupSource.Directory,
            vatNumberSnapshot = currentVat,
            companyNumberSnapshot = currentCompanyNumber,
            errorMessage = "PEPPOL not configured for tenant"
        ).getOrThrow()
    }

    private suspend fun searchAndCacheResult(
        tenantId: TenantId,
        contactId: ContactId,
        query: String,
        currentVat: String?,
        currentCompanyNumber: String?
    ): PeppolResolution {
        val searchResult = recommandProvider.searchDirectory(query)

        return if (searchResult.isSuccess) {
            cacheSearchSuccess(tenantId, contactId, searchResult.getOrThrow(), currentVat, currentCompanyNumber)
        } else {
            cacheSearchError(tenantId, contactId, searchResult.exceptionOrNull(), currentVat, currentCompanyNumber)
        }
    }

    private suspend fun cacheSearchSuccess(
        tenantId: TenantId,
        contactId: ContactId,
        results: List<tech.dokus.peppol.provider.client.PeppolDirectorySearchResult>,
        currentVat: String?,
        currentCompanyNumber: String?
    ): PeppolResolution {
        return if (results.isNotEmpty()) {
            val match = results.first()
            val scheme = match.peppolAddress.substringBefore(":", "")
            logger.info("Found PEPPOL participant for contact $contactId: ${match.peppolAddress}")

            cacheRepository.upsert(
                tenantId = tenantId,
                contactId = contactId,
                status = PeppolLookupStatus.Found,
                participantId = match.peppolAddress,
                scheme = scheme.takeIf { it.isNotEmpty() },
                supportedDocTypes = match.supportedDocumentTypes,
                source = PeppolLookupSource.Directory,
                vatNumberSnapshot = currentVat,
                companyNumberSnapshot = currentCompanyNumber,
                errorMessage = null
            ).getOrThrow()
        } else {
            logger.debug("No PEPPOL participant found for contact $contactId")

            cacheRepository.upsert(
                tenantId = tenantId,
                contactId = contactId,
                status = PeppolLookupStatus.NotFound,
                participantId = null,
                scheme = null,
                supportedDocTypes = emptyList(),
                source = PeppolLookupSource.Directory,
                vatNumberSnapshot = currentVat,
                companyNumberSnapshot = currentCompanyNumber,
                errorMessage = null
            ).getOrThrow()
        }
    }

    private suspend fun cacheSearchError(
        tenantId: TenantId,
        contactId: ContactId,
        error: Throwable?,
        currentVat: String?,
        currentCompanyNumber: String?
    ): PeppolResolution {
        logger.error("PEPPOL directory search failed for contact $contactId", error)

        return cacheRepository.upsert(
            tenantId = tenantId,
            contactId = contactId,
            status = PeppolLookupStatus.Error,
            participantId = null,
            scheme = null,
            supportedDocTypes = emptyList(),
            source = PeppolLookupSource.Directory,
            vatNumberSnapshot = currentVat,
            companyNumberSnapshot = currentCompanyNumber,
            errorMessage = error?.message?.take(ERROR_MESSAGE_MAX_LENGTH)
        ).getOrThrow()
    }

    /**
     * Build API response from resolution.
     */
    fun toStatusResponse(resolution: PeppolResolution?, refreshed: Boolean): PeppolStatusResponse {
        return if (resolution == null) {
            PeppolStatusResponse(
                status = "unknown",
                participantId = null,
                supportedDocTypes = emptyList(),
                source = null,
                lastCheckedAt = null,
                expiresAt = null,
                refreshed = refreshed,
                errorMessage = null
            )
        } else {
            PeppolStatusResponse(
                status = resolution.status.dbValue,
                participantId = resolution.participantId,
                supportedDocTypes = resolution.supportedDocTypes,
                source = resolution.source.dbValue,
                lastCheckedAt = resolution.lastCheckedAt,
                expiresAt = resolution.expiresAt,
                refreshed = refreshed,
                errorMessage = resolution.errorMessage
            )
        }
    }
}
