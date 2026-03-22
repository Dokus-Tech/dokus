package tech.dokus.backend.services.documents.resolution

import tech.dokus.database.mapper.from
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.domain.Name
import tech.dokus.domain.enums.ContactSource
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactMatchScoreDto
import tech.dokus.domain.model.contact.SuggestedContactDto
import tech.dokus.domain.model.contact.UpdateContactRequest
import tech.dokus.domain.util.JaroWinkler
import tech.dokus.foundation.backend.utils.loggerFor

class ContactMatchingUtils(
    private val contactRepository: ContactRepository
) {
    private val logger = loggerFor()

    fun similarity(left: String, right: String): Double {
        return JaroWinkler.similarity(normalizeName(left), normalizeName(right))
    }

    fun formatScore(value: Double): String = String.format("%.2f", value)

    fun ContactDto.toSuggestedContact(
        vatMatch: Boolean,
        ibanMatch: Boolean,
        nameSimilarity: Double?,
        ambiguityCount: Int,
        reason: String
    ): SuggestedContactDto {
        val score = ContactMatchScoreDto(
            vatMatch = vatMatch,
            ibanMatch = ibanMatch,
            nameSimilarity = nameSimilarity ?: 0.0,
            ambiguityCount = ambiguityCount,
            cbeResult = null
        )
        return SuggestedContactDto(
            contactId = id,
            name = name.value,
            vatNumber = vatNumber,
            iban = iban,
            matchScore = score,
            reason = reason
        )
    }

    suspend fun maybeHealPaymentAliasContact(
        tenantId: TenantId,
        matchedContact: ContactDto,
        authoritativeName: String?
    ): ContactDto {
        val targetName = authoritativeName.orEmpty()
        if (targetName.isEmpty()) return matchedContact
        if (matchedContact.source != ContactSource.AI) return matchedContact

        val currentName = matchedContact.name.value
        if (!isPaymentTokenAlias(currentName)) return matchedContact
        if (isPaymentTokenAlias(targetName)) return matchedContact
        if (similarity(targetName, currentName) >= PaymentAliasRenameSimilarityThreshold) return matchedContact

        val candidateName = Name(targetName)
        if (!candidateName.isValid) {
            logger.warn("Skipping payment alias heal: invalid name '{}' for contact {}", targetName, matchedContact.id)
            return matchedContact
        }

        val result = contactRepository.updateContact(
            contactId = matchedContact.id,
            tenantId = tenantId,
            request = UpdateContactRequest(name = candidateName)
        ).map { ContactDto.from(it) }
        return result.getOrElse { e ->
            logger.warn("Failed to heal payment alias for contact {}: {}", matchedContact.id, e.message)
            matchedContact
        }.also { healed ->
            if (healed.id == matchedContact.id && healed.name.value == targetName) {
                logger.info(
                    "Healed payment alias contact {}: '{}' -> '{}'",
                    matchedContact.id,
                    currentName,
                    targetName
                )
            }
        }
    }

    private fun isPaymentTokenAlias(value: String): Boolean {
        return paymentTokenKeywordPattern.containsMatchIn(value) && maskedOrTailPattern.containsMatchIn(value)
    }

    private fun normalizeName(value: String): String {
        return value.lowercase()
            .replace(nonAlphanumericPattern, " ")
            .replace(multiSpacePattern, " ")
            .trim()
    }

    companion object {
        const val StrongNameThreshold = 0.90
        const val SuggestionThreshold = 0.80
        const val PaymentAliasRenameSimilarityThreshold = 0.70

        private val paymentTokenKeywordPattern = Regex(
            "\\b(visa|mastercard|apple\\s*pay|google\\s*pay|amex|american\\s*express|bancontact|card)\\b",
            RegexOption.IGNORE_CASE
        )
        private val maskedOrTailPattern = Regex("(\\*{2,}|\\.{2,}|[*.]+\\s*\\d{4}\\b)")
        private val nonAlphanumericPattern = Regex("[^a-z0-9\\s]")
        private val multiSpacePattern = Regex("\\s+")
    }
}
