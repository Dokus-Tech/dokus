package tech.dokus.backend.services.documents.postextraction

import tech.dokus.backend.services.documents.ContactResolutionService
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.contact.ContactResolution
import tech.dokus.domain.model.contact.CounterpartyInfo
import tech.dokus.domain.model.contact.CounterpartySnapshot
import tech.dokus.foundation.backend.utils.loggerFor

internal class ResolveDocumentContactUseCase(
    private val contactResolutionService: ContactResolutionService,
    private val documentRepository: DocumentRepository,
) {
    private val logger = loggerFor()

    suspend operator fun invoke(
        tenantId: TenantId,
        documentId: DocumentId,
        draftData: DocumentDraftData,
        authoritativeSnapshot: CounterpartySnapshot,
        tenantVat: VatNumber?,
    ): ContactId? {
        val resolution = contactResolutionService.resolve(
            tenantId = tenantId,
            draftData = draftData,
            authoritativeSnapshot = authoritativeSnapshot,
            tenantVat = tenantVat,
        )
        return when (val decision = resolution.resolution) {
            is ContactResolution.Matched -> {
                documentRepository.updateContactResolution(
                    documentId = documentId,
                    tenantId = tenantId,
                    counterpartySnapshot = resolution.snapshot,
                    counterparty = CounterpartyInfo.Linked(
                        contactId = decision.contactId,
                        source = ContactLinkSource.AI,
                        evidence = decision.evidence,
                    )
                )
                decision.contactId
            }

            is ContactResolution.AutoCreate -> {
                // Receipts should not auto-create contacts — one-off merchants
                // (restaurants, parking, etc.) would pollute the contacts list.
                if (draftData is ReceiptDraftData) {
                    logger.info("Skipping auto-create for receipt document {}", documentId)
                    documentRepository.updateContactResolution(
                        documentId = documentId,
                        tenantId = tenantId,
                        counterpartySnapshot = resolution.snapshot,
                        counterparty = CounterpartyInfo.Unresolved(snapshot = resolution.snapshot)
                    )
                    return null
                }

                val contactId = contactResolutionService.createContactFromResolution(
                    tenantId = tenantId,
                    resolution = decision,
                )
                documentRepository.updateContactResolution(
                    documentId = documentId,
                    tenantId = tenantId,
                    counterpartySnapshot = resolution.snapshot,
                    counterparty = if (contactId != null) {
                        CounterpartyInfo.Linked(
                            contactId = contactId,
                            source = ContactLinkSource.AI,
                            evidence = decision.evidence,
                        )
                    } else {
                        CounterpartyInfo.Unresolved(snapshot = resolution.snapshot)
                    }
                )
                contactId
            }

            is ContactResolution.Suggested -> {
                documentRepository.updateContactResolution(
                    documentId = documentId,
                    tenantId = tenantId,
                    counterpartySnapshot = resolution.snapshot,
                    counterparty = CounterpartyInfo.Unresolved(
                        suggestions = decision.candidates,
                    )
                )
                null
            }

            is ContactResolution.PendingReview -> {
                documentRepository.updateContactResolution(
                    documentId = documentId,
                    tenantId = tenantId,
                    counterpartySnapshot = resolution.snapshot,
                    counterparty = CounterpartyInfo.Unresolved()
                )
                null
            }
        }
    }
}
