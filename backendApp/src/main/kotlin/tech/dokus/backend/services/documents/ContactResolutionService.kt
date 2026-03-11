package tech.dokus.backend.services.documents

import tech.dokus.backend.services.contacts.ContactService
import tech.dokus.backend.services.documents.resolution.AutoCreateResolver
import tech.dokus.backend.services.documents.resolution.IbanNameResolver
import tech.dokus.backend.services.documents.resolution.NameSuggestionResolver
import tech.dokus.backend.services.documents.resolution.ResolverInput
import tech.dokus.backend.services.documents.resolution.ResolverOutcome
import tech.dokus.backend.services.documents.resolution.VatMatchResolver
import tech.dokus.domain.Name
import tech.dokus.domain.enums.ContactSource
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.contact.ContactAddressInput
import tech.dokus.domain.model.contact.ContactResolution
import tech.dokus.domain.model.contact.CounterpartySnapshot
import tech.dokus.domain.model.contact.CreateContactRequest
import tech.dokus.domain.model.contact.SuggestedContact
import tech.dokus.foundation.backend.utils.loggerFor

data class ContactResolutionResult(
    val snapshot: CounterpartySnapshot,
    val resolution: ContactResolution
)

class ContactResolutionService(
    private val vatMatchResolver: VatMatchResolver,
    private val ibanNameResolver: IbanNameResolver,
    private val autoCreateResolver: AutoCreateResolver,
    private val nameSuggestionResolver: NameSuggestionResolver,
    private val contactService: ContactService
) {
    private val logger = loggerFor()

    suspend fun resolve(
        tenantId: TenantId,
        draftData: DocumentDraftData,
        authoritativeSnapshot: CounterpartySnapshot,
        tenantVat: VatNumber? = null
    ): ContactResolutionResult {
        val rawSnapshot = authoritativeSnapshot.normalized()
        if (rawSnapshot.isEmptyForResolution()) {
            return ContactResolutionResult(rawSnapshot, ContactResolution.PendingReview(rawSnapshot))
        }

        // A counterparty should never be the tenant — strip hallucinated tenant VAT
        val snapshot = if (rawSnapshot.vatNumber.isSameVat(tenantVat)) {
            logger.warn("Counterparty VAT {} matches tenant VAT; stripping from snapshot", rawSnapshot.vatNumber)
            rawSnapshot.copy(vatNumber = null)
        } else {
            rawSnapshot
        }
        if (snapshot.isEmptyForResolution()) {
            return ContactResolutionResult(snapshot, ContactResolution.PendingReview(snapshot))
        }

        val strictAutoLink = when (draftData) {
            is InvoiceDraftData -> draftData.direction == DocumentDirection.Unknown
            is ReceiptDraftData -> draftData.direction == DocumentDirection.Unknown
            is CreditNoteDraftData -> draftData.direction == DocumentDirection.Unknown
            is BankStatementDraftData -> false
        }

        val input = ResolverInput(tenantId, snapshot, strictAutoLink)
        val suggestions = mutableListOf<SuggestedContact>()

        for (resolver in listOf(
            vatMatchResolver::invoke,
            ibanNameResolver::invoke,
            autoCreateResolver::invoke,
            nameSuggestionResolver::invoke
        )) {
            when (val outcome = resolver(input)) {
                is ResolverOutcome.Resolved -> return ContactResolutionResult(snapshot, outcome.resolution)
                is ResolverOutcome.Partial -> suggestions += outcome.suggestions
            }
        }

        if (suggestions.isNotEmpty()) {
            return ContactResolutionResult(
                snapshot = snapshot,
                resolution = ContactResolution.Suggested(
                    candidates = suggestions.distinctBy { it.contactId },
                    suggestedNew = snapshot.takeIf { it.name != null || it.vatNumber != null || it.iban != null },
                    reason = "Potential matches found"
                )
            )
        }

        return ContactResolutionResult(snapshot, ContactResolution.PendingReview(snapshot))
    }

    suspend fun createContactFromResolution(
        tenantId: TenantId,
        resolution: ContactResolution.AutoCreate
    ): ContactId? {
        val data = resolution.contactData
        val name = data.name?.trim().orEmpty()
        if (name.isEmpty()) return null

        val request = CreateContactRequest(
            name = Name(name),
            email = data.email,
            iban = data.iban,
            vatNumber = data.vatNumber,
            addresses = resolution.cbeVerified?.address?.let { addr ->
                listOf(
                    ContactAddressInput(
                        streetLine1 = addr.streetLine1,
                        streetLine2 = addr.streetLine2,
                        city = addr.city,
                        postalCode = addr.postalCode,
                        country = addr.country.dbValue,
                    )
                )
            } ?: data.toAddressInputs(),
            companyNumber = resolution.cbeVerified?.enterpriseNumber ?: data.companyNumber,
            source = ContactSource.AI
        )

        return contactService.createContact(tenantId, request).getOrNull()?.id
    }

    private fun VatNumber?.isSameVat(other: VatNumber?): Boolean {
        if (this == null || other == null) return false
        return this.normalized == other.normalized
    }

    private fun CounterpartySnapshot.normalized(): CounterpartySnapshot = CounterpartySnapshot(
        name = name?.trim()?.takeIf { it.isNotEmpty() },
        vatNumber = vatNumber,
        iban = iban,
        email = email,
        companyNumber = companyNumber?.trim()?.takeIf { it.isNotEmpty() },
        streetLine1 = streetLine1?.trim()?.takeIf { it.isNotEmpty() },
        streetLine2 = streetLine2?.trim()?.takeIf { it.isNotEmpty() },
        postalCode = postalCode?.trim()?.takeIf { it.isNotEmpty() },
        city = city?.trim()?.takeIf { it.isNotEmpty() },
        country = country
    )

    private fun CounterpartySnapshot.isEmptyForResolution(): Boolean {
        return name == null && vatNumber == null && iban == null
    }

    private fun CounterpartySnapshot.toAddressInputs(): List<ContactAddressInput> {
        if (streetLine1 == null && city == null && postalCode == null && country == null) return emptyList()
        return listOf(
            ContactAddressInput(
                streetLine1 = streetLine1,
                streetLine2 = streetLine2,
                city = city,
                postalCode = postalCode,
                country = country?.dbValue
            )
        )
    }
}
