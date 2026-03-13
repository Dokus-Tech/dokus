package tech.dokus.backend.services.cashflow.matching

import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.foundation.backend.utils.loggerFor
import java.util.UUID

/**
 * Records confirmed and rejected matches into the learning tables
 * (MatchPatternsTable and RejectedMatchPairsTable).
 */
internal class MatchFeedbackStore(
    private val matchingRepository: MatchingRepository,
) {
    private val logger = loggerFor()

    /**
     * Record a confirmed match — updates the IBAN → contact pattern table.
     */
    suspend fun recordConfirmedMatch(
        tenantId: TenantId,
        counterpartyIban: String?,
        contactId: ContactId?,
    ) {
        if (counterpartyIban.isNullOrBlank() || contactId == null) return

        matchingRepository.upsertMatchPattern(
            tenantId = tenantId,
            counterpartyIban = counterpartyIban,
            contactId = contactId,
        )
        logger.debug("Recorded match pattern: iban={} → contact={}", counterpartyIban, contactId)
    }

    /**
     * Record a rejected match — blacklists this (transaction, document) pair.
     */
    suspend fun recordRejectedMatch(
        tenantId: TenantId,
        transactionId: BankTransactionId,
        documentId: DocumentId,
        rejectedBy: UUID?,
    ) {
        matchingRepository.insertRejectedPair(
            tenantId = tenantId,
            transactionId = transactionId,
            documentId = documentId,
            rejectedBy = rejectedBy,
        )
        logger.debug("Recorded rejected pair: tx={} × doc={}", transactionId, documentId)
    }
}
