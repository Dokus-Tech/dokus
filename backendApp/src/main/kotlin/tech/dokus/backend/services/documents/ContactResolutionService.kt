package tech.dokus.backend.services.documents

import tech.dokus.backend.services.contacts.ContactService
import tech.dokus.backend.services.documents.resolution.CbeAutoCreateResolver
import tech.dokus.backend.services.documents.resolution.IbanNameResolver
import tech.dokus.backend.services.documents.resolution.NameIbanAutoCreateResolver
import tech.dokus.backend.services.documents.resolution.NameSuggestionResolver
import tech.dokus.backend.services.documents.resolution.ResolverInput
import tech.dokus.backend.services.documents.resolution.ResolverOutcome
import tech.dokus.backend.services.documents.resolution.VatAutoCreateResolver
import tech.dokus.backend.services.documents.resolution.VatMatchResolver
import tech.dokus.domain.Name
import tech.dokus.domain.enums.ContactSource
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.AnnualAccountsDraftData
import tech.dokus.domain.model.BankFeeDraftData
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.BoardMinutesDraftData
import tech.dokus.domain.model.C4DraftData
import tech.dokus.domain.model.CompanyExtractDraftData
import tech.dokus.domain.model.ContractDraftData
import tech.dokus.domain.model.CorporateTaxAdvanceDraftData
import tech.dokus.domain.model.CorporateTaxDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.CustomsDeclarationDraftData
import tech.dokus.domain.model.DeliveryNoteDraftData
import tech.dokus.domain.model.DepreciationScheduleDraftData
import tech.dokus.domain.model.DimonaDraftData
import tech.dokus.domain.model.DividendDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.EmploymentContractDraftData
import tech.dokus.domain.model.ExpenseClaimDraftData
import tech.dokus.domain.model.FineDraftData
import tech.dokus.domain.model.HolidayPayDraftData
import tech.dokus.domain.model.IcListingDraftData
import tech.dokus.domain.model.InsuranceDraftData
import tech.dokus.domain.model.InterestStatementDraftData
import tech.dokus.domain.model.IntrastatDraftData
import tech.dokus.domain.model.InventoryDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.LeaseDraftData
import tech.dokus.domain.model.LoanDraftData
import tech.dokus.domain.model.OrderConfirmationDraftData
import tech.dokus.domain.model.OssReturnDraftData
import tech.dokus.domain.model.OtherDraftData
import tech.dokus.domain.model.PaymentConfirmationDraftData
import tech.dokus.domain.model.PayrollSummaryDraftData
import tech.dokus.domain.model.PermitDraftData
import tech.dokus.domain.model.PersonalTaxDraftData
import tech.dokus.domain.model.ProFormaDraftData
import tech.dokus.domain.model.PurchaseOrderDraftData
import tech.dokus.domain.model.QuoteDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.ReminderDraftData
import tech.dokus.domain.model.SalarySlipDraftData
import tech.dokus.domain.model.SelfEmployedContributionDraftData
import tech.dokus.domain.model.ShareholderRegisterDraftData
import tech.dokus.domain.model.SocialContributionDraftData
import tech.dokus.domain.model.SocialFundDraftData
import tech.dokus.domain.model.StatementOfAccountDraftData
import tech.dokus.domain.model.SubsidyDraftData
import tech.dokus.domain.model.TaxAssessmentDraftData
import tech.dokus.domain.model.VapzDraftData
import tech.dokus.domain.model.VatAssessmentDraftData
import tech.dokus.domain.model.VatListingDraftData
import tech.dokus.domain.model.VatReturnDraftData
import tech.dokus.domain.model.WithholdingTaxDraftData
import tech.dokus.domain.model.contact.ContactAddressInput
import tech.dokus.domain.model.contact.ContactResolution
import tech.dokus.domain.model.contact.CounterpartySnapshotDto
import tech.dokus.domain.model.contact.CreateContactRequest
import tech.dokus.domain.model.contact.PostalAddressDto
import tech.dokus.domain.model.contact.SuggestedContactDto
import tech.dokus.foundation.backend.utils.loggerFor

data class ContactResolutionResult(
    val snapshot: CounterpartySnapshotDto,
    val resolution: ContactResolution
)

class ContactResolutionService(
    private val vatMatchResolver: VatMatchResolver,
    private val ibanNameResolver: IbanNameResolver,
    private val cbeAutoCreateResolver: CbeAutoCreateResolver,
    private val vatAutoCreateResolver: VatAutoCreateResolver,
    private val nameIbanAutoCreateResolver: NameIbanAutoCreateResolver,
    private val nameSuggestionResolver: NameSuggestionResolver,
    private val contactService: ContactService
) {
    private val logger = loggerFor()

    suspend fun resolve(
        tenantId: TenantId,
        draftData: DocumentDraftData,
        authoritativeSnapshot: CounterpartySnapshotDto,
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
            is ProFormaDraftData,
            is QuoteDraftData,
            is OrderConfirmationDraftData,
            is DeliveryNoteDraftData,
            is ReminderDraftData,
            is StatementOfAccountDraftData,
            is PurchaseOrderDraftData,
            is ExpenseClaimDraftData,
            is BankFeeDraftData,
            is InterestStatementDraftData,
            is PaymentConfirmationDraftData,
            is VatReturnDraftData,
            is VatListingDraftData,
            is VatAssessmentDraftData,
            is IcListingDraftData,
            is OssReturnDraftData,
            is CorporateTaxDraftData,
            is CorporateTaxAdvanceDraftData,
            is TaxAssessmentDraftData,
            is PersonalTaxDraftData,
            is WithholdingTaxDraftData,
            is SocialContributionDraftData,
            is SocialFundDraftData,
            is SelfEmployedContributionDraftData,
            is VapzDraftData,
            is SalarySlipDraftData,
            is PayrollSummaryDraftData,
            is EmploymentContractDraftData,
            is DimonaDraftData,
            is C4DraftData,
            is HolidayPayDraftData,
            is ContractDraftData,
            is LeaseDraftData,
            is LoanDraftData,
            is InsuranceDraftData,
            is DividendDraftData,
            is ShareholderRegisterDraftData,
            is CompanyExtractDraftData,
            is AnnualAccountsDraftData,
            is BoardMinutesDraftData,
            is SubsidyDraftData,
            is FineDraftData,
            is PermitDraftData,
            is CustomsDeclarationDraftData,
            is IntrastatDraftData,
            is DepreciationScheduleDraftData,
            is InventoryDraftData,
            is OtherDraftData -> false
        }

        val input = ResolverInput(tenantId, snapshot, strictAutoLink)
        val suggestions = mutableListOf<SuggestedContactDto>()

        for (resolver in listOf(
            vatMatchResolver::invoke,
            ibanNameResolver::invoke,
            cbeAutoCreateResolver::invoke,
            vatAutoCreateResolver::invoke,
            nameIbanAutoCreateResolver::invoke,
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

    private fun CounterpartySnapshotDto.normalized(): CounterpartySnapshotDto = CounterpartySnapshotDto(
        name = name?.trim()?.takeIf { it.isNotEmpty() },
        vatNumber = vatNumber,
        iban = iban,
        email = email,
        companyNumber = companyNumber?.trim()?.takeIf { it.isNotEmpty() },
        address = address.normalized()
    )

    private fun PostalAddressDto.normalized(): PostalAddressDto = PostalAddressDto(
        streetLine1 = streetLine1?.trim()?.takeIf { it.isNotEmpty() },
        streetLine2 = streetLine2?.trim()?.takeIf { it.isNotEmpty() },
        postalCode = postalCode?.trim()?.takeIf { it.isNotEmpty() },
        city = city?.trim()?.takeIf { it.isNotEmpty() },
        country = country
    )

    private fun CounterpartySnapshotDto.isEmptyForResolution(): Boolean {
        return name == null && vatNumber == null && iban == null
    }

    private fun CounterpartySnapshotDto.toAddressInputs(): List<ContactAddressInput> {
        val addr = address
        if (addr.streetLine1 == null && addr.city == null && addr.postalCode == null && addr.country == null) {
            return emptyList()
        }
        return listOf(
            ContactAddressInput(
                streetLine1 = addr.streetLine1,
                streetLine2 = addr.streetLine2,
                city = addr.city,
                postalCode = addr.postalCode,
                country = addr.country?.dbValue
            )
        )
    }
}
