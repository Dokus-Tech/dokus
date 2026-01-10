package tech.dokus.backend.services.contacts

import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.ContactMatchReason
import tech.dokus.domain.model.contact.ContactSuggestion
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Service for matching extracted counterparty data to existing contacts.
 *
 * Used during document processing to suggest which contact to link.
 * All matches are suggestions - user must confirm before linking.
 *
 * Matching priority (highest confidence first):
 * 1. VAT Number (exact match) → confidence 0.95+
 * 2. Company Number (exact match) → confidence 0.90
 * 3. Name + Country (fuzzy) → confidence 0.6-0.8
 * 4. Name only (fuzzy) → confidence 0.3-0.5
 * 5. No match → confidence 0.0
 *
 * NOTE: PEPPOL ID matching removed - PEPPOL participant IDs are now in PeppolDirectoryCacheTable
 */
class ContactMatchingService(
    private val contactRepository: ContactRepository
) {
    private val logger = loggerFor()

    /**
     * Extracted counterparty data from a document.
     * Used as input for matching.
     *
     * NOTE: peppolId is kept for extraction but not used for matching.
     * PEPPOL IDs are discovered via directory lookup, not contact master data.
     */
    data class ExtractedCounterparty(
        val name: String? = null,
        val vatNumber: String? = null,
        val peppolId: String? = null, // Extracted but not used for matching
        val companyNumber: String? = null,
        val country: String? = null,
        val email: String? = null,
        val address: String? = null
    )

    /**
     * Find the best matching contact for extracted counterparty data.
     *
     * Returns a [ContactSuggestion] with:
     * - Matched contact (if found)
     * - Confidence score (0.0 - 1.0)
     * - Reason for match
     * - Human-readable details
     *
     * @param tenantId The tenant to search within
     * @param extracted The extracted counterparty data from AI
     * @return ContactSuggestion with match result
     */
    suspend fun findMatch(
        tenantId: TenantId,
        extracted: ExtractedCounterparty
    ): Result<ContactSuggestion> = runCatching {
        logger.debug("Finding contact match for tenant: {}, extracted: {}", tenantId, extracted)

        // 1. Try VAT number (highest confidence)
        if (!extracted.vatNumber.isNullOrBlank()) {
            val match = contactRepository.findByVatNumber(tenantId, extracted.vatNumber).getOrNull()
            if (match != null) {
                logger.info("Matched contact by VAT: ${match.id} for VAT: ${extracted.vatNumber}")
                return@runCatching ContactSuggestion(
                    contactId = match.id,
                    contact = match,
                    confidence = 0.98f,
                    matchReason = ContactMatchReason.VatNumber,
                    matchDetails = "Matched VAT: ${extracted.vatNumber}"
                )
            }
        }

        // NOTE: PEPPOL ID matching removed - PEPPOL IDs are now in PeppolDirectoryCacheTable

        // 2. Try company number (high confidence)
        if (!extracted.companyNumber.isNullOrBlank()) {
            val match =
                contactRepository.findByCompanyNumber(tenantId, extracted.companyNumber).getOrNull()
            if (match != null) {
                logger.info("Matched contact by company number: ${match.id} for: ${extracted.companyNumber}")
                return@runCatching ContactSuggestion(
                    contactId = match.id,
                    contact = match,
                    confidence = 0.90f,
                    matchReason = ContactMatchReason.CompanyNumber,
                    matchDetails = "Matched company number: ${extracted.companyNumber}"
                )
            }
        }

        // 3. Try name (confidence based on country presence)
        // Note: Country filtering removed from contacts table (now in address table).
        // TODO: Re-implement country matching via JOIN with ContactAddressesTable/AddressTable.
        if (!extracted.name.isNullOrBlank()) {
            val matches = contactRepository.findByName(
                tenantId,
                extracted.name,
                limit = 1
            ).getOrNull()

            if (!matches.isNullOrEmpty()) {
                val match = matches.first()
                // Calculate confidence based on name similarity
                // Give higher confidence if we have country in extracted data (even though we can't filter by it yet)
                val baseMultiplier = if (!extracted.country.isNullOrBlank()) 0.6f else 0.5f
                val matchReason = if (!extracted.country.isNullOrBlank()) {
                    ContactMatchReason.NameAndCountry
                } else {
                    ContactMatchReason.NameOnly
                }
                val confidence = calculateNameSimilarity(extracted.name, match.name.value) * baseMultiplier
                if (confidence >= 0.25f) {
                    logger.info("Matched contact by name: ${match.id} for: ${extracted.name}")
                    return@runCatching ContactSuggestion(
                        contactId = match.id,
                        contact = match,
                        confidence = confidence,
                        matchReason = matchReason,
                        matchDetails = if (!extracted.country.isNullOrBlank()) {
                            "Matched name \"${match.name.value}\" (country: ${extracted.country})"
                        } else {
                            "Partial name match: \"${match.name.value}\""
                        }
                    )
                }
            }
        }

        // 4. No match found
        logger.debug("No contact match found for: {}", extracted)
        ContactSuggestion(
            contactId = null,
            contact = null,
            confidence = 0.0f,
            matchReason = ContactMatchReason.NoMatch,
            matchDetails = "No existing contact matched"
        )
    }

    /**
     * Calculate similarity between two names (0.0 - 1.0).
     * Uses a simple case-insensitive contains check for now.
     * Could be enhanced with Levenshtein distance or fuzzy matching.
     */
    private fun calculateNameSimilarity(extracted: String, stored: String): Float {
        val extractedNorm = extracted.lowercase().trim()
        val storedNorm = stored.lowercase().trim()

        return when {
            extractedNorm == storedNorm -> 1.0f
            storedNorm.contains(extractedNorm) -> 0.9f
            extractedNorm.contains(storedNorm) -> 0.8f
            else -> {
                // Simple word overlap check
                val extractedWords = extractedNorm.split(Regex("\\s+")).toSet()
                val storedWords = storedNorm.split(Regex("\\s+")).toSet()
                val overlap = extractedWords.intersect(storedWords).size
                val total = maxOf(extractedWords.size, storedWords.size)
                if (total > 0) overlap.toFloat() / total else 0.0f
            }
        }
    }
}
