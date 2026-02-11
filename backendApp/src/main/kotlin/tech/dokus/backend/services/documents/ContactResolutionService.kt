package tech.dokus.backend.services.documents

import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.domain.Name
import tech.dokus.domain.enums.ContactSource
import tech.dokus.domain.enums.CreditNoteDirection
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.BillDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.contact.ContactMatchScore
import tech.dokus.domain.model.contact.ContactResolution
import tech.dokus.domain.model.contact.CounterpartySnapshot
import tech.dokus.domain.model.contact.MatchEvidence
import tech.dokus.domain.model.contact.SuggestedContact
import tech.dokus.domain.model.contact.CreateContactRequest
import tech.dokus.domain.model.contact.ContactAddressInput
import tech.dokus.domain.model.entity.EntityLookup
import tech.dokus.domain.model.entity.EntityStatus
import tech.dokus.domain.util.JaroWinkler
import tech.dokus.foundation.backend.lookup.CbeApiClient

data class ContactResolutionResult(
    val snapshot: CounterpartySnapshot,
    val resolution: ContactResolution
)

class ContactResolutionService(
    private val contactRepository: ContactRepository,
    private val cbeApiClient: CbeApiClient
) {
    companion object {
        private const val StrongNameThreshold = 0.90
        private const val SuggestionThreshold = 0.80
    }

    suspend fun resolve(
        tenantId: TenantId,
        draftData: DocumentDraftData
    ): ContactResolutionResult {
        val snapshot = buildSnapshot(draftData)
        val name = snapshot.name?.trim().orEmpty().takeIf { it.isNotEmpty() }
        val vat = snapshot.vatNumber
        val iban = snapshot.iban
        val strictAutoLink = draftData is CreditNoteDraftData && draftData.direction == CreditNoteDirection.Unknown

        val suggestions = mutableListOf<SuggestedContact>()

        // 1) VAT match (exact, normalized)
        if (vat != null) {
            val vatMatch = contactRepository.findByVatNumber(tenantId, vat.value).getOrNull()
            if (vatMatch != null) {
                val cbeStatus = resolveCbeStatus(vat)
                val allowAutoLink = !strictAutoLink || cbeStatus == EntityStatus.Active
                val nameSimilarity = name?.let { similarity(it, vatMatch.name.value) }

                if (allowAutoLink) {
                    val evidence = MatchEvidence(
                        vatMatch = true,
                        ibanMatch = false,
                        nameSimilarity = nameSimilarity,
                        ambiguityCount = 1,
                        cbeStatus = cbeStatus
                    )
                    return ContactResolutionResult(
                        snapshot = snapshot,
                        resolution = ContactResolution.Matched(
                            contactId = vatMatch.id,
                            evidence = evidence
                        )
                    )
                }

                suggestions += vatMatch.toSuggestedContact(
                    vatMatch = true,
                    ibanMatch = false,
                    nameSimilarity = nameSimilarity,
                    ambiguityCount = 1,
                    reason = "VAT matched but direction is unclear"
                )
            }
        }

        // 2) IBAN match + strong name similarity
        if (iban != null && name != null) {
            val ibanMatches = contactRepository.findByIban(tenantId, iban).getOrNull().orEmpty()
            if (ibanMatches.isNotEmpty()) {
                val scored = ibanMatches.map { contact ->
                    contact to similarity(name, contact.name.value)
                }
                val strongMatches = scored.filter { it.second >= StrongNameThreshold }
                if (strongMatches.size == 1 && (!strictAutoLink || strongMatches.first().second >= StrongNameThreshold)) {
                    val (contact, score) = strongMatches.first()
                    val evidence = MatchEvidence(
                        vatMatch = false,
                        ibanMatch = true,
                        nameSimilarity = score,
                        ambiguityCount = strongMatches.size,
                        cbeStatus = null
                    )
                    return ContactResolutionResult(
                        snapshot = snapshot,
                        resolution = ContactResolution.Matched(
                            contactId = contact.id,
                            evidence = evidence
                        )
                    )
                }

                val ambiguityCount = strongMatches.size
                suggestions += strongMatches.map { (contact, score) ->
                    contact.toSuggestedContact(
                        vatMatch = false,
                        ibanMatch = true,
                        nameSimilarity = score,
                        ambiguityCount = ambiguityCount,
                        reason = "IBAN match with name similarity ${formatScore(score)}"
                    )
                }
            }
        }

        // 3) CBE auto-create for Belgian VAT
        if (vat != null && vat.isValid && vat.isBelgian) {
            val cbeLookup = cbeApiClient.searchByVat(vat).getOrNull()
            if (cbeLookup != null) {
                when (cbeLookup.status) {
                    EntityStatus.Active -> return ContactResolutionResult(
                        snapshot = snapshot,
                        resolution = ContactResolution.AutoCreate(
                            contactData = snapshotFromCbe(cbeLookup, snapshot),
                            cbeVerified = cbeLookup,
                            evidence = MatchEvidence(
                                vatMatch = false,
                                ibanMatch = false,
                                nameSimilarity = null,
                                ambiguityCount = 0,
                                cbeStatus = EntityStatus.Active
                            )
                        )
                    )
                    EntityStatus.Inactive -> return ContactResolutionResult(
                        snapshot = snapshot,
                        resolution = ContactResolution.PendingReview(snapshot)
                    )
                    EntityStatus.Unknown -> {
                        // Fall through to non-CBE logic
                    }
                }
            }
        }

        // 4) Auto-create for valid non-BE VAT
        if (vat != null && vat.isValid && !vat.isBelgian && name != null) {
            val evidence = MatchEvidence(
                vatMatch = false,
                ibanMatch = false,
                nameSimilarity = null,
                ambiguityCount = 0,
                cbeStatus = null
            )
            return ContactResolutionResult(
                snapshot = snapshot,
                resolution = ContactResolution.AutoCreate(
                    contactData = snapshot,
                    cbeVerified = null,
                    evidence = evidence
                )
            )
        }

        // 5) Auto-create without VAT if name + IBAN present
        if (vat == null && name != null && iban != null) {
            val evidence = MatchEvidence(
                vatMatch = false,
                ibanMatch = false,
                nameSimilarity = null,
                ambiguityCount = 0,
                cbeStatus = null
            )
            return ContactResolutionResult(
                snapshot = snapshot,
                resolution = ContactResolution.AutoCreate(
                    contactData = snapshot,
                    cbeVerified = null,
                    evidence = evidence
                )
            )
        }

        // 6) Suggestions by name similarity
        if (name != null) {
            val candidates = contactRepository.findByName(tenantId, name, limit = 10).getOrNull().orEmpty()
            val scored = candidates.map { contact ->
                contact to similarity(name, contact.name.value)
            }.filter { it.second >= SuggestionThreshold }

            if (scored.isNotEmpty()) {
                val ambiguityCount = scored.size
                val suggestionList = scored.map { (contact, score) ->
                    contact.toSuggestedContact(
                        vatMatch = false,
                        ibanMatch = false,
                        nameSimilarity = score,
                        ambiguityCount = ambiguityCount,
                        reason = "Name similarity ${formatScore(score)}"
                    )
                }
                suggestions += suggestionList
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

        return ContactResolutionResult(
            snapshot = snapshot,
            resolution = ContactResolution.PendingReview(snapshot)
        )
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

        return contactRepository.createContact(tenantId, request).getOrNull()?.id
    }

    private fun buildSnapshot(draftData: DocumentDraftData): CounterpartySnapshot = when (draftData) {
        is InvoiceDraftData -> when (draftData.direction) {
            DocumentDirection.Inbound -> CounterpartySnapshot(
                name = draftData.seller.name ?: draftData.customerName,
                vatNumber = draftData.seller.vat ?: draftData.customerVat,
                iban = draftData.seller.iban ?: draftData.iban,
                email = draftData.seller.email ?: draftData.customerEmail,
                streetLine1 = draftData.seller.streetLine1,
                streetLine2 = draftData.seller.streetLine2,
                postalCode = draftData.seller.postalCode,
                city = draftData.seller.city,
                country = null
            )
            DocumentDirection.Outbound -> CounterpartySnapshot(
                name = draftData.buyer.name ?: draftData.customerName,
                vatNumber = draftData.buyer.vat ?: draftData.customerVat,
                iban = draftData.buyer.iban,
                email = draftData.buyer.email ?: draftData.customerEmail,
                streetLine1 = draftData.buyer.streetLine1,
                streetLine2 = draftData.buyer.streetLine2,
                postalCode = draftData.buyer.postalCode,
                city = draftData.buyer.city,
                country = null
            )
            DocumentDirection.Unknown -> CounterpartySnapshot(
                name = draftData.customerName ?: draftData.buyer.name ?: draftData.seller.name,
                vatNumber = draftData.customerVat ?: draftData.buyer.vat ?: draftData.seller.vat,
                iban = draftData.iban ?: draftData.buyer.iban ?: draftData.seller.iban,
                email = draftData.customerEmail ?: draftData.buyer.email ?: draftData.seller.email,
                streetLine1 = draftData.buyer.streetLine1 ?: draftData.seller.streetLine1,
                streetLine2 = draftData.buyer.streetLine2 ?: draftData.seller.streetLine2,
                postalCode = draftData.buyer.postalCode ?: draftData.seller.postalCode,
                city = draftData.buyer.city ?: draftData.seller.city,
                country = null
            )
        }
        is BillDraftData -> CounterpartySnapshot(
            name = draftData.supplierName ?: draftData.seller.name,
            vatNumber = draftData.supplierVat ?: draftData.seller.vat,
            iban = draftData.iban ?: draftData.seller.iban,
            email = null,
        )
        is CreditNoteDraftData -> CounterpartySnapshot(
            name = draftData.counterpartyName,
            vatNumber = draftData.counterpartyVat,
            iban = null,
            email = null,
        )
        is ReceiptDraftData -> CounterpartySnapshot(
            name = draftData.merchantName,
            vatNumber = draftData.merchantVat,
            iban = null,
            email = null,
        )
    }

    private suspend fun resolveCbeStatus(vat: VatNumber): EntityStatus? {
        if (!vat.isValid || !vat.isBelgian) return null
        return cbeApiClient.searchByVat(vat).getOrNull()?.status
    }

    private fun snapshotFromCbe(
        lookup: EntityLookup,
        fallback: CounterpartySnapshot
    ): CounterpartySnapshot {
        val address = lookup.address
        return CounterpartySnapshot(
            name = lookup.name.value,
            vatNumber = lookup.vatNumber,
            iban = fallback.iban,
            email = fallback.email,
            companyNumber = lookup.enterpriseNumber,
            streetLine1 = address?.streetLine1,
            streetLine2 = address?.streetLine2,
            postalCode = address?.postalCode,
            city = address?.city,
            country = address?.country
        )
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

    private fun similarity(left: String, right: String): Double {
        return JaroWinkler.similarity(normalizeName(left), normalizeName(right))
    }

    private fun normalizeName(value: String): String {
        return value.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun formatScore(value: Double): String = String.format("%.2f", value)

    private fun tech.dokus.domain.model.contact.ContactDto.toSuggestedContact(
        vatMatch: Boolean,
        ibanMatch: Boolean,
        nameSimilarity: Double?,
        ambiguityCount: Int,
        reason: String
    ): SuggestedContact {
        val score = ContactMatchScore(
            vatMatch = vatMatch,
            ibanMatch = ibanMatch,
            nameSimilarity = nameSimilarity ?: 0.0,
            ambiguityCount = ambiguityCount,
            cbeResult = null
        )
        return SuggestedContact(
            contactId = id,
            name = name.value,
            vatNumber = vatNumber,
            iban = iban,
            matchScore = score,
            reason = reason
        )
    }
}
