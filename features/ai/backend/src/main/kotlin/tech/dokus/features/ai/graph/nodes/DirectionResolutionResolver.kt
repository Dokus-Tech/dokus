package tech.dokus.features.ai.graph.nodes

import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.util.JaroWinkler
import tech.dokus.features.ai.models.DirectionResolution
import tech.dokus.features.ai.models.DirectionResolutionSource
import tech.dokus.features.ai.models.FinancialExtractionResult

internal object DirectionResolutionResolver {
    private const val NameMatchConfidence = 0.80
    private const val DefaultHintConfidence = 0.60
    private const val NameSimilarityThreshold = 0.90

    fun resolve(
        extraction: FinancialExtractionResult,
        tenant: Tenant,
        associatedPersonNames: List<String>
    ): DirectionResolution {
        return when (extraction) {
            is FinancialExtractionResult.Invoice -> resolveForParties(
                sellerName = extraction.data.sellerName,
                sellerVat = extraction.data.sellerVat?.normalized,
                buyerName = extraction.data.buyerName,
                buyerVat = extraction.data.buyerVat?.normalized,
                hintDirection = extraction.data.directionHint,
                hintConfidence = extraction.data.directionHintConfidence,
                tenant = tenant,
                associatedPersonNames = associatedPersonNames,
                type = "invoice"
            )

            is FinancialExtractionResult.CreditNote -> resolveForParties(
                sellerName = extraction.data.sellerName,
                sellerVat = extraction.data.sellerVat?.normalized,
                buyerName = extraction.data.buyerName,
                buyerVat = extraction.data.buyerVat?.normalized,
                hintDirection = extraction.data.directionHint,
                hintConfidence = extraction.data.directionHintConfidence,
                tenant = tenant,
                associatedPersonNames = associatedPersonNames,
                type = "credit_note"
            )

            is FinancialExtractionResult.Receipt -> resolveReceipt(
                merchantName = extraction.data.merchantName,
                merchantVat = extraction.data.merchantVat?.normalized,
                hintDirection = extraction.data.directionHint,
                hintConfidence = extraction.data.directionHintConfidence,
                tenant = tenant,
                associatedPersonNames = associatedPersonNames
            )

            is FinancialExtractionResult.BankStatement -> DirectionResolution(
                direction = DocumentDirection.Neutral,
                source = DirectionResolutionSource.Unknown,
                confidence = 1.0,
                reasoning = "Bank statement rows are evidence-only and direction is neutral"
            )

            is FinancialExtractionResult.ProForma,
            is FinancialExtractionResult.PurchaseOrder,
            is FinancialExtractionResult.Quote,
            is FinancialExtractionResult.Unsupported -> DirectionResolution(
                direction = DocumentDirection.Unknown,
                source = DirectionResolutionSource.Unknown,
                confidence = 0.0,
                reasoning = "Direction not applicable for extraction type ${extraction::class.simpleName}"
            )
        }
    }

    fun resolvedCounterpartyVat(
        extraction: FinancialExtractionResult,
        direction: DocumentDirection,
        tenantVat: String? = null
    ): String? {
        val raw = when (extraction) {
            is FinancialExtractionResult.Invoice -> when (direction) {
                DocumentDirection.Inbound -> extraction.data.sellerVat?.normalized
                DocumentDirection.Outbound -> extraction.data.buyerVat?.normalized
                DocumentDirection.Neutral,
                DocumentDirection.Unknown -> null
            }

            is FinancialExtractionResult.CreditNote -> when (direction) {
                DocumentDirection.Inbound -> extraction.data.sellerVat?.normalized
                DocumentDirection.Outbound -> extraction.data.buyerVat?.normalized
                DocumentDirection.Neutral,
                DocumentDirection.Unknown -> null
            }

            is FinancialExtractionResult.Receipt -> when (direction) {
                DocumentDirection.Inbound -> extraction.data.merchantVat?.normalized
                DocumentDirection.Outbound,
                DocumentDirection.Neutral,
                DocumentDirection.Unknown -> null
            }

            is FinancialExtractionResult.BankStatement -> null

            is FinancialExtractionResult.ProForma,
            is FinancialExtractionResult.PurchaseOrder,
            is FinancialExtractionResult.Quote,
            is FinancialExtractionResult.Unsupported -> null
        }
        // A counterparty VAT that equals the tenant VAT is always wrong (hallucination or extraction error)
        if (raw != null && tenantVat != null && raw == tenantVat) return null
        return raw
    }

    private fun resolveForParties(
        sellerName: String?,
        sellerVat: String?,
        buyerName: String?,
        buyerVat: String?,
        hintDirection: DocumentDirection,
        hintConfidence: Double?,
        tenant: Tenant,
        associatedPersonNames: List<String>,
        type: String
    ): DirectionResolution {
        val tenantVat = tenant.vatNumber.normalized.takeIf { it.isNotBlank() }
        val sellerVatMatch = tenantVat != null && sellerVat == tenantVat
        val buyerVatMatch = tenantVat != null && buyerVat == tenantVat

        if (sellerVatMatch.xor(buyerVatMatch)) {
            val direction = if (sellerVatMatch) DocumentDirection.Outbound else DocumentDirection.Inbound
            val counterpartyVat = if (direction == DocumentDirection.Inbound) sellerVat else buyerVat
            return DirectionResolution(
                direction = direction,
                source = DirectionResolutionSource.VatMatch,
                confidence = 1.0,
                matchedField = if (sellerVatMatch) "sellerVat" else "buyerVat",
                matchedValue = tenantVat,
                tenantVat = tenantVat,
                counterpartyVat = counterpartyVat,
                reasoning = "Resolved $type direction from tenant VAT match"
            )
        }

        if (sellerVatMatch && buyerVatMatch) {
            return DirectionResolution(
                direction = DocumentDirection.Unknown,
                source = DirectionResolutionSource.Unknown,
                confidence = 0.0,
                matchedField = "sellerVat,buyerVat",
                matchedValue = tenantVat,
                tenantVat = tenantVat,
                reasoning = "Both seller and buyer VAT match tenant VAT; ambiguous direction"
            )
        }

        val tenantNames = tenantNameCandidates(tenant, associatedPersonNames)
        val sellerNameMatch = matchesTenantName(sellerName, tenantNames)
        val buyerNameMatch = matchesTenantName(buyerName, tenantNames)

        if (sellerNameMatch.xor(buyerNameMatch)) {
            val direction = if (sellerNameMatch) DocumentDirection.Outbound else DocumentDirection.Inbound
            val counterpartyVat = if (direction == DocumentDirection.Inbound) sellerVat else buyerVat
            return DirectionResolution(
                direction = direction,
                source = DirectionResolutionSource.NameMatch,
                confidence = NameMatchConfidence,
                matchedField = if (sellerNameMatch) "sellerName" else "buyerName",
                matchedValue = if (sellerNameMatch) sellerName else buyerName,
                tenantVat = tenantVat,
                counterpartyVat = counterpartyVat,
                reasoning = "Resolved $type direction from tenant name similarity"
            )
        }

        if (hintDirection != DocumentDirection.Unknown) {
            val normalizedHintConfidence = (hintConfidence ?: DefaultHintConfidence).coerceIn(0.0, 1.0)
            val counterpartyVat = when (hintDirection) {
                DocumentDirection.Inbound -> sellerVat
                DocumentDirection.Outbound -> buyerVat
                DocumentDirection.Neutral,
                DocumentDirection.Unknown -> null
            }
            return DirectionResolution(
                direction = hintDirection,
                source = DirectionResolutionSource.AiHint,
                confidence = normalizedHintConfidence,
                matchedField = "directionHint",
                matchedValue = hintDirection.name,
                tenantVat = tenantVat,
                counterpartyVat = counterpartyVat,
                reasoning = "Resolved $type direction from AI tie-breaker hint"
            )
        }

        return DirectionResolution(
            direction = DocumentDirection.Unknown,
            source = DirectionResolutionSource.Unknown,
            confidence = 0.0,
            tenantVat = tenantVat,
            reasoning = "No VAT/name/hint evidence to resolve $type direction"
        )
    }

    private fun resolveReceipt(
        merchantName: String?,
        merchantVat: String?,
        hintDirection: DocumentDirection,
        hintConfidence: Double?,
        tenant: Tenant,
        associatedPersonNames: List<String>
    ): DirectionResolution {
        val tenantVat = tenant.vatNumber.normalized.takeIf { it.isNotBlank() }
        if (tenantVat != null && merchantVat != null) {
            if (merchantVat != tenantVat) {
                // Different VAT → definitely inbound from this merchant
                return DirectionResolution(
                    direction = DocumentDirection.Inbound,
                    source = DirectionResolutionSource.VatMatch,
                    confidence = 1.0,
                    matchedField = "merchantVat",
                    matchedValue = merchantVat,
                    tenantVat = tenantVat,
                    counterpartyVat = merchantVat,
                    reasoning = "Resolved receipt direction: merchant VAT differs from tenant"
                )
            }
            // merchantVat == tenantVat — only trust if merchant name also matches tenant
            val tenantNames = tenantNameCandidates(tenant, associatedPersonNames)
            if (matchesTenantName(merchantName, tenantNames)) {
                return DirectionResolution(
                    direction = DocumentDirection.Outbound,
                    source = DirectionResolutionSource.VatMatch,
                    confidence = 1.0,
                    matchedField = "merchantVat",
                    matchedValue = merchantVat,
                    tenantVat = tenantVat,
                    counterpartyVat = null,
                    reasoning = "Resolved receipt direction: merchant VAT and name match tenant"
                )
            }
            // VAT matches but name doesn't — likely hallucinated VAT, fall through to name/hint resolution
        }

        // If merchantVat equals tenantVat but name didn't match, the VAT is unreliable (hallucination)
        val trustedMerchantVat = merchantVat?.takeIf { tenantVat == null || it != tenantVat }

        val tenantNames = tenantNameCandidates(tenant, associatedPersonNames)
        if (matchesTenantName(merchantName, tenantNames)) {
            return DirectionResolution(
                direction = DocumentDirection.Outbound,
                source = DirectionResolutionSource.NameMatch,
                confidence = NameMatchConfidence,
                matchedField = "merchantName",
                matchedValue = merchantName,
                tenantVat = tenantVat,
                counterpartyVat = null,
                reasoning = "Resolved receipt direction from merchant name similarity"
            )
        }
        if (!merchantName.isNullOrBlank()) {
            return DirectionResolution(
                direction = DocumentDirection.Inbound,
                source = DirectionResolutionSource.NameMatch,
                confidence = NameMatchConfidence,
                matchedField = "merchantName",
                matchedValue = merchantName,
                tenantVat = tenantVat,
                counterpartyVat = trustedMerchantVat,
                reasoning = "Resolved receipt direction: merchant name differs from tenant"
            )
        }

        if (hintDirection != DocumentDirection.Unknown) {
            val normalizedHintConfidence = (hintConfidence ?: DefaultHintConfidence).coerceIn(0.0, 1.0)
            return DirectionResolution(
                direction = hintDirection,
                source = DirectionResolutionSource.AiHint,
                confidence = normalizedHintConfidence,
                matchedField = "directionHint",
                matchedValue = hintDirection.name,
                tenantVat = tenantVat,
                counterpartyVat = if (hintDirection == DocumentDirection.Inbound) trustedMerchantVat else null,
                reasoning = "Resolved receipt direction from AI tie-breaker hint"
            )
        }

        return DirectionResolution(
            direction = DocumentDirection.Unknown,
            source = DirectionResolutionSource.Unknown,
            confidence = 0.0,
            tenantVat = tenantVat,
            reasoning = "No VAT/name/hint evidence to resolve receipt direction"
        )
    }

    private fun tenantNameCandidates(tenant: Tenant, associatedPersonNames: List<String>): List<String> {
        return buildList {
            add(tenant.legalName.value)
            add(tenant.displayName.value)
            addAll(associatedPersonNames)
        }.mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            .distinct()
    }

    private fun matchesTenantName(value: String?, tenantNames: List<String>): Boolean {
        val normalizedValue = normalizeName(value ?: return false)
        if (normalizedValue.isBlank()) return false

        return tenantNames.any { tenantName ->
            val normalizedTenant = normalizeName(tenantName)
            if (normalizedTenant.isBlank()) return@any false
            normalizedValue == normalizedTenant ||
                normalizedValue.contains(normalizedTenant) ||
                normalizedTenant.contains(normalizedValue) ||
                JaroWinkler.similarity(normalizedValue, normalizedTenant) >= NameSimilarityThreshold
        }
    }

    private fun normalizeName(value: String): String {
        return value.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
