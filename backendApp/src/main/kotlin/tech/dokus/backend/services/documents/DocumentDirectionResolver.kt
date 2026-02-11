package tech.dokus.backend.services.documents

import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.model.BillDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.util.JaroWinkler

data class NormalizedDraft(
    val documentType: DocumentType,
    val draftData: DocumentDraftData?
)

/**
 * Resolves deterministic business direction and normalizes invoice/bill drafts.
 *
 * The AI extracts neutral seller/buyer facts. This resolver determines whether the tenant is
 * the seller or buyer using tenant VAT/name matching and then maps to invoice vs bill semantics.
 */
class DocumentDirectionResolver {

    fun normalize(
        classifiedType: DocumentType,
        draftData: DocumentDraftData?,
        tenant: Tenant,
        associatedPersonNames: List<String> = emptyList()
    ): NormalizedDraft {
        if (draftData == null) return NormalizedDraft(classifiedType, null)

        return when (draftData) {
            is InvoiceDraftData -> normalizeInvoice(classifiedType, draftData, tenant, associatedPersonNames)
            is BillDraftData -> NormalizedDraft(
                documentType = DocumentType.Bill,
                draftData = draftData.copy(direction = draftData.direction.takeUnless { it == DocumentDirection.Unknown } ?: DocumentDirection.Inbound)
            )
            is ReceiptDraftData -> {
                val direction = resolveReceiptDirection(draftData, tenant, associatedPersonNames)
                NormalizedDraft(
                    documentType = DocumentType.Receipt,
                    draftData = draftData.copy(direction = direction)
                )
            }
            is CreditNoteDraftData -> NormalizedDraft(
                documentType = DocumentType.CreditNote,
                draftData = draftData
            )
        }
    }

    private fun normalizeInvoice(
        classifiedType: DocumentType,
        draftData: InvoiceDraftData,
        tenant: Tenant,
        associatedPersonNames: List<String>
    ): NormalizedDraft {
        val direction = resolveInvoiceDirection(draftData, tenant, associatedPersonNames)
        val invoice = draftData.copy(
            direction = direction,
            // Keep legacy customer fields synced with neutral buyer facts.
            customerName = draftData.buyer.name ?: draftData.customerName,
            customerVat = draftData.buyer.vat ?: draftData.customerVat,
            customerEmail = draftData.buyer.email ?: draftData.customerEmail
        )

        return when (direction) {
            DocumentDirection.Outbound -> NormalizedDraft(DocumentType.Invoice, invoice)
            DocumentDirection.Inbound -> NormalizedDraft(
                DocumentType.Bill,
                BillDraftData(
                    direction = DocumentDirection.Inbound,
                    supplierName = invoice.seller.name,
                    supplierVat = invoice.seller.vat,
                    invoiceNumber = invoice.invoiceNumber,
                    issueDate = invoice.issueDate,
                    dueDate = invoice.dueDate,
                    currency = invoice.currency,
                    subtotalAmount = invoice.subtotalAmount,
                    vatAmount = invoice.vatAmount,
                    totalAmount = invoice.totalAmount,
                    lineItems = invoice.lineItems,
                    vatBreakdown = invoice.vatBreakdown,
                    iban = invoice.iban ?: invoice.seller.iban,
                    payment = invoice.payment,
                    notes = invoice.notes,
                    seller = invoice.seller,
                    buyer = invoice.buyer
                )
            )
            DocumentDirection.Unknown -> {
                if (classifiedType == DocumentType.Bill) {
                    NormalizedDraft(
                        DocumentType.Bill,
                        BillDraftData(
                            direction = DocumentDirection.Unknown,
                            supplierName = invoice.seller.name,
                            supplierVat = invoice.seller.vat,
                            invoiceNumber = invoice.invoiceNumber,
                            issueDate = invoice.issueDate,
                            dueDate = invoice.dueDate,
                            currency = invoice.currency,
                            subtotalAmount = invoice.subtotalAmount,
                            vatAmount = invoice.vatAmount,
                            totalAmount = invoice.totalAmount,
                            lineItems = invoice.lineItems,
                            vatBreakdown = invoice.vatBreakdown,
                            iban = invoice.iban ?: invoice.seller.iban,
                            payment = invoice.payment,
                            notes = invoice.notes,
                            seller = invoice.seller,
                            buyer = invoice.buyer
                        )
                    )
                } else {
                    NormalizedDraft(DocumentType.Invoice, invoice)
                }
            }
        }
    }

    private fun resolveInvoiceDirection(
        draftData: InvoiceDraftData,
        tenant: Tenant,
        associatedPersonNames: List<String>
    ): DocumentDirection {
        val tenantVat = tenant.vatNumber.normalized.takeIf { it.isNotBlank() }
        val sellerVatMatch = tenantVat != null && draftData.seller.vat?.normalized == tenantVat
        val buyerVatMatch = tenantVat != null && draftData.buyer.vat?.normalized == tenantVat

        if (sellerVatMatch.xor(buyerVatMatch)) {
            return if (sellerVatMatch) DocumentDirection.Outbound else DocumentDirection.Inbound
        }
        if (sellerVatMatch && buyerVatMatch) return DocumentDirection.Unknown

        val tenantNames = buildTenantNameCandidates(tenant, associatedPersonNames)
        val sellerNameMatch = nameMatchesTenant(draftData.seller.name, tenantNames)
        val buyerNameMatch = nameMatchesTenant(draftData.buyer.name ?: draftData.customerName, tenantNames)

        return when {
            sellerNameMatch && !buyerNameMatch -> DocumentDirection.Outbound
            buyerNameMatch && !sellerNameMatch -> DocumentDirection.Inbound
            else -> DocumentDirection.Unknown
        }
    }

    private fun resolveReceiptDirection(
        draftData: ReceiptDraftData,
        tenant: Tenant,
        associatedPersonNames: List<String>
    ): DocumentDirection {
        val tenantVat = tenant.vatNumber.normalized.takeIf { it.isNotBlank() }
        val merchantVatMatch = tenantVat != null && draftData.merchantVat?.normalized == tenantVat
        if (merchantVatMatch) return DocumentDirection.Outbound

        val tenantNames = buildTenantNameCandidates(tenant, associatedPersonNames)
        return if (nameMatchesTenant(draftData.merchantName, tenantNames)) {
            DocumentDirection.Outbound
        } else {
            DocumentDirection.Inbound
        }
    }

    private fun buildTenantNameCandidates(
        tenant: Tenant,
        associatedPersonNames: List<String>
    ): List<String> {
        return buildList {
            add(tenant.legalName.value)
            add(tenant.displayName.value)
            addAll(associatedPersonNames)
        }.mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            .distinct()
    }

    private fun nameMatchesTenant(partyName: String?, tenantNames: List<String>): Boolean {
        val normalizedParty = normalizeName(partyName ?: return false)
        if (normalizedParty.isBlank()) return false

        return tenantNames.any { tenantName ->
            val normalizedTenant = normalizeName(tenantName)
            if (normalizedTenant.isBlank()) return@any false

            normalizedParty == normalizedTenant ||
                normalizedParty.contains(normalizedTenant) ||
                normalizedTenant.contains(normalizedParty) ||
                JaroWinkler.similarity(normalizedParty, normalizedTenant) >= 0.90
        }
    }

    private fun normalizeName(value: String): String {
        return value.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
