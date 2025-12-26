package ai.dokus.backend.services.contacts

import ai.dokus.foundation.database.repository.contacts.ContactRepository
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.ContactMatchReason
import ai.dokus.foundation.domain.model.ContactSuggestion
import org.slf4j.LoggerFactory

/**
 * Service for matching extracted counterparty data to existing contacts.
 *
 * Used during document processing to suggest which contact to link.
 * All matches are suggestions - user must confirm before linking.
 *
 * Matching priority (highest confidence first):
 * 1. VAT Number (exact match) → confidence 0.95+
 * 2. Peppol ID (exact match) → confidence 0.95+
 * 3. Company Number (exact match) → confidence 0.90
 * 4. Name + Country (fuzzy) → confidence 0.6-0.8
 * 5. Name only (fuzzy) → confidence 0.3-0.5
 * 6. No match → confidence 0.0
 */
class ContactMatchingService(
    private val contactRepository: ContactRepository
) {
    private val logger = LoggerFactory.getLogger(ContactMatchingService::class.java)

    /**
     * Extracted counterparty data from a document.
     * Used as input for matching.
     */
    data class ExtractedCounterparty(
        val name: String? = null,
        val vatNumber: String? = null,
        val peppolId: String? = null,
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
        logger.debug("Finding contact match for tenant: $tenantId, extracted: $extracted")

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

        // 2. Try Peppol ID (highest confidence)
        if (!extracted.peppolId.isNullOrBlank()) {
            val match = contactRepository.findByPeppolId(tenantId, extracted.peppolId).getOrNull()
            if (match != null) {
                logger.info("Matched contact by Peppol ID: ${match.id} for Peppol: ${extracted.peppolId}")
                return@runCatching ContactSuggestion(
                    contactId = match.id,
                    contact = match,
                    confidence = 0.97f,
                    matchReason = ContactMatchReason.PeppolId,
                    matchDetails = "Matched Peppol ID: ${extracted.peppolId}"
                )
            }
        }

        // 3. Try company number (high confidence)
        if (!extracted.companyNumber.isNullOrBlank()) {
            val match = contactRepository.findByCompanyNumber(tenantId, extracted.companyNumber).getOrNull()
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

        // 4. Try name + country (medium confidence)
        if (!extracted.name.isNullOrBlank() && !extracted.country.isNullOrBlank()) {
            val matches = contactRepository.findByName(
                tenantId,
                extracted.name,
                country = extracted.country,
                limit = 1
            ).getOrNull()

            if (!matches.isNullOrEmpty()) {
                val match = matches.first()
                // Calculate confidence based on name similarity
                val confidence = calculateNameSimilarity(extracted.name, match.name.value) * 0.8f
                if (confidence >= 0.5f) {
                    logger.info("Matched contact by name+country: ${match.id} for: ${extracted.name}")
                    return@runCatching ContactSuggestion(
                        contactId = match.id,
                        contact = match,
                        confidence = confidence,
                        matchReason = ContactMatchReason.NameAndCountry,
                        matchDetails = "Matched name \"${match.name.value}\" in ${extracted.country}"
                    )
                }
            }
        }

        // 5. Try name only (low confidence)
        if (!extracted.name.isNullOrBlank()) {
            val matches = contactRepository.findByName(
                tenantId,
                extracted.name,
                country = null,
                limit = 1
            ).getOrNull()

            if (!matches.isNullOrEmpty()) {
                val match = matches.first()
                val confidence = calculateNameSimilarity(extracted.name, match.name.value) * 0.5f
                if (confidence >= 0.25f) {
                    logger.info("Matched contact by name only: ${match.id} for: ${extracted.name}")
                    return@runCatching ContactSuggestion(
                        contactId = match.id,
                        contact = match,
                        confidence = confidence,
                        matchReason = ContactMatchReason.NameOnly,
                        matchDetails = "Partial name match: \"${match.name.value}\""
                    )
                }
            }
        }

        // 6. No match found
        logger.debug("No contact match found for: $extracted")
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
